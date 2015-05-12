package cz.muni.fi.infinispan.threephasecommit;

import java.io.IOException;
import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;


public class Coordinator {

    private Cache<String, String> coordinatorCache;
    private Cache<Address, String> sitesCache;
    private String result = "";
    private static Integer mutex;
    private int sitesCount;
        
    /**
     * Used for saving the current time while managing timeouts.
     */
    private Long start = null;
    private static final long TIMEOUT = 5000;
    
    /**
     * Performs the three phase phase commit over the group of connected
     * participants.
     *
     * @return true if the transaction was commited, false otherwise
     */
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
       
       //perform sitesCache initialization and get the sites count
       sitesCount = initializeSitesCache(sitesCache);
       
       //forward initial request to all sites
       coordinatorCache.put("request", "canCommit");
       startCountingTimeout();
       
       //create listener for the sitesCache
       SiteVotesListener siteVotesListener = new SiteVotesListener();
       sitesCache.addListener(siteVotesListener);
       
       //initialize synchronization primitive for the result change
       mutex = new Integer(-1);
        
       //wait for being notified of the result change
       synchronized(mutex) {
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

   /**
    * Puts the unique addresses of the active members using sitesCache
    * into the siteCache map and initialize them with empty value
    * @param sitesCache the shared sites cache
    * @return count of the members using the sites cache minus the coordinator
    */
   private int initializeSitesCache(Cache<Address, String> sitesCache) {
       List<Address> members = sitesCache.getAdvancedCache().getRpcManager().getMembers();
       for (Address address : members) {
           sitesCache.put(address, "");
       }
       return members.size() - 1;
   }
   
    /**
     * Perform the two phase commit and print the result of the transaction
     */
    public static void run() {
        Coordinator coordinator = new Coordinator();
        boolean result = coordinator.performThreePhaseCommit();

        if (result) {
            System.out.println("Transaction was commited.");
        } else {
            System.out.println("Transaction was aborted.");
        }
    }
    
    private void startCountingTimeout() {
        start = System.currentTimeMillis();
    }

    private void abortIfTimeoutElapsed() {
        if ((System.currentTimeMillis() - start) > TIMEOUT) {
            coordinatorCache.put("decision", "abort");
            result = "aborted";

            //notify the coordinator of the result
            synchronized (mutex) {
                mutex.notify();
            }
        }
    }
    
    @Listener
    @SuppressWarnings("unused")
    private class SiteVotesListener {
        /**
         * Set of unique addresses of participants which have already commited.
         */
        private final Set<Address> canCommitSites = Collections.synchronizedSet(new HashSet<Address>());
        /**
         * Set of unique addresses of participants which have already acknowledged receiving the result.
         */
        private final Set<Address> acknowledgedSites = Collections.synchronizedSet(new HashSet<Address>());
        /**
         * Set of unique addresses of participants which have already acknowledged commiting or aborting.
         */
        private final Set<Address> haveCommitedSites = Collections.synchronizedSet(new HashSet<Address>());


        /**
         * Method triggered on a modification of a site's cache entry.
         *
         * @param e object containing information about the event that happened
         */
        @CacheEntryCreated
        @CacheEntryModified
        public synchronized void addSitesDecision(CacheEntryEvent e) {
            if (!e.isPre()) {
                switch((String) e.getValue()) {
                    case "Yes": {
                        abortIfTimeoutElapsed();
                        canCommitSites.add((Address) e.getKey());
                        if (canCommitSites.size() == sitesCount) {
                            coordinatorCache.put("request", "preCommit");
                            System.out.println("called preCommit");
                        }
                        startCountingTimeout();
                        break;
                    }
                    case "No": {
                        coordinatorCache.put("decision", "abort");
                        result = "aborted";

                        //notify the coordinator of the result
                        synchronized (mutex) {
                            mutex.notify();
                        }
                        break;
                    }
                    case "ACK": {
                        abortIfTimeoutElapsed();
                        acknowledgedSites.add((Address) e.getKey());
                        if (acknowledgedSites.size() == sitesCount) {
                            coordinatorCache.put("decision", "doCommit");
                            result = "commited";
                        }
                        startCountingTimeout();
                        break;
                    }
                    case "haveCommited": {
                        abortIfTimeoutElapsed();
                        haveCommitedSites.add((Address) e.getKey());
                        if (haveCommitedSites.size() == sitesCount) {
                            //notify the coordinator of the result
                            synchronized (mutex) {
                                mutex.notify();
                            }
                        }
                        break;
                    }
                }
            }
        }
    }
}

