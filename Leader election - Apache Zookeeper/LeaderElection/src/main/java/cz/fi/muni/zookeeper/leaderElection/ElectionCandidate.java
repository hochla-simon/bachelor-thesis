/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.fi.muni.zookeeper.leaderElection;

import static cz.fi.muni.zookeeper.leaderElection.SyncPrimitive.zk;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;

/**
 *
 * @author Simon
 */
public class ElectionCandidate extends SyncPrimitive {

    public static final String ADDRESS = "127.0.0.1";
    public static final int PORT = 2181;
    
    ElectionCandidate(String address, String root) throws KeeperException, InterruptedException {
        super(address);
        this.root = root;

        //Create root node
        if (zk != null) {
            Stat s = zk.exists(root, false);
            if (s == null) {
                zk.create(root, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            }
        }
    }

    /**
     * Wait until I am elected as the leader and then perform the leader procedure.
     * 
     * @throws KeeperException
     * @throws InterruptedException 
     */
    public void becomeElectable() throws KeeperException, InterruptedException {
        String nodePath = null;
        if (zk != null) {
            //create a node under the root with a name
            //ending with a sequentially incremented index
            nodePath = zk.create(root + "/guid-n_", new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        }
        //parse out the part after the last slash
        String nodeName = nodePath.substring(nodePath.lastIndexOf("/") + 1);

        while (true) {
            List<String> children = null;
            //get names of all children under the root node
            children = zk.getChildren(root, true);
            //sort children names alphabetically
            Collections.sort(children);

            int index = children.indexOf(nodeName);
            //testing whether I am the child with the smallest index
            if (index != 0) {
                String nodeToWaitFor = null;
                //my local leader is the first node before me
                nodeToWaitFor = root + "/" + children.get(index - 1);
                //wait until it is deleted by either terminating after a failure
                //or ceasing having the leadership
                waitForNodeDeletion(nodeToWaitFor);
            } else {
                //I am the leader now
                leaderProcedure(nodeName);
                break;
            }
        }
    }

    /**
     * Wait until the given node is deleted.
     * 
     * @param nodeToWaitFor node to wait for
     * @throws KeeperException
     * @throws InterruptedException 
     */
    private void waitForNodeDeletion(String nodeToWaitFor) throws KeeperException, InterruptedException {
        while (true) {
            synchronized (mutex) {
                //testing whether the node has been removed
                if (zk.exists(nodeToWaitFor, true) == null) {
                    break;
                }
                mutex.wait();
            }
        }
    }

    /**
     * Set 'nodeName' as the current leader name and write it to console.
     * @param nodeName
     * @throws KeeperException
     * @throws InterruptedException 
     */
    public void leaderProcedure(String nodeName) throws KeeperException, InterruptedException {
        //create node under the root for the leader designation
        String leaderPath = root + "/leader";
        if (zk != null) {
            Stat s = zk.exists(leaderPath, false);
            if (s == null) {
                zk.create(leaderPath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            }
        }
        //set the node data with the name of the new leader
        byte[] data;
        data = nodeName.getBytes();
        zk.setData(leaderPath, data, -1);
        System.out.println("Now the leader is: " + nodeName);
    }

    /**
     * Wait until I am elected as the leader, sleep for 5 seconds and then shut down 
     * 
     * @param args
     * @throws InterruptedException
     * @throws KeeperException
     * @throws UnsupportedEncodingException 
     */
    private static void run() throws InterruptedException, KeeperException, UnsupportedEncodingException {
        ElectionCandidate coordinator = new ElectionCandidate(ADDRESS + ":" + PORT, "/ELECTION");
        coordinator.becomeElectable();
        Thread.sleep(5000);
    }
    
    public static void main(String args[]) {
        try {
            run();
        } catch (UnsupportedEncodingException | InterruptedException | KeeperException ex) {
            Logger.getLogger(SyncPrimitive.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
