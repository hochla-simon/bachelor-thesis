/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.netty.lock.main;

import cz.muni.fi.netty.lock.coordinator.Coordinator;
import cz.muni.fi.netty.lock.participant.Participant;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LockFileDemo {
   
    private static final String FILE_PATH = "C:\\Users\\Simon\\Desktop\\";
    private static final String FILE_NAME = "file.txt";
    
    private static final File lockFile = new File(FILE_PATH, FILE_NAME);
    private static RandomAccessFile file = null;
    private static FileLock lock = null;
    
    public static final int SITES_COUNT = 3;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Wrong count of arguments: "
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
    
    public static void lockFile() throws InterruptedException {
        try {
            // This will create the file if it doesn't exit.
            file = new RandomAccessFile(lockFile, "rw");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
        FileChannel f = file.getChannel();

        FileLock fileLock = null;
        try {
            fileLock = f.tryLock();
        } catch (IOException ex) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (fileLock == null || !fileLock.isValid()) {
            System.out.println("Lock for file " + file + " cannot be applied.");
            System.exit(1);
        }
        lock = fileLock;
    }
    
    public static void releaseLock() {
        if (lock != null && lock.isValid()) {
            try {
                lock.release();
            } catch (IOException ex) {
                Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            System.out.println("Lock " + lock + " is unvalid.");
        }
        if (file != null) {
            try {
                file.close();
            } catch (IOException ex) {
                Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}