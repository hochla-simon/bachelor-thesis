/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.fi.muni.two_phase_commit;

import static cz.fi.muni.two_phase_commit.SyncPrimitive.zk;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileLock;
import java.util.List;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;

/**
 *
 * @author simon
 */
public class Site extends SyncPrimitive {

    int size;
    String sitePath = null;
    String transaction;

    Site(String address, String root, int size) throws Exception {
        super(address);
        this.root = root;
        this.size = size;

        if (zk != null) {
            Stat s = zk.exists(root, false);
            if (s == null) {
                throw new Exception("Root element does not exist");
            }
        }
    }

    boolean enter() throws KeeperException, InterruptedException, UnsupportedEncodingException, IOException {
        //create the given site node under the root element
        createSiteNode();
        //wait for other sites to enter
        waitForAllSites();
        //wait for the coordinator to write the transaction string to the root node
        //and put it to the transaction variable
        waitForTransaction();
        
        //decide transaction and lock resources when agree with commit
        FileLock lock = null;
        SyncPrimitive.Decision decision;
        if ("commit".equals(LockFileDemo.decideTransaction())) {
            decision = SyncPrimitive.Decision.commit;
            lock = LockFileDemo.lockFile();
        } else {
            decision = SyncPrimitive.Decision.abort;
        }
        
        //write decision as data of the site node
        voteForCommit(decision);
        //get final result
        SyncPrimitive.Decision result = getResult();
        
        //release resources
        if (decision == SyncPrimitive.Decision.commit) {
            LockFileDemo.releaseLock(lock);
        }
        
        //return true if commited
        switch (result) {
            case commit:
                return true;
            case abort:
                return false;
            default:
                return false;
        }
    }

    private void createSiteNode() throws KeeperException, InterruptedException {
        sitePath = zk.create(root + "/s_", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    private void waitForAllSites() throws KeeperException, InterruptedException {
        while (true) {
            synchronized (mutex) {
                List<String> list = zk.getChildren(root, true);
                if (list != null) {
                    if (list.size() == size) {
                        return;
                    }
                }
                mutex.wait();
            }
        }
    }

    private void waitForTransaction() throws UnsupportedEncodingException, KeeperException, InterruptedException {
        Stat stat = new Stat();
        while (true) {
            synchronized (mutex) {
                String _transaction = new String(zk.getData(root, true, stat), "UTF-8");
                if (!_transaction.equals("")) {
                    transaction = _transaction;
                    return;
                }
                mutex.wait();
            }
        }
    }

    public void voteForCommit(SyncPrimitive.Decision decision) throws KeeperException, InterruptedException {
        byte[] data;
        if (decision == SyncPrimitive.Decision.commit) {
            data = "commit".getBytes();
        } else {
            data = "abort".getBytes();
        }
        zk.setData(sitePath, data, -1);
    }

    private SyncPrimitive.Decision getResult() throws UnsupportedEncodingException, KeeperException, InterruptedException {
        SyncPrimitive.Decision decision;
        Stat stat = new Stat();
        while (true) {
            synchronized (mutex) {
                byte[] data = zk.getData(root, true, stat);
                String result = new String(data, "UTF-8");
                if (result.equals("commit")) {
                    decision = SyncPrimitive.Decision.commit;
                    return decision;
                } else if (result.equals("abort")) {
                    decision = SyncPrimitive.Decision.abort;
                    return decision;
                }
                mutex.wait();
            }
        }
    }

    boolean leave() throws KeeperException, InterruptedException {
        zk.delete(sitePath, -1);
        while (true) {
            synchronized (mutex) {
                List<String> list = zk.getChildren(root, true);
                if (list.size() > 0) {
                    mutex.wait();
                } else {
                    return true;
                }
            }
        }
    }
    
   

    public static void siteTest(String args[]) throws UnsupportedEncodingException, InterruptedException, KeeperException, Exception {
        Site site = new Site(args[0], "/Tx", new Integer(args[1]));

        boolean result = site.enter();

        if (result) {
            System.out.println("Transaction was commited.");
        } else {
            System.out.println("Transaction was aborted.");
        }

        site.leave();
    }
}
