/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.akka.threephasecommit.main;

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
    private static FileLock fileLock = null;
    private static RandomAccessFile file = null;
    private static final String FILE_PATH = "/home/xhochla/Bakalarka";
    private static final File lockFile = new File(FILE_PATH, FILE_NAME);

    /**
     * Write 'data' to the file 'file'.
     * 
     * @param data data string to write
     */
    public static void writeToFile(String data) {
        if (file == null) {
            System.out.println("File " + file + " is null and cannot be written to.");
            System.exit(1);
        }
        byte[] answerByteArray = data.getBytes();
        ByteBuffer byteBuffer = ByteBuffer.wrap(answerByteArray);
        FileChannel f = file.getChannel();
        try {
            f.position(file.length());
            while (byteBuffer.hasRemaining()) {
                f.write(byteBuffer);
            }
        } catch (IOException e) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * Returns the site's transaction decision
     *
     * @return the site's decision to commit or abort
     */
    public static void lockFile() {
        try {
            file = new RandomAccessFile(lockFile, "rw");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        FileChannel f = file.getChannel();
        FileLock lock = null;
        try {
            lock = f.tryLock();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (lock == null || !lock.isValid()) {
            System.out.println("Lock for file " + file + " cannot be applied.");
            System.exit(1);
        }
        fileLock = lock;
    }

    /**
     * Release the lock 'lock' and close the file 'file'
     *
     * @throws IOException
     */
    public static void releaseLock() {
        if (fileLock != null && fileLock.isValid()) {
            try {
                fileLock.release();
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            System.out.println("Lock " + fileLock + " is unvalid.");
        }
        try {
            file.close();
            file = null;
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
