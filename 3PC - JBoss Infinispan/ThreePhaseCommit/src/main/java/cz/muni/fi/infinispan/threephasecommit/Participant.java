package cz.muni.fi.infinispan.threephasecommit;

import cz.muni.fi.infinispan.threephasecommit.main.Main;
import cz.muni.fi.infinispan.threephasecommit.main.Main.TransactionDecision;
import cz.muni.fi.infinispan.threephasecommit.main.LockFileDemo;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.infinispan.remoting.transport.Address;


public class Participant {

    public static enum Decision {
        Yes, No
    }
    
    private Cache<String, String> coordinatorCache;
    private Cache<Address, String> sitesCache;
    private String result = "";
    private static Integer mutex;
    
    public boolean performThreePhaseCommit() {
        //create cache manager
        EmbeddedCacheManager embeddedCacheManager = null;
        try {
            embeddedCacheManager = new DefaultCacheManager("infinispan.xml");
        } catch (IOException ex) {
            Logger.getLogger(Coordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //get coordinator and sites caches
        coordinatorCache = embeddedCacheManager.getCache("coordinator cache");
        sitesCache = embeddedCacheManager.getCache("sites cache");
        
        //set listener for watching changes in coordinator cache
        CoordinatorListener coordinatorListener = new CoordinatorListener();
        coordinatorCache.addListener(coordinatorListener);
        
        //initialize synchronization primitive for the result change
        mutex = new Integer(-1);
        
        //wait for the listener to set the result value
        synchronized (mutex) {
            try {
                mutex.wait();
            } catch (InterruptedException e) {
                Logger.getLogger(Coordinator.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        
        //stop the cache manager
        embeddedCacheManager.stop();
        
        //return true if the transaction was commited
        return "commited".equals(result);
    }

    public static void run() {
        Participant participant = new Participant();

        boolean result = participant.performThreePhaseCommit();

        if (result) {
            System.out.println("Transaction has been commited.");
        } else {
            System.out.println("Transaction has been aborted.");
        }
    }
    
    /**
     * Listener on the change of the coordinator cache.
     */
    @Listener (sync = false)
    @SuppressWarnings("unused")
    private class CoordinatorListener {

        private Decision decision = null;
        
        @CacheEntryCreated
        @CacheEntryModified
        public synchronized void addSitesDecision(CacheEntryEvent e) {
            if (!e.isPre()) {
                switch ((String) e.getKey()) {

                    case "request": {
                        String request = (String) e.getValue();
                        switch(request) {
                            case "canCommit": {
                                if (TransactionDecision.commit.equals(Main.decideTransaction())) {
                                    decision = Decision.Yes;
                                    LockFileDemo.lockFile();
                                } else {
                                    decision = Decision.No;
                                }
                                sitesCache.put(sitesCache.getCacheManager().getAddress(), decision.name());

                                break;
                            }
                            case "preCommit": {
                                //acknowledge transaction decision
                                sitesCache.put(sitesCache.getCacheManager().getAddress(), "ACK");
                                break;
                            }
                        }
                        break;
                    }

                    case "decision": {
                        String request = (String) e.getValue();

                        switch(request) {
                            case "doCommit": {
                                result = "commited";
                                //write the commited transaction
                                LockFileDemo.writeToFile(Main.TRANSACTION_DATA);
                                //release locked resources
                                LockFileDemo.releaseLock();
                                sitesCache.put(sitesCache.getCacheManager().getAddress(), "haveCommited");
                                break;
                            }
                            case "abort": {
                                result = "aborted";
                                //released resources if they have been locked
                                if (Decision.Yes == decision) {
                                    LockFileDemo.releaseLock();
                                }
                            }
                        }

                        //notify participant of the result
                        synchronized (mutex) {
                            mutex.notify();
                        }
                    }
                }
            }
        }
    }
}
