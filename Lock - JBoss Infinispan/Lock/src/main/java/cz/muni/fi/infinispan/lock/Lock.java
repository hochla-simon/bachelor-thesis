package cz.muni.fi.infinispan.lock;

import cz.muni.fi.infinispan.lock.main.LockFileDemo;
import java.io.IOException;
import java.util.ArrayList;
import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.infinispan.commons.util.CloseableIteratorSet;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.remoting.transport.Address;

public class Lock {

    /**
     * Cache for the addresses of the electable members indexed by the
     * incremental value assigned when they entered the cache. After the member
     * performs the leadership method, member removes its entry from this cache.
     */
    private Cache<Long, Address> electableMembersCache;

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
    private Long myIndex;

    /**
     * Manager managing both leaderCache and embeddedMembersCache.
     */
    private EmbeddedCacheManager embeddedCacheManager = null;

    /**
     * Become electable: put my address into the cache of
     * waiting members and perform leader procedure when I become the leader.
     */
    public void askForLock() {
        initializeCachesAndMyAddress();

        myIndex = System.currentTimeMillis();
        electableMembersCache.put(myIndex, myAddress);

        CloseableIteratorSet<Long> keySet = electableMembersCache.keySet();

        //converting CloseableIteratorSet to ArrayList to be able to acquire the minimal value
        List<Long> memberIndexes = new ArrayList<>();
        for (Long index : keySet) {
            memberIndexes.add(index);
        }
        Long minIndex = Collections.min(memberIndexes);
        
        //if I am the only active electable member, I become the leader,
        //otherwise set listener on leaderCache triggered when the current leader finishes   
        if (myIndex.equals(minIndex)) {
            performLocking();
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

        @CacheEntryRemoved
        public synchronized void leaderAddressRemoved(CacheEntryRemovedEvent e) {
            if (e.isPre()) {
                CloseableIteratorSet<Long> keySet = electableMembersCache.keySet();

                //get the minimal index from electableMembersCache to determine the new leader
                if (!keySet.isEmpty()) {
                    //converting CloseableIteratorSet to ArrayList to be able to acquire the minimal value
                    List<Long> memberIndexes = new ArrayList<>();
                    for (Long index : keySet) {
                        memberIndexes.add(index);
                    }
                    Long minIndex = Collections.min(memberIndexes);

                    //if my index is the minimal index, I will become the leader
                    if (minIndex.equals(myIndex)) {
                        performLocking();
                        embeddedCacheManager.stop();
                    }
                }
            }
        }
    }

    /**
     * Lock resources, perform operation requiring unique access to resources
     * and release the lock.
     */
    private void performLocking() {
        lock();
        try {
            exclusiveResourceAccessOperation();
        } catch (InterruptedException ex) {
            Logger.getLogger(Lock.class.getName()).log(Level.SEVERE, null, ex);
        }
        unlock();
        embeddedCacheManager.stop();
    }

    /**
     * Write into console when performing the procedure and wait for some time
     * before finishing.
     *
     * @throws InterruptedException
     */
    private void exclusiveResourceAccessOperation() throws InterruptedException {
        System.out.println("---I have acquired the lock.---");
        System.out.println("---Going to wait for ten seconds...---");
        LockFileDemo.lockFile();
        Thread.sleep(10000);
        LockFileDemo.releaseLock();
        System.out.println("---Done.---");
        System.out.println("---Leaving.---");
    }
}
