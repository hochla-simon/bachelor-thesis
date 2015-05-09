/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.fi.muni.zookeeper.threePhaseCommit;

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
    private static final long TIMEOUT = 5000;
    
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

    /**
     * Perform the two phase commit. Wait for joining of all sites, request for
     * their votes, decide result based on the collected votes, report the
     * result back to the transactions and finally, wait for their
     * acknowledgements.
     *
     * @return true when the transaction has been commited, false otherwise
     * @throws InterruptedException
     * @throws KeeperException
     * @throws UnsupportedEncodingException
     */
    boolean enter()throws InterruptedException, KeeperException, 
            UnsupportedEncodingException {
        
        waitForAllSites();
        
        //ask sites to give their votes about the transaction
        writeVote(CoordinatorVote.canCommit);
        
        //get the result after collecting all the votes
        CoordinatorVote result = decideResult();
        
        //send the result back to participants
        writeVote(result);
        
        //transaction was aborted by one of the participants
        if (result != CoordinatorVote.preCommit) {
            return false;
        }
        
        //if the collecting of acknowledgement votes timed out, abort the transaction
        if (!collectVotes(Participant.ParticipantVote.ACK)) {
            writeVote(CoordinatorVote.abort);
            return false;
        } else {
            //commit the transaction
            writeVote(CoordinatorVote.doCommit);
        }
        
        //inform when the final acknowledgement about the participant's success got lost
        if (!collectVotes(Participant.ParticipantVote.haveCommited)) {
            System.out.println("Error: Some participants have timed out before they "
                    + "acknowledged having commited the transaction.");
        }
        
        return true;
    }

    /**
     * Wait for joining of all sites.
     *
     * @throws InterruptedException
     * @throws KeeperException
     */
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

    /**
     * Put the coordinators' request to the root node's data.
     * @param coordinatorVote
     * @throws KeeperException
     * @throws InterruptedException 
     */
    private void writeVote(CoordinatorVote coordinatorVote) throws KeeperException, InterruptedException {
        byte[] data = coordinatorVote.name().getBytes();
        zk.setData(root, data, -1);
    }

    
    /**
     * Wait for either all sites decide for commit or at least one of the sites
     * decide for abort and than return the result.
     *
     * @return the decision made, either commit or abort
     * @throws UnsupportedEncodingException
     * @throws InterruptedException
     * @throws KeeperException
     */
    private CoordinatorVote decideResult() throws UnsupportedEncodingException, 
            InterruptedException, KeeperException {
        Stat stat = new Stat();
        while (true) {
            synchronized (mutex) {
                int canCommitCounter = 0;
                
                for (String childName : zk.getChildren(root, false)) {
                    byte[] data = zk.getData(root + "/" + childName, true, stat);
                    String vote = new String(data, "UTF-8");
                    
                    switch (vote) {
                        case "No":
                            return CoordinatorVote.abort;
                        case "Yes":    
                            canCommitCounter++;
                    }
                }
                
                if (canCommitCounter == size) {
                    return CoordinatorVote.preCommit;
                }
                mutex.wait();
            }
        }
    }

    private boolean collectVotes(Participant.ParticipantVote awaitedVote)
            throws UnsupportedEncodingException, InterruptedException, KeeperException {
        Stat stat = new Stat();
        long start = System.currentTimeMillis();
        while (true) {
            synchronized (mutex) {
                int counter = 0;
                
                for (String childName : zk.getChildren(root, false)) {
                    byte[] data = zk.getData(root + "/" + childName, true, stat);
                    String vote = new String(data, "UTF-8");
                    
                    Participant.ParticipantVote siteVote =
                            Participant.ParticipantVote.valueOf(vote);
                    if (siteVote == awaitedVote) {
                        counter++;
                    }
                }
                
                if (counter == size) return true;
                
                mutex.wait(TIMEOUT);
                if ((System.currentTimeMillis() - start) > TIMEOUT) {
                    return false;
                }
            }
        }
    }

    void leave() throws KeeperException, InterruptedException {
        List<String> list = zk.getChildren(root, true);
        for (String childName : list) {
            zk.delete(root + "/" + childName, -1);
        }
        zk.delete(root, -1);
    }
    
    /**
     * Wait for the given count of sites to join the transaction than perform
     * the two phase commit and print the result whether the transaction has
     * been commited or aborted.
     *
     * @param host IP address of the server Zookeeper client is connected to
     * @param port port Zookeeper client is operating on
     * @param sitesCount Count of the sites participating in the two phase
     * commit
     * @param transaction string with the transaction to performon the
     * participants
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
