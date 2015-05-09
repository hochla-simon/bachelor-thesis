/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.netty.twophasecommit.main;

import cz.muni.fi.netty.twophasecommit.coordinator.Coordinator;
import cz.muni.fi.netty.twophasecommit.participant.Participant;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LockFileDemo {
    
    public enum TransactionDecision {
        commit, abort
    };
    
    private static final String FILE_PATH = "C:\\Users\\Simon\\Desktop\\";
    private static final String FILE_NAME = "file.txt";
        
    private static final File LOCK_FILE = new File(FILE_PATH, FILE_NAME);
    private static RandomAccessFile file = null;
    
    public static final int SITES_COUNT = 1;
    public static final String TRANSACTION_DATA = "Lorem ipsum dolor sit amet.";

    private static final TransactionDecision TRANSACTION_DECISION = TransactionDecision.commit;
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Wrong count of parameters: "
                    + "run either with 'participant' or 'coordinator' as argument.");
            System.exit(0);
        }
        try {
            switch (args[0]) {
                case "participant":
                    Participant.run();
                    break;
                case "coordinator":
                    Coordinator.run();
                    break;
                default:
                    System.out.println("Wrong type of argument: "
                            + "run either with 'participant' or 'coordinator' as argument.");
                    System.exit(0);
            }
        } catch (Exception e) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    /**
     * Locks the file 'file'
     * 
     * @return The acquired lock for the file 'file' 
     * @throws IOException
     * @throws InterruptedException 
     */
    public static FileLock lockFile() {
        try {
            // This will create the file if it doesn't exit.
            file = new RandomAccessFile(LOCK_FILE, "rw");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
        FileChannel f = file.getChannel();

        FileLock lock = null;
        try {
            lock = f.tryLock();
        } catch (IOException ex) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (lock == null || !lock.isValid()) {
            System.out.println("Lock for file " + file + " cannot be applied.");
            System.exit(1);
        }
        return lock;
    }
    
    /**
     * Release the lock 'lock' and close the file 'file'
     *
     * @throws IOException
     */
    public static void releaseLock(FileLock lock) {
        if (lock != null && lock.isValid()) {
            try {
                lock.release();
            } catch (IOException ex) {
                Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            System.out.println("Lock " + lock + " is unvalid.");
        }
        try {
            file.close();
        } catch (IOException ex) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Write the data string from the first position into the file 'file'
     * 
     * @param data string to write
     */
    public static void writeToFile(String data) {
        if (file == null) {
            System.out.println("File " + file + " is null and cannot be written to.");
            System.exit(1);
        }
        byte[] answerByteArray = data.getBytes();
        ByteBuffer byteBuffer = ByteBuffer.wrap(answerByteArray);
        
        FileChannel f = file.getChannel();

        // Move to the beginning of the file and write out the contents
        // of the byteBuffer.
        try {
            f.position(0);
            while (byteBuffer.hasRemaining()) {
                f.write(byteBuffer);
            }
        } catch (IOException e) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    /**
     * Returns the site's transaction decision
     *
     * @return the site's decision to commit or abort
     */
    public static TransactionDecision decideTransaction() {
        return TRANSACTION_DECISION;
    }
}