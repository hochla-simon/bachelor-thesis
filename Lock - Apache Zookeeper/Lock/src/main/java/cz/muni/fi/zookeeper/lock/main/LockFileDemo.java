/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.zookeeper.lock.main;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 *
 * @author Simon
 */
public class LockFileDemo {
    
    private static final String FILE_NAME = "file.txt";
    private static final String FILE_PATH = "/home/simon/Desktop/";
    static final File lockFile = new File(FILE_PATH, FILE_NAME);
    
    static FileLock fileLock = null;
    static RandomAccessFile file = null;

    public static void lockFile() throws IOException, InterruptedException {
        LockFileDemo.file = new RandomAccessFile(LockFileDemo.lockFile, "rw");
        FileChannel fileChannel = LockFileDemo.file.getChannel();
        FileLock lock = fileChannel.tryLock();
        if (lock == null || !lock.isValid()) {
            System.out.println("Lock for file " + LockFileDemo.file + " cannot be applied.");
            System.exit(1);
        }
        LockFileDemo.fileLock = lock;
    }

    public static void releaseLock() throws IOException {
        if (LockFileDemo.fileLock != null && LockFileDemo.fileLock.isValid()) {
            LockFileDemo.fileLock.release();
        } else {
            System.out.println("Lock " + LockFileDemo.fileLock + " is unvalid.");
        }
        if (LockFileDemo.file != null) {
            LockFileDemo.file.close();
            LockFileDemo.file = null;
        }
    }
    
}
