/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.zookeeper.threePhaseCommit.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Simon
 */
public class LockFileDemo {
    //path to the locked file
    private static final String FILE_PATH = "/home/simon/Desktop/";
    //name of the locked file
    private static final String FILE_NAME = "file.txt";
    //file to lock
    static final File LOCK_FILE = new File(FILE_PATH, FILE_NAME);
    
    private static FileLock fileLock = null;
    private static RandomAccessFile file = null;

    /**
     * Lock the file LOCK_FILE if possible, otherwise exit with error
     * @return the locked file
     * @throws IOException
     * @throws InterruptedException
     */
    public static void lockFile() {
        try {
            LockFileDemo.file = new RandomAccessFile(LockFileDemo.LOCK_FILE, "rw");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
        FileChannel fileChannel = LockFileDemo.file.getChannel();
        FileLock lock = null;
        try {
            lock = fileChannel.tryLock();
        } catch (IOException ex) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (lock == null || !lock.isValid()) {
            System.out.println("Lock for file " + LockFileDemo.file + " cannot be applied.");
            System.exit(1);
        }
        LockFileDemo.fileLock = lock;
    }

    /**
     * Release the lock 'lock' and close the file 'file'
     * @param fileLock file to lock
     * @throws IOException
     */
    public static void releaseLock() {
        if (LockFileDemo.fileLock != null && LockFileDemo.fileLock.isValid()) {
            try {
                LockFileDemo.fileLock.release();
            } catch (IOException ex) {
                Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            System.out.println("Lock " + LockFileDemo.fileLock + " is unvalid.");
        }
        try {
            LockFileDemo.file.close();
            file = null;
        } catch (IOException ex) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void writeToFile(String data) {
        if (LockFileDemo.file == null) {
            System.out.println("File " + LockFileDemo.file + " is null and cannot be written to.");
            System.exit(1);
        }
        byte[] answerByteArray = data.getBytes();
        ByteBuffer byteBuffer = ByteBuffer.wrap(answerByteArray);
        FileChannel f = LockFileDemo.file.getChannel();
        try {
            f.position(file.length());
            while (byteBuffer.hasRemaining()) {
                f.write(byteBuffer);
            }
        } catch (IOException e) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
}
