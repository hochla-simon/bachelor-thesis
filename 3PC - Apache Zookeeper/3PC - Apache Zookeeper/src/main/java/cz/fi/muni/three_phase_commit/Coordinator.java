/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.fi.muni.three_phase_commit;

import java.io.UnsupportedEncodingException;
import java.util.List;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;

/**
 *
 * @author simon
 */
public class Coordinator extends SyncPrimitive {

    private final int size;
    private static final long TIMEOUT = 10000;
    
    public enum CoordinatorVote {
        canCommit, preCommit, doCommit, abort
    };

    Coordinator(String address, String root, int size, String transaction) throws KeeperException, InterruptedException {
        super(address);
        this.root = root;
        this.size = size;

        // Create transaction node
        if (zk != null) {
            Stat s = zk.exists(root, false);
            if (s == null) {
                zk.create(root, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            }
        }
    }

    boolean enter() throws InterruptedException, KeeperException, UnsupportedEncodingException {
        
        waitForAllSites();
        writeDecision(CoordinatorVote.canCommit);
        CoordinatorVote result = decideResult();
        writeDecision(result);
        if (!collectVotes(Site.SiteVote.ACK)) {
            writeDecision(CoordinatorVote.abort);
            return false;
        }
        writeDecision(CoordinatorVote.doCommit);
        return true;
    }

    private void waitForAllSites() throws InterruptedException, KeeperException {
        while (true) {
            synchronized (mutex) {
                List<String> list = null;
                list = zk.getChildren(root, true);
                if (list != null) {
                    if (list.size() == size) {
                        return;
                    }
                }
                mutex.wait();
            }
        }
    }

    private void writeDecision(CoordinatorVote coordinatorVote) throws KeeperException, InterruptedException {
        byte[] data = coordinatorVote.name().getBytes();
        zk.setData(root, data, -1);
    }

    private CoordinatorVote decideResult() throws UnsupportedEncodingException, InterruptedException, KeeperException {
        CoordinatorVote result = null;
        Stat stat = new Stat();
        while (true) {
            synchronized (mutex) {
                int canCommitCounter = 0;
                for (String childName : zk.getChildren(root, false)) {
                    byte[] data = zk.getData(root + "/" + childName, true, stat);
                    String vote = new String(data, "UTF-8");
                    if (vote.equals("")) break;
                    Site.SiteVote siteVote = Site.SiteVote.valueOf(vote);
                    
                    if (siteVote == Site.SiteVote.No) {
                        result = CoordinatorVote.abort;
                        break;
                    } else if (siteVote == Site.SiteVote.Yes) {
                        canCommitCounter++;    
                    }
                }
                if (result == CoordinatorVote.abort) {
                    break;
                }
                if (canCommitCounter == size) {
                    result = CoordinatorVote.preCommit;
                    break;
                }
                mutex.wait();
            }
        }
        return result;
    }

    private boolean collectVotes(Site.SiteVote awaitedVote) throws UnsupportedEncodingException, InterruptedException, KeeperException {
        Stat stat = new Stat();
        long start = 0;
        while (true) {
            synchronized (mutex) {
                int counter = 0;
                for (String childName : zk.getChildren(root, false)) {
                    byte[] data = zk.getData(root + "/" + childName, true, stat);
                    String vote = new String(data, "UTF-8");
                    if (vote.equals("")) {
                        break;
                    }
                    Site.SiteVote siteVote = Site.SiteVote.valueOf(vote);
                    if (siteVote == awaitedVote) {
                        counter++;
                    }
                }
                if (counter == size) break;
                
                start = System.currentTimeMillis();
                mutex.wait(TIMEOUT);
                if ((System.currentTimeMillis() - start) > TIMEOUT) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean leave() throws KeeperException, InterruptedException {
        while (true) {
            synchronized (mutex) {
                List<String> list = zk.getChildren(root, true);
                if (list.size() > 0) {
                    mutex.wait();
                } else {
                    zk.delete(root, -1);
                    return true;
                }
            }
        }
    }
    
    public static void coordinatorTest(String args[]) throws InterruptedException, KeeperException, UnsupportedEncodingException {
        Coordinator coordinator = new Coordinator(args[0], "/Tx", new Integer(args[1]), args[2]);

        boolean result = coordinator.enter();

        if (result) {
            System.out.println("Transaction was commited.");
        } else {
            System.out.println("Transaction was aborted.");
        }

        coordinator.leave();
    }
}
