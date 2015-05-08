/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.netty.twoPhaseCommit.main;

import cz.muni.fi.netty.twoPhaseCommit.coordinator.Coordinator;
import cz.muni.fi.netty.twoPhaseCommit.participant.Participant;
import java.io.File;
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
    
    private static final String FILE_PATH = "C:\\Users\\Simon\\Desktop\\";
    private static final String FILE_NAME = "file.txt";
        
    private static final File LOCK_FILE = new File(FILE_PATH, FILE_NAME);
    private static RandomAccessFile file = null;
    
    public static final int SITES_COUNT = 2;

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
    
    public static FileLock lockFile() throws IOException, InterruptedException {
        // This will create the file if it doesn't exit.
        file = new RandomAccessFile(LOCK_FILE, "rw");
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