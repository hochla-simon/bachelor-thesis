/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.fi.muni.infinispan.lock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.logging.Level;
import java.util.logging.Logger;

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
       Lock lock = new Lock();
       lock.acquireForLock();
    }
    
    /**
     * Lock the file LOCK_FILE if possible, otherwise exit with error
     * @throws IOException
     * @throws InterruptedException 
     */
    public static void lockFile() {
        try {
            // This will create the file if it doesn't exit.
            file = new RandomAccessFile(LOCK_FILE, "rw");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
        FileChannel fileChannel = file.getChannel();

        FileLock lock = null;
        try {
            lock = fileChannel.tryLock();
        } catch (IOException ex) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
        }

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
    public static void releaseLock()  {
        if (fileLock != null && fileLock.isValid()) {
            try {
                fileLock.release();
            } catch (IOException ex) {
                Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            System.out.println("Lock " + fileLock + " is unvalid.");
        }
        if (file != null) {
            try {
                file.close();
            } catch (IOException ex) {
                Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
            }
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