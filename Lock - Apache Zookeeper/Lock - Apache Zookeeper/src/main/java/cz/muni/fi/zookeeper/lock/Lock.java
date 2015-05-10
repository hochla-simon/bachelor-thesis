/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.zookeeper.lock;

import java.util.Collections;
import java.util.List;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;

/**
 *
 * @author simon
 */
public class Lock extends SyncPrimitive {
    
    private String nodePath = null;
    
    Lock(String host, int port, String root) throws KeeperException, InterruptedException {
        super(host + ":" + port);
        this.root = root;

        if (zk != null) {
            Stat s = zk.exists(root, false);
            if (s == null) {
                zk.create(root, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            }
        }
    }

    /**
     * Wait until I will acquire the global lock.
     * @throws KeeperException
     * @throws InterruptedException 
     */
    public void lock() throws KeeperException, InterruptedException {      
        nodePath = null;
        if (zk != null) {
            //create a node under the root with a name
            //ending with a sequentially incremented index
            nodePath = zk.create(root + "/guid-lock-", new byte[0],
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
                break;
            }
        }
    }
    
    /**
     * Release the global lock.
     * @throws InterruptedException
     * @throws KeeperException 
     */
    public void unlock() throws InterruptedException, KeeperException {
        if (nodePath != null) {
            zk.delete(nodePath, -1);
            nodePath = null;
        }
    }
        
    /**
     * Wait until the given Zookeeper node is deleted.
     * @param nodeToWaitFor
     * @throws KeeperException
     * @throws InterruptedException 
     */
    private void waitForNodeDeletion(String nodeToWaitFor) throws KeeperException, InterruptedException {
        while (true) {
            synchronized (mutex) {
                //testing whether the node has not been removed
                if (zk.exists(nodeToWaitFor, true) == null) {
                    break;
                }
                mutex.wait();
            }
        }
    }
}
