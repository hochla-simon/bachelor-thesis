/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.fi.muni.lock;

import java.io.UnsupportedEncodingException;
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
    
    private String nodePath;

    Lock(String address, String root) throws KeeperException, InterruptedException {
        super(address);
        this.root = root;

        if (zk != null) {
            Stat s = zk.exists(root, false);
            if (s == null) {
                zk.create(root, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            }
        }
        
        nodePath = null;
    }

    public void lock() throws KeeperException, InterruptedException {
        nodePath = zk.create(root + "/guid-lock-", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
            CreateMode.EPHEMERAL_SEQUENTIAL);
        //parse out the part after the last slash
        int lastSlashIndex = nodePath.lastIndexOf("/");
        String node = nodePath.substring(lastSlashIndex + 1);

        while (true) {
            List<String> children = null;
            children = zk.getChildren(root, true);
            Collections.sort(children);

            int index = children.indexOf(node);
            //testing whether I am the child with the smallest index
            if (index == 0) {
                //I have acquired the lock
                return;
            }
            String nodeToWaitFor = null;
            nodeToWaitFor = root + "/" + children.get(index - 1);
            waitForNodeDeletion(nodeToWaitFor);
        }
    }
    
    public void unlock() throws InterruptedException, KeeperException {
        zk.delete(nodePath, -1);
        nodePath = null;
    }
        
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
