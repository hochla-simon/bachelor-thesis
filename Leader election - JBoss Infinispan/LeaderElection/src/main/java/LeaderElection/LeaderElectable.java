package LeaderElection;

import java.io.IOException;
import java.util.ArrayList;
import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.infinispan.commons.util.CloseableIteratorSet;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;

public class LeaderElectable {

    /**
     * Cache for the addresses of the electable members indexed by the
     * incremental value assigned when they entered the cache. After the member
     * performs the leadership method, member removes its entry from this cache.
     */
    private Cache<Integer, Address> electableMembersCache;

    /**
     * Cache with the address of the current leader.
     */
    private Cache<Address, String> leaderCache;

    /**
     * My unique address.
     */
    private Address myAddress;

    /**
     * Index assigned when entering the electableMembersCache, it is an
     * incremental value.
     */
    private Integer myIndex;

    /**
     * Manager managing both leaderCache and embeddedMembersCache.
     */
    private EmbeddedCacheManager embeddedCacheManager = null;

    /**
     * Initialize caches and become electable: put my address into the cache of
     * waiting members and perform leader procedure when I become the leader.
     */
    public void becomeElectable() {
        initializeCachesAndMyAddress();

        CloseableIteratorSet<Integer> keySet = electableMembersCache.keySet();
        //if electableMembersCache contains any values, assign my address index greater
        //than the maximal index contained in the cache, otherwise index is 0
        if (keySet.isEmpty()) {
            myIndex = 0;
        } else {
            List<Integer> memberIndexes = new ArrayList<>();
            for (Integer index : keySet) {
                memberIndexes.add(index);
            }
            Integer maxIndex = Collections.max(memberIndexes);
            myIndex = maxIndex + 1;
        }

        electableMembersCache.put(myIndex, myAddress);

        //if I am the only active electable member, I become the leader,
        //otherwise set listener on leaderCache triggered when the current leader finishes   
        if (myIndex == 0) {
            becomeLeader();
            embeddedCacheManager.stop();
        } else {
            LeaderAddressListener leaderAddressListener = new LeaderAddressListener();
            leaderCache.addListener(leaderAddressListener);
        }
    }

    /**
     * Inform other members that I have become the leader, perform the
     * leaderProcedure and do the work after releasing the leadership.
     */
    private synchronized void becomeLeader() {
        leaderCache.put(myAddress, "");
        try {
            leaderProcedure();
        } catch (InterruptedException ex) {
            Logger.getLogger(LeaderElectable.class.getName()).log(Level.SEVERE, null, ex);
        }
        electableMembersCache.remove(myIndex);
        leaderCache.remove(myAddress);
    }

    /**
     * Initializes the given cache manager and both members and leader cache.
     * Next it initializes myAddress with the address of this node.
     */
    private void initializeCachesAndMyAddress() {
        try {
            embeddedCacheManager = new DefaultCacheManager("infinispan.xml");
        } catch (IOException ex) {
            Logger.getLogger(LeaderElectable.class.getName()).log(Level.SEVERE, null, ex);
        }

        electableMembersCache = embeddedCacheManager.getCache("electable members cache");
        leaderCache = embeddedCacheManager.getCache("leader cache");

        myAddress = electableMembersCache.getAdvancedCache().getRpcManager().getAddress();
    }

    /**
     * Listener on the removal of the leader address from the leaderCache.
     */
    @Listener
    @SuppressWarnings("unused")
    private class LeaderAddressListener {

        private final Set<Address> commitedSites = Collections.synchronizedSet(new HashSet<Address>());

        @CacheEntryRemoved
        public synchronized void leaderAddressRemoved(CacheEntryEvent e) {
            CloseableIteratorSet<Integer> keySet = electableMembersCache.keySet();

            Integer minIndex;
            if (!keySet.isEmpty()) {
                List<Integer> memberIndexes = new ArrayList<>();
                for (Integer index : keySet) {
                    memberIndexes.add(index);
                }
                minIndex = Collections.min(memberIndexes);
            } else {
                minIndex = - 1;
            }

            if (minIndex.equals(myIndex)) {
                becomeLeader();
                embeddedCacheManager.stop();

            }
        }
    }

    /**
     * Write into console when performing the procedure and wait for some time
     * before finishing. Imitates the behavior of an actual leader procedure.
     *
     * @throws InterruptedException
     */
    private void leaderProcedure() throws InterruptedException {
        System.out.println("---I have acquired the leadership.---");;
        System.out.println("---Going to wait for ten seconds...---");
        Thread.sleep(10000);
        System.out.println("---Done.---");
        System.out.println("---Leaving.---");
    }

    public static void main(String[] argc) {
        LeaderElectable coordinator = new LeaderElectable();
        coordinator.becomeElectable();
    }
}
