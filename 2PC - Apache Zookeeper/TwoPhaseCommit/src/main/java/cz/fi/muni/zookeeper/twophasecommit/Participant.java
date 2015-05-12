/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.fi.muni.zookeeper.twophasecommit;

import cz.fi.muni.zookeeper.twophasecommit.main.Main;
import cz.fi.muni.zookeeper.twophasecommit.main.Main.TransactionDecision;
import static cz.fi.muni.zookeeper.twophasecommit.SyncPrimitive.zk;
import cz.fi.muni.zookeeper.twophasecommit.main.LockFileDemo;
import java.io.IOException;
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
public class Participant extends SyncPrimitive {

    private final int size;
    private String sitePath;
    private String transaction;

    Participant(String address, String root, int size) throws Exception {
        super(address);
        this.root = root;
        this.size = size;
        
        this.sitePath = null;
        this.transaction = null;

        if (zk != null) {
            Stat s = zk.exists(root, false);
            if (s == null) {
                throw new Exception("Root element does not exist");
            }
        }
    }

    /**
     * Perform the two phase commit and return the result.
     * @return true when the transaction has been commited, false otherwise
     * @throws KeeperException
     * @throws InterruptedException
     * @throws UnsupportedEncodingException
     * @throws IOException 
     */
    boolean enter() throws KeeperException, InterruptedException, UnsupportedEncodingException, IOException {
        createSiteNode();
        waitForAllSites();
        waitForTransaction();
        
        //decide transaction and lock resources when agree with commit
        SyncPrimitive.Decision decision;
        if (TransactionDecision.commit == Main.decideTransaction()) {
            decision = SyncPrimitive.Decision.commit;
            LockFileDemo.lockFile();
        } else {
            decision = SyncPrimitive.Decision.abort;
        }
        voteForCommit(decision);
        SyncPrimitive.Decision result = getResult();

        if (SyncPrimitive.Decision.commit == result) {
            LockFileDemo.writeToFile(Main.TRANSACTION_DATA);
        }
        
        //release resources
        if (decision == SyncPrimitive.Decision.commit) {
            LockFileDemo.releaseLock();
        }
        
        sendAcknowledgement();
        
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
    /**
     * Create the given site node under the root element
     * @throws KeeperException
     * @throws InterruptedException 
     */
    private void createSiteNode() throws KeeperException, InterruptedException {
        sitePath = zk.create(root + "/s_", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL);
    }
    
    /**
     * Wait for other sites to enter.
     * @throws KeeperException
     * @throws InterruptedException 
     */
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

    private void sendAcknowledgement() throws KeeperException, InterruptedException {
        byte[] data = "ACK".getBytes();
        zk.setData(sitePath, data, -1);
    }
    
    /**
     * Wait for the coordinator to write the transaction string to the root node
     * and put it to the transaction variable.
     * @throws UnsupportedEncodingException
     * @throws KeeperException
     * @throws InterruptedException 
     */
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

    /**
     * Write decision as data of the respective site node.
     * @param decision Decision to commit or abort.
     * @throws KeeperException
     * @throws InterruptedException 
     */
    public void voteForCommit(SyncPrimitive.Decision decision) throws KeeperException, InterruptedException {
        byte[] data;
        if (decision == SyncPrimitive.Decision.commit) {
            data = "commit".getBytes();
        } else {
            data = "abort".getBytes();
        }
        zk.setData(sitePath, data, -1);
    }

    /**
     * Wait for the coordinator to decide the transaction and return the result.
     * @return result of the transaction
     * @throws UnsupportedEncodingException
     * @throws KeeperException
     * @throws InterruptedException 
     */
    private SyncPrimitive.Decision getResult() throws UnsupportedEncodingException, KeeperException, InterruptedException {
        SyncPrimitive.Decision decision;
        Stat stat = new Stat();
        while (true) {
            synchronized (mutex) {
                byte[] data = zk.getData(root, true, stat);
                String result = new String(data, "UTF-8");
                switch (result) {
                    case "commit":
                        decision = SyncPrimitive.Decision.commit;
                        return decision;
                    case "abort":
                        decision = SyncPrimitive.Decision.abort;
                        return decision;
                }
                mutex.wait();
            }
        }
    }

    /**
     * Delete the respective site node and wait for all siblings to be deleted.
     * @throws KeeperException
     * @throws InterruptedException 
     */
    void leave() throws KeeperException, InterruptedException {
        zk.delete(sitePath, -1);
        while (true) {
            synchronized (mutex) {
                List<String> list = zk.getChildren(root, true);
                if (list.size() > 0) {
                    mutex.wait();
                } else {
                    return;
                }
            }
        }
    }
    
    /**
     * Join to the two phase commit, print whether the transaction was commited
     * or aborted and than release the used Zookeeper resources.
     *
     * @param host IP address of the server Zookeeper client is connected to
     * @param port port Zookeeper client is operating on
     * @param sitesCount Count of the sites participating in the two phase
     * commit
     * @throws InterruptedException
     * @throws KeeperException
     * @throws Exception
     */
    public static void run(String host, int port, int sitesCount)
            throws UnsupportedEncodingException, InterruptedException, KeeperException, Exception {
        String address = host + ":" + port;
        Participant participant = new Participant(address, "/Tx", new Integer(sitesCount));

        boolean result = participant.enter();

        if (result) {
            System.out.println("Transaction was commited.");
        } else {
            System.out.println("Transaction was aborted.");
        }

        participant.leave();
    }
}
