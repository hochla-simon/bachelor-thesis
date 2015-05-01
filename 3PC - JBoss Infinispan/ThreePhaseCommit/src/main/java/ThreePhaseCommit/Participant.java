package ThreePhaseCommit;

import ThreePhaseCommit.LockFileDemo.TransactionDecision;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.infinispan.remoting.transport.Address;


public class Participant {

    private Cache<String, String> coordinatorCache;
    private Cache<Address, String> sitesCache;
    private FileLock lock = null;
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

    public static void threePhaseCommitTest() {
        Participant participant = new Participant();

        boolean result = participant.performThreePhaseCommit();

        if (result) {
            System.out.println("Transaction was commited.");
        } else {
            System.out.println("Transaction was aborted.");
        }
    }
    
    @Listener (sync = false)
    @SuppressWarnings("unused")
    private class CoordinatorListener {        
        @CacheEntryCreated
        @CacheEntryModified
        @CacheEntryRemoved
        public synchronized void addSitesDecision(CacheEntryEvent e) {
            switch ((String) e.getKey()) {
                
                case "request": {
                    String request = (String) e.getValue();
                    switch(request) {
                        case "canCommit?": {
                            String decision = null;
                            if (TransactionDecision.commit.equals(LockFileDemo.decideTransaction())) {
                                decision = "Yes";
									lock = LockFileDemo.lockFile();
                            } else {
                                decision = "No";
                            }
                            sitesCache.put(sitesCache.getCacheManager().getAddress(), decision);
                            
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
                    String decision = (String) e.getValue();
                    
                    switch(decision) {
                        case "doCommit": {
                            result = "commited";
                            sitesCache.put(sitesCache.getCacheManager().getAddress(), "haveCommited");
                            break;
                        }
                        case "abort": {
                            result = "aborted";
                        }
                    }
                    
                    //notify participant of the result
                    synchronized (mutex) {
                        mutex.notify();
                    }

                    //released locked resources
                    if (lock != null) {
                       LockFileDemo.releaseLock(lock);
                    }
                }
            }
        }
    }
    
    public static void main(String[] argc) {
        threePhaseCommitTest();
    }
}
