/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.fi.muni.leader_election;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

public class SyncPrimitive implements Watcher {
    static ZooKeeper zk = null;
    static Integer mutex;

    String root;

    SyncPrimitive(String address) {
        if (zk == null) {
            try {
                zk = new ZooKeeper(address, 3000, this);
                mutex = new Integer(-1);
            } catch (IOException e) {
                System.out.println(e.toString());
                zk = null;
            }
        }
        //else mutex = new Integer(-1);
    }

    @Override
    synchronized public void process(WatchedEvent event) {
        synchronized (mutex) {
            //System.out.println("Process: " + event.getType());
            mutex.notify();
        }
    }
    
    public static class ElectionCandidate extends SyncPrimitive {
        
        ElectionCandidate(String address, String root) throws KeeperException, InterruptedException {
            super(address);
            this.root = root;

            // Create transaction node
            if (zk != null) {
                Stat s = zk.exists(root, false);
                if (s == null) {
                    zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE,
                            CreateMode.PERSISTENT);
                }
            }
        }
        
        public void becomeElectable() throws KeeperException, InterruptedException {
            String nodePath = null;
            if (zk != null) {
                nodePath = zk.create(root + "/guid-n_", new byte[0],
                        Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            }
            //parse out the part after the last slash
            int lastSlashIndex = nodePath.lastIndexOf("/");
            String node = nodePath.substring(lastSlashIndex + 1);
            
            while(true) {
                List<String> children = null;
                children = zk.getChildren(root, true);
                Collections.sort(children);
                
                int index = children.indexOf(node);
                //testing whether I am the child with the smallest index
                if (index == 0) {
                    //I am the leader now
                    leaderProcedure(node);
                    return;
                }
                String nodeToWaitFor = null;
                //my local leader is the first node before me
                nodeToWaitFor = root + "/" + children.get(index-1);
                waitForLocalLeaderDeletion(nodeToWaitFor);
            }
        }
        
        private void waitForLocalLeaderDeletion(String nodeToWaitFor) throws KeeperException, InterruptedException {
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
        public void leaderProcedure(String node) throws KeeperException, InterruptedException {
            //create node under the root for the leader designation
            String leaderPath = root + "/leader";
            if (zk != null) {
                Stat s = zk.exists(leaderPath, false);
                if (s == null) {
                    zk.create(leaderPath, new byte[0], Ids.OPEN_ACL_UNSAFE,
                            CreateMode.PERSISTENT);
                }
            }
            //set the node data with the name of the new leader
            byte[] data;
            data = node.getBytes();
            zk.setData(leaderPath, data, -1);
            System.out.println("Now the leader is: " + node);
        }
    }
    
    public static void main(String args[])  {
        try {
            leaderElectionTest(args);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SyncPrimitive.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(SyncPrimitive.class.getName()).log(Level.SEVERE, null, ex);
        } catch (KeeperException ex) {
            Logger.getLogger(SyncPrimitive.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void leaderElectionTest(String args[]) throws InterruptedException, KeeperException, UnsupportedEncodingException {
        ElectionCandidate coordinator = new ElectionCandidate(args[0], "/ELECTION");
        coordinator.becomeElectable();
        Thread.sleep(5000);
    }
}
