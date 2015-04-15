package TwoPhaseCommit;

import java.io.IOException;
import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
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
    private String result;
    private static Integer mutex;
    private int sitesCount;
    
    public boolean performTwoPhaseCommit() {
       EmbeddedCacheManager embeddedCacheManager = null;
       try {
           embeddedCacheManager = new DefaultCacheManager("infinispan.xml");
       } catch (IOException ex) {
           Logger.getLogger(Coordinator.class.getName()).log(Level.SEVERE, null, ex);
       }
       coordinatorCache = embeddedCacheManager.getCache("coordinator cache");
       sitesCache = embeddedCacheManager.getCache("sites cache");
       mutex = new Integer(-1);
       result = "";
       
       List<Address> members = sitesCache.getAdvancedCache().getRpcManager().getMembers();
       for (Address address: members) {
           sitesCache.put(address, "");
       }
       sitesCount = members.size() - 1;
       
       coordinatorCache.put("request", "canCommit?");
       
       SiteVotesListener siteVotesListener = new SiteVotesListener();
       sitesCache.addListener(siteVotesListener);
       
       synchronized(mutex) {
           try {
               mutex.wait();
           } catch (InterruptedException e) {
               Logger.getLogger(Coordinator.class.getName()).log(Level.SEVERE, null, e);
           }
       }
       embeddedCacheManager.stop();
       
       return "commited".equals(result);
   }

   public static void twoPhaseCommitTest() {
       Coordinator coordinator = new Coordinator();
       boolean result = coordinator.performTwoPhaseCommit();

       if (result) {
           System.out.println("Transaction was commited.");
       } else {
           System.out.println("Transaction was aborted.");
       }
   }
   
    @Listener
    @SuppressWarnings("unused")
    private class SiteVotesListener {
        private final Set<Address> commitedSites = Collections.synchronizedSet(new HashSet<Address>());
        
        @CacheEntryCreated
        @CacheEntryModified
        @CacheEntryRemoved
        public synchronized void addSitesDecision(CacheEntryEvent e) {
            switch((String) e.getValue()) {
                case "commit": {
                    commitedSites.add((Address) e.getKey());
                    if (commitedSites.size() == sitesCount) {
                        reportTransactionDecision("commited");
                    }
                    break;
                }
                case "abort": {
                     reportTransactionDecision("aborted");
                }
            }
        }
        
        private void reportTransactionDecision(String decision) {
            coordinatorCache.put("decision", decision);
            result = decision;
            synchronized (mutex) {
                mutex.notify();
            }
        }
    }
    
    public static void main(String[] argc) {
        twoPhaseCommitTest();
    }
}

