package cz.muni.fi.zookeeper.threePhaseCommit;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import cz.muni.fi.zookeeper.threePhaseCommit.main.Main;
import cz.muni.fi.zookeeper.threePhaseCommit.main.LockFileDemo;
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

    public enum ParticipantVote {
        Yes, No, ACK, haveCommited
    }
   
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
     * 
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
        ParticipantVote decision;
        if (Main.TransactionDecision.commit.
                equals(Main.decideTransaction())) {
            decision = ParticipantVote.Yes;
            LockFileDemo.lockFile();
        } else {
            decision = ParticipantVote.No;
        }
        voteForCommit(decision);
        
        Coordinator.CoordinatorVote result;
        result = getPrecommitResult();
        
        if (result != Coordinator.CoordinatorVote.preCommit) {
            //release resources if they have been locked
            if (decision == ParticipantVote.Yes) {
                LockFileDemo.releaseLock();
            }
            return false;
        }
        
        sendAcknowledgement(ParticipantVote.ACK);
        result = getCommitResult();
        
        if (result != Coordinator.CoordinatorVote.doCommit) {
            //release resources
            LockFileDemo.releaseLock();
            return false;
        }
        //write the commited transaction data to the file
        LockFileDemo.writeToFile(Main.TRANSACTION_DATA);
        //release resources
        LockFileDemo.releaseLock();
        sendAcknowledgement(ParticipantVote.haveCommited);
        return true;
    }
    
    /**
     * Create the given site node under the root element
     * 
     * @throws KeeperException
     * @throws InterruptedException 
     */
    private void createSiteNode() throws KeeperException, InterruptedException {
        sitePath = zk.create(root + "/s_", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL);
    }
    
    /**
     * Wait for other sites to enter.
     * 
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

    /**
     * Send the acknowledgement to the coordinator.
     * 
     * @param acknowledgement
     * @throws KeeperException
     * @throws InterruptedException 
     */
    private void sendAcknowledgement(ParticipantVote acknowledgement)
            throws KeeperException, InterruptedException {
        if (acknowledgement != ParticipantVote.ACK &&
                acknowledgement != ParticipantVote.haveCommited) {
            throw new IllegalArgumentException(
                    "Acknowledgement must be either 'ACK' or 'haveCommited'.");
        }
        byte[] data = acknowledgement.name().getBytes();
        zk.setData(sitePath, data, -1);
    }
    
    /**
     * Wait for the coordinator to write the transaction string to the root node
     * and put it to the transaction variable.
     * 
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
     * 
     * @param vote Decision to commit or abort.
     * @throws KeeperException
     * @throws InterruptedException 
     */
    public void voteForCommit(ParticipantVote vote)
            throws KeeperException, InterruptedException {
        byte[] data;
        if (vote == ParticipantVote.Yes) {
            data = "Yes".getBytes();
        } else if (vote == ParticipantVote.No) {
            data = "No".getBytes();
        } else {
            throw new IllegalArgumentException("Vote must be either 'Yes' or 'No'.");
        }
        zk.setData(sitePath, data, -1);
    }

    /**
     * Wait for the coordinator to decide the transaction and return the result.
     * 
     * @return result of the transaction
     * @throws UnsupportedEncodingException
     * @throws KeeperException
     * @throws InterruptedException 
     */
    private Coordinator.CoordinatorVote getPrecommitResult()
            throws UnsupportedEncodingException, KeeperException, InterruptedException {
        Coordinator.CoordinatorVote vote;
        Stat stat = new Stat();
        while (true) {
            synchronized (mutex) {
                byte[] data = zk.getData(root, true, stat);
                String result = new String(data, "UTF-8");
                switch (result) {
                    case "preCommit":
                        vote = Coordinator.CoordinatorVote.preCommit;
                        return vote;
                    case "abort":
                        vote = Coordinator.CoordinatorVote.abort;
                        return vote;
                }
                mutex.wait();
            }
        }
    }
    
    /**
     * Wait for the coordinator to decide the transaction and return the result.
     *
     * @return result of the transaction
     * @throws UnsupportedEncodingException
     * @throws KeeperException
     * @throws InterruptedException
     */
    private Coordinator.CoordinatorVote getCommitResult()
            throws UnsupportedEncodingException, KeeperException, InterruptedException {
        Coordinator.CoordinatorVote vote;
        Stat stat = new Stat();
        while (true) {
            synchronized (mutex) {
                byte[] data = zk.getData(root, true, stat);
                String result = new String(data, "UTF-8");
                switch (result) {
                    case "abort":
                        vote = Coordinator.CoordinatorVote.abort;
                        return vote;
                    case "doCommit":
                        vote = Coordinator.CoordinatorVote.doCommit;
                        return vote;
                }
                mutex.wait();
            }
        }
    }
    
    /**
     * Join to the two phase commit and print whether the transaction was commited
     * or aborted.
     *
     * @param host IP address of the server Zookeeper client is connected to
     * @param port port Zookeeper client is operating on
     * @param sitesCount Count of the sites participating in the two phase
     * commit
     * @throws java.io.UnsupportedEncodingException
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
    }
}
