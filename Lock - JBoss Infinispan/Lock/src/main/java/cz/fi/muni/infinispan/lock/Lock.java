package cz.fi.muni.infinispan.lock;

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

public class Lock {

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
    public void acquireForLock() {
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
            lock();
            try {
                performFileLocking();
            } catch (InterruptedException ex) {
                Logger.getLogger(Lock.class.getName()).log(Level.SEVERE, null, ex);
            }
            unlock();
            embeddedCacheManager.stop();
        } else {
            LeaderAddressListener leaderAddressListener = new LeaderAddressListener();
            leaderCache.addListener(leaderAddressListener);
        }
    }

    /**
     * Inform other members that I have acquired the lock.
     */
    private synchronized void lock() {
        leaderCache.put(myAddress, "");
    }

    /**
     * Inform the other members that I am releasing the lock.
     */
    private synchronized void unlock() {
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
            Logger.getLogger(Lock.class.getName()).log(Level.SEVERE, null, ex);
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
                lock();
                try {
                    performFileLocking();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Lock.class.getName()).log(Level.SEVERE, null, ex);
                }
                unlock();
                embeddedCacheManager.stop();

            }
        }
    }

    /**
     * Write into console when performing the procedure and wait for some time
     * before finishing.
     *
     * @throws InterruptedException
     */
    private void performFileLocking() throws InterruptedException {
        System.out.println("---I have acquired the lock.---");;
        System.out.println("---Going to wait for ten seconds...---");
        LockFileDemo.lockFile();
        Thread.sleep(10000);
        LockFileDemo.releaseLock();
        System.out.println("---Done.---");
        System.out.println("---Leaving.---");
    }
}
