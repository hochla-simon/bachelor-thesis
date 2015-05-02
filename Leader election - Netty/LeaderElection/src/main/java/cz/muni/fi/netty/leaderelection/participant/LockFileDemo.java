/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.netty.leaderelection.participant;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class LockFileDemo {
    
    public enum TransactionDecision {
        commit, abort
    };
    
    private static final String FILE_PATH = "C:\\Users\\Simon\\Desktop\\";
    private static final String FILE_NAME = "file.txt";
    private static final TransactionDecision TRANSACTION_DECISION = TransactionDecision.commit;
    
    private static final File lockFile = new File(FILE_PATH, FILE_NAME);
    private static RandomAccessFile file = null;
    
    
    public static void main(String[] args) {
        try {
            Participant.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static FileLock lockFile() throws IOException, InterruptedException {
        // This will create the file if it doesn't exit.
        file = new RandomAccessFile(lockFile, "rw");
        FileChannel f = file.getChannel();

        FileLock lock = f.tryLock();

        if (lock == null || !lock.isValid()) {
            System.out.println("Lock for file " + file + " cannot be applied.");
            System.exit(1);
        }
        return lock;
    }
    
    public static void releaseLock(FileLock lock) throws IOException  {
        if (lock != null && lock.isValid()) {
            lock.release();
        } else {
            System.out.println("Lock " + lock + " is unvalid.");
        }
        if (file != null) {
            file.close();
        }
    }
    
    public static TransactionDecision decideTransaction() {
        return TRANSACTION_DECISION;
    }
}