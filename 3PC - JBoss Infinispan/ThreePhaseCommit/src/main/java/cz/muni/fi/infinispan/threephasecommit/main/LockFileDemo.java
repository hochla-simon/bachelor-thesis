/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.infinispan.threephasecommit.main;

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
    private static final String FILE_NAME = "file.txt";
    private static final String FILE_PATH = "C:\\Users\\Simon\\Desktop\\";
    static final File lockFile = new File(FILE_PATH, FILE_NAME);
    
    static FileLock fileLock = null;
    static RandomAccessFile file = null;

    /**
     * Returns the site's transaction decision
     *
     * @return the site's decision to commit or abort
     */
    public static void lockFile() {
        try {
            LockFileDemo.file = new RandomAccessFile(LockFileDemo.lockFile, "rw");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Cannot create or modify file at path: " + LockFileDemo.lockFile.getPath() + ".");
            System.exit(1);
        }
        FileChannel f = LockFileDemo.file.getChannel();
        FileLock lock = null;
        try {
            lock = f.tryLock();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (lock == null || !lock.isValid()) {
            System.out.println("Lock for file " + LockFileDemo.file + " cannot be applied.");
            System.exit(1);
        }
        LockFileDemo.fileLock = lock;
    }

    /**
     * Release the lock 'lock' and close the file 'file'
     *
     * @throws IOException
     */
    public static void releaseLock() {
        if (LockFileDemo.fileLock != null && LockFileDemo.fileLock.isValid()) {
            try {
                LockFileDemo.fileLock.release();
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            System.out.println("Lock " + LockFileDemo.fileLock + " is unvalid.");
        }
        try {
            LockFileDemo.file.close();
            file = null;
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
}
