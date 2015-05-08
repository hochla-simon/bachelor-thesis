/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.fi.muni.zookeeper.twophasecommit;

import static cz.fi.muni.zookeeper.twophasecommit.SyncPrimitive.zk;
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

    private final int numberOfParticipants;
    private final String transaction;

    Coordinator(String address, String root, int numberOfParticipants, String transaction) throws KeeperException, InterruptedException {
        super(address);
        this.root = root;
        this.numberOfParticipants = numberOfParticipants;
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

    /**
     * Perform the two phase commit. Wait for joining of all sites, request for
     * their votes, decide result based on the collected votes,
     * report the result back to the transactions and finally, wait
     * for their acknowledgements.
     * @return true when the transaction has been commited, false otherwise
     * @throws InterruptedException
     * @throws KeeperException
     * @throws UnsupportedEncodingException 
     */
    boolean enter() throws InterruptedException, KeeperException, UnsupportedEncodingException {
        waitForAllSites();
        writeTransaction();
        SyncPrimitive.Decision result = decideResult();
        writeResult(result);
        waitForAcknowledgements();
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
     * Wait for joining of all sites.
     * @throws InterruptedException
     * @throws KeeperException 
     */
    private void waitForAllSites() throws InterruptedException, KeeperException {
        while (true) {
            synchronized (mutex) {
                List<String> list = null;
                list = zk.getChildren(root, true);
                if (list != null) {
                    if (list.size() == numberOfParticipants) {
                        return;
                    }
                }
                mutex.wait();
            }
        }
    }

    /**
     * Write the transaction string to the root node. 
     * @throws KeeperException
     * @throws InterruptedException 
     */
    private void writeTransaction() throws KeeperException, InterruptedException {
        byte[] data = transaction.getBytes();
        zk.setData(root, data, -1);
    }

    /**
     * Wait for either all sites decide for commit or at least one of the sites
     * decide for abort and than return the result.
     * @return the decision made, either commit or abort
     * @throws UnsupportedEncodingException
     * @throws InterruptedException
     * @throws KeeperException 
     */
    private SyncPrimitive.Decision decideResult() throws UnsupportedEncodingException, InterruptedException, KeeperException {
        Stat stat = new Stat();
        while (true) {
            synchronized (mutex) {
                int commitedCount = 0;
                for (String childName : zk.getChildren(root, false)) {
                    byte[] data = zk.getData(root + "/" + childName, true, stat);
                    String vote = new String(data, "UTF-8");
                    switch (vote) {
                        case "abort":
                            return SyncPrimitive.Decision.abort;
                        case "commit":
                            commitedCount++;
                    }
                }
                if (commitedCount == numberOfParticipants) {
                    return SyncPrimitive.Decision.commit;
                }
                mutex.wait();
            }
        }
    }

    /**
     * Wait until all participants have acknowledged the transaction.
     * @throws UnsupportedEncodingException
     * @throws InterruptedException
     * @throws KeeperException 
     */
    private void waitForAcknowledgements() throws UnsupportedEncodingException, InterruptedException, KeeperException {
        Stat stat = new Stat();
        while (true) {
            synchronized (mutex) {
                int acknowledgedCounter = 0;
                for (String childName : zk.getChildren(root, false)) {
                    byte[] data = zk.getData(root + "/" + childName, true, stat);
                    String vote = new String(data, "UTF-8");
                    if ("ACK".equals(vote)) {
                        acknowledgedCounter++;
                    }
                }
                if (acknowledgedCounter == numberOfParticipants) {
                    return;
                }
                mutex.wait();
            }
        }
    }
    
    /**
     * Write the transaction result to the root node.
     * @param result result of the transaction
     * @throws KeeperException
     * @throws InterruptedException 
     */
    private void writeResult(SyncPrimitive.Decision result) throws KeeperException, InterruptedException {
        byte[] data = null;
        switch (result) {
            case commit:
                data = "commit".getBytes();
                break;
            case abort:
                data = "abort".getBytes();
        }
        zk.setData(root, data, -1);
    }

    /**
     * Delete the coordinator / root node after all children have been deleted. 
     * @throws KeeperException
     * @throws InterruptedException 
     */
    void leave() throws KeeperException, InterruptedException {
        while (true) {
            synchronized (mutex) {
                List<String> list = zk.getChildren(root, true);
                if (list.size() > 0) {
                    mutex.wait();
                } else {
                    zk.delete(root, -1);
                    return;
                }
            }
        }
    }
    
    /**
     * Wait for the given count of sites to join the transaction 
     * than perform the two phase commit and print the result
     * whether the transaction has been commited or aborted.
     * @param host IP address of the server Zookeeper client is connected to
     * @param port port Zookeeper client is operating on
     * @param sitesCount Count of the sites participating in the two phase commit
     * @param transaction string with the transaction to performon the participants
     * @throws InterruptedException
     * @throws KeeperException
     * @throws UnsupportedEncodingException 
     */
    public static void run(String host, int port, int sitesCount, String transaction)
            throws InterruptedException, KeeperException, UnsupportedEncodingException {
        String address = host + ":" + port;
        Coordinator coordinator = new Coordinator(address, "/Tx", new Integer(sitesCount), transaction);

        boolean result = coordinator.enter();

        if (result) {
            System.out.println("Transaction was commited.");
        } else {
            System.out.println("Transaction was aborted.");
        }

        coordinator.leave();
    }
}
