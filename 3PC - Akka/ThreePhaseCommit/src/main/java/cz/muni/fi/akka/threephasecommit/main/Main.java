/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.akka.threephasecommit.main;

import cz.muni.fi.akka.threephasecommit.Coordinator;
import cz.muni.fi.akka.threephasecommit.Participant;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Simon
 */
public class Main {
    
    public enum TransactionDecision {
        commit, abort
    };
    
    private static final TransactionDecision TRANSACTION_DECISION = TransactionDecision.commit;
    public static final String TRANSACTION_DATA = "Lorem ipsum dolor sit amet.";

    private static final List<String> PARTICIPANT_NAMES = Arrays.asList("p1", "p2");

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Wrong count of parameters: "
                    + "run either with 'participant' or 'coordinator' as argument.");
            System.exit(0);
        }
        
        if (args[0].equals("participant")) {
            Participant.run();
        } else if (args[0].equals("coordinator")) {
            Coordinator.run(PARTICIPANT_NAMES);
        } else {
            System.out.println("Wrong type of argument: "
                    + "run either with 'participant' or 'coordinator' as argument.");
            System.exit(0);
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