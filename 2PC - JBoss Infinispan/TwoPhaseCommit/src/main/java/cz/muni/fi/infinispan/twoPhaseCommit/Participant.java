package cz.muni.fi.infinispan.twophasecommit;

import cz.fi.muni.infinispan.twophasecommit.main.Main;
import static cz.fi.muni.infinispan.twophasecommit.main.Main.TRANSACTION_DATA;
import cz.fi.muni.infinispan.twophasecommit.main.Main.TransactionDecision;
import cz.fi.muni.infinispan.twophasecommit.main.LockFileDemo;
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

    private Cache<String, String> coordinatorCache;
    private Cache<Address, String> sitesCache;
    private String result = "";
    private static Integer mutex;
    private String decision = null;
    
    /**
     * Perform the two phase commit by asking the coordinator for participation in the process,
     * waiting for the transaction request, reporting the decision and getting the final result.
     * @return true when the transaction was commited, otherwise false
     */
    public boolean performTwoPhaseCommit() {
        //create cache manager
        EmbeddedCacheManager embeddedCacheManager = null;
        try {
            embeddedCacheManager = new DefaultCacheManager("infinispan.xml");
        } catch (IOException ex) {
            Logger.getLogger(Coordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //getting coordinator and sites caches
        coordinatorCache = embeddedCacheManager.getCache("coordinator cache");
        sitesCache = embeddedCacheManager.getCache("sites cache");
        
        //setting listener for watching changes in coordinator cache
        CoordinatorListener coordinatorListener = new CoordinatorListener();
        coordinatorCache.addListener(coordinatorListener);
        
        //initialize synchronization primtive for result change
        mutex = new Integer(-1);
        
        //waiting for the listener to set the result value
        synchronized (mutex) {
            try {
                mutex.wait();
            } catch (InterruptedException e) {
                Logger.getLogger(Coordinator.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        
        embeddedCacheManager.stop();
        
        //return true if the transaction was commited
        return "commited".equals(result);
    }
    
    /**
     * Perform the two phase commit and print the result of the transaction
     */
    public static void run() {
        Participant participant = new Participant();

        boolean result = participant.performTwoPhaseCommit();

        if (result) {
            System.out.println("Transaction was commited.");
        } else {
            System.out.println("Transaction was aborted.");
        }
    }
    
    /**
     * Listener on a change of the coordinator's cache.
     */
    @Listener (sync = false)
    @SuppressWarnings("unused")
    private class CoordinatorListener {
        
        /**
         * Report transaction decision after receiving request from the coordinator
         * @param e object containing information about the event that happened 
         */
        @CacheEntryCreated
        @CacheEntryModified
        public synchronized void reportTransactionDecision(CacheEntryEvent e) {
            if ("request".equals((String) e.getKey()) && !e.isPre()) {
                if ("canCommit?".equals((String) e.getValue())) {
                    decideAndReportTransactionDecision();
                }
            }
        }
    
        /**
         * Receive the transaction result from the coordinator
         * and acknowledge the reception of the result.
         * @param e object containing information about the event that happened
         */
        @CacheEntryCreated
        @CacheEntryModified
        public synchronized void receiveTransactionResult(CacheEntryEvent e) {
            if ("decision".equals((String) e.getKey()) && !e.isPre()) {
                //set result
                result = (String) e.getValue();
                
                //write out the transaction content if commited
                if ("commited".equals(result)) {
                    LockFileDemo.writeToFile(Main.TRANSACTION_DATA);
                }
                //release locked resources
                if ("commit".equals(decision)) {
                    LockFileDemo.releaseLock();
                }
               
                //acknowledge having received the result back to the coordinator
                sitesCache.put(sitesCache.getCacheManager().getAddress(), "ACK");

                //notify the participant of the result
                synchronized (mutex) {
                    mutex.notify();
                }
            }
        }
        
        /**
         * Decide for commit or abort and report the decision
         * by putting it to the site's cache under the local address.
         */
        private void decideAndReportTransactionDecision() {
            if (TransactionDecision.commit.equals(Main.decideTransaction())) {
                decision = "commit";
                LockFileDemo.lockFile();
            } else {
                decision = "abort";
            }
            sitesCache.put(sitesCache.getCacheManager().getAddress(), decision);
        }
    }
}
