/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.fi.muni.two_phase_commit;

import static cz.fi.muni.two_phase_commit.Coordinator.coordinatorTest;
import static cz.fi.muni.two_phase_commit.Site.siteTest;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.zookeeper.KeeperException;

public class LockFileDemo {
    
    private enum TransactionDecision {
        commit, abort
    };
    
    private static final String FILE_PATH = "/home/simon/Desktop/";
    private static final String FILE_NAME = "file.txt";
    private static final String TRANSACTION_DECISION = TransactionDecision.commit.name();
    
    private static final File lockFile = new File(FILE_PATH, FILE_NAME);
    private static RandomAccessFile file = null;
    
    
    public static void main(String[] args) {
        try {
            //uncommited the test you want to run
            siteTest(args);
//            coordinatorTest(args);
        } catch (InterruptedException ex) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (KeeperException ex) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ex);
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
    
    public static String decideTransaction() {
        return TRANSACTION_DECISION;
    }
}