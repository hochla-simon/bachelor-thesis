/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.fi.muni.two_phase_commit;

import static cz.fi.muni.two_phase_commit.SyncPrimitive.zk;
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

    private int size;
    private String transaction;

    Coordinator(String address, String root, int size, String transaction) throws KeeperException, InterruptedException {
        super(address);
        this.root = root;
        this.size = size;
        this.transaction = transaction;

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
        writeTransaction();
        SyncPrimitive.Decision result = decideResult();
        writeResult(result);
        switch (result) {
            case commit:
                return true;
            case abort:
                return false;
            default:
                return false;
        }
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

    private void writeTransaction() throws KeeperException, InterruptedException {
        byte[] data = transaction.getBytes();
        zk.setData(root, data, -1);
    }

    private SyncPrimitive.Decision decideResult() throws UnsupportedEncodingException, InterruptedException, KeeperException {
        SyncPrimitive.Decision result = null;
        Stat stat = new Stat();
        while (true) {
            synchronized (mutex) {
                int commitedCount = 0;
                for (String childName : zk.getChildren(root, false)) {
                    byte[] data = zk.getData(root + "/" + childName, true, stat);
                    String vote = new String(data, "UTF-8");
                    if (vote.equals("")) {
                        break;
                    } else if (vote.equals("abort")) {
                        result = SyncPrimitive.Decision.abort;
                        break;
                    } else if (vote.equals("commit")) {
                        commitedCount++;
                    }
                }
                if (result == SyncPrimitive.Decision.abort) {
                    break;
                }
                if (commitedCount == size) {
                    result = SyncPrimitive.Decision.commit;
                    break;
                }
                mutex.wait();
            }
        }
        return result;
    }

    private void writeResult(SyncPrimitive.Decision vote) throws KeeperException, InterruptedException {
        byte[] data = null;
        switch (vote) {
            case commit:
                data = "commit".getBytes();
                break;
            case abort:
                data = "abort".getBytes();
        }
        zk.setData(root, data, -1);
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
        cz.fi.muni.two_phase_commit.Coordinator coordinator = new cz.fi.muni.two_phase_commit.Coordinator(args[0], "/Tx", new Integer(args[1]), args[2]);

        boolean result = coordinator.enter();

        if (result) {
            System.out.println("Transaction was commited.");
        } else {
            System.out.println("Transaction was aborted.");
        }

        coordinator.leave();
    }
}
