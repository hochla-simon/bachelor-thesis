/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.fi.muni.lock;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.zookeeper.KeeperException;

public class LockFileDemo {
    
    private static final String FILE_PATH = "/home/simon/Desktop/";
    private static final String FILE_NAME = "file.txt";
    
    private static final File lockFile = new File(FILE_PATH, FILE_NAME);
    private static RandomAccessFile file = null;
    
    
    public static void main(String[] args) {
        try {
            Lock lock = new Lock(args[0], "/_locknode_");
            lock.lock();
            FileLock fileLock = lockFile();
            Thread.sleep(5000);
            releaseLock(fileLock);
            lock.unlock();
        } catch (InterruptedException | KeeperException | IOException ex) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static FileLock lockFile() throws IOException, InterruptedException {
        // This will create the file if it doesn't exit.
        file = new RandomAccessFile(lockFile, "rw");
        FileChannel fileChannel = file.getChannel();

        FileLock lock = fileChannel.tryLock();

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
}