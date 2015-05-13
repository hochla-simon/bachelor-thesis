package cz.muni.fi.infinispan.leaderelection;

import java.io.IOException;
import java.util.ArrayList;
import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.infinispan.commons.util.CloseableIteratorCollection;
import org.infinispan.commons.util.CloseableIteratorSet;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;

public class ElectionCandidate {

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
     * Initialize caches and become electable: put my address into the cache of
     * waiting members and perform leader procedure when I become the leader.
     */
    public void becomeElectable() {
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
            becomeLeader();
            embeddedCacheManager.stop();
        } else {
            LeaderAddressListener leaderAddressListener = new LeaderAddressListener();
            embeddedCacheManager.addListener(leaderAddressListener);
        }
    }

    /**
     * Inform other members that I have become the leader, perform the
     * leaderProcedure and clean respective cache entries after releasing the leadership.
     */
    private synchronized void becomeLeader() {
        leaderCache.put(myAddress, "");
        try {
            leaderProcedure();
        } catch (InterruptedException ex) {
            Logger.getLogger(ElectionCandidate.class.getName()).log(Level.SEVERE, null, ex);
        }
        electableMembersCache.remove(myIndex);
        leaderCache.remove(myAddress);
    }

    /**
     * Initializes the given cache manager, members cache and leader cache.
     * Next it initializes myAddress with the address of this node.
     */
    private void initializeCachesAndMyAddress() {
        try {
            embeddedCacheManager = new DefaultCacheManager("infinispan.xml");
        } catch (IOException ex) {
            Logger.getLogger(ElectionCandidate.class.getName()).log(Level.SEVERE, null, ex);
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

        @ViewChanged
        public synchronized void leaderAddressRemoved(ViewChangedEvent  e) {
            CloseableIteratorSet<Long> keySet = electableMembersCache.keySet();
            CloseableIteratorCollection<Address> values = electableMembersCache.values();
            
            List<Address> newMembers = e.getNewMembers();
            List<Address> oldMembers = e.getOldMembers();
            
            //copying oldMembers to a new mutable list
            List<Address> disconnectedMembers = new ArrayList<>();
            for (Address address : oldMembers) {
                disconnectedMembers.add(address);
            }

            //disconnectedMembers will contain only the nodes, which have been removed in this view change
            disconnectedMembers.removeAll(newMembers);
            
            //removing the disconnected nodes from the electableMembersCache
            for (Address disconnectedNode : disconnectedMembers) {
                if (values.contains(disconnectedNode)) {
                    for (Long nodeId : keySet) {
                        if (disconnectedNode.equals(electableMembersCache.get(nodeId))) {
                            electableMembersCache.remove(nodeId);
                        }
                    }
                }
            }
            
            if (!keySet.isEmpty()) {
                //converting CloseableIteratorSet to ArrayList to be able to acquire the minimal value
                List<Long> memberIndexes = new ArrayList<>();
                for (Long index : keySet) {
                    memberIndexes.add(index);
                }
                Long minIndex = Collections.min(memberIndexes);

                //if my index is the minimal index, I will become the leader
                if (minIndex.equals(myIndex)) {
                    becomeLeader();
                    embeddedCacheManager.stop();
                }
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
        System.out.println("---I have acquired the leadership.---");
        System.out.println("---Going to wait for ten seconds...---");
        Thread.sleep(10000);
        System.out.println("---Done.---");
        System.out.println("---Leaving.---");
    }

    public static void main(String[] argc) {
        ElectionCandidate coordinator = new ElectionCandidate();
        coordinator.becomeElectable();
    }
}
