/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.fi.muni.two_phase_commit;

import static cz.fi.muni.two_phase_commit.Coordinator.coordinatorTest;
import static cz.fi.muni.two_phase_commit.Site.siteTest;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.zookeeper.KeeperException;

public class LockFileDemo {
    
    public enum TransactionDecision {
        commit, abort
    };
    
    //path to the locked file
    private static final String FILE_PATH = "/home/simon/Desktop/";
    //name of the locked file
    private static final String FILE_NAME = "file.txt";
    //file to lock
    private static final File LOCK_FILE = new File(FILE_PATH, FILE_NAME);
    
    private static RandomAccessFile file = null;
    private static FileLock fileLock = null;
    
    //site's decision in transaction
    private static final TransactionDecision TRANSACTION_DECISION = TransactionDecision.commit;
    
    public static void main(String[] args) {
        try {
            //uncommited the test you want to run
//            siteTest(args);
            coordinatorTest(args);
        } catch (InterruptedException ex) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (KeeperException ex) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Lock the file LOCK_FILE if possible, otherwise exit with error
     * @throws IOException
     * @throws InterruptedException 
     */
    public static void lockFile() throws IOException, InterruptedException {
        // This will create the file if it doesn't exit.
        file = new RandomAccessFile(LOCK_FILE, "rw");
        FileChannel fileChannel = file.getChannel();

        FileLock lock = fileChannel.tryLock();

        if (lock == null || !lock.isValid()) {
            System.out.println("Lock for file " + file + " cannot be applied.");
            System.exit(1);
        }
        fileLock = lock;
    }
    
    /**
     * Release the lock 'lock' and close the file 'file'
     * @throws IOException 
     */
    public static void releaseLock() throws IOException  {
        if (fileLock != null && fileLock.isValid()) {
            fileLock.release();
        } else {
            System.out.println("Lock " + fileLock + " is unvalid.");
        }
        if (file != null) {
            file.close();
        }
    }
    
    /** 
     * Returns the site's transaction decision
     * @return the site's decision to commit or abort
     */
    public static TransactionDecision decideTransaction() {
        return TRANSACTION_DECISION;
    }
}