/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.zookeeper.threePhaseCommit.main;

import cz.muni.fi.zookeeper.threePhaseCommit.Coordinator;
import cz.muni.fi.zookeeper.threePhaseCommit.Participant;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.zookeeper.KeeperException;

public class Main {
    
    public enum TransactionDecision {
        commit, abort
    };
    
    //site's decision in transaction
    private static final TransactionDecision TRANSACTION_DECISION = TransactionDecision.commit;
    public static final String TRANSACTION_DATA = "Lorem ipsum dolor sit amet.";
    
    //transaction to be performed on the participant
    private static final String TRANSACTION = "transactionToPerform";
    
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 2181;
    
    private static final int SITES_COUNT = 2;
    
    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                System.out.println("Wrong count of parameters: "
                        + "run either with 'participant' or 'coordinator' as argument.");
                System.exit(0);
            }
            switch (args[0]) {
                case "participant":
                    Participant.run(HOST, PORT, SITES_COUNT);
                    break;
                case "coordinator":
                    Coordinator.run(HOST, PORT, SITES_COUNT, TRANSACTION);
                    break;
                default:
                    System.out.println("Wrong type of argument: "
                            + "run either with 'participant' or 'coordinator' as argument.");
                    System.exit(0);
            }
        } catch (InterruptedException | KeeperException | UnsupportedEncodingException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        
    /** 
     * Returns the site's transaction decision
     * @return the site's decision to commit or abort
     */
    public static TransactionDecision decideTransaction() {
        return TRANSACTION_DECISION;
    }
    
}