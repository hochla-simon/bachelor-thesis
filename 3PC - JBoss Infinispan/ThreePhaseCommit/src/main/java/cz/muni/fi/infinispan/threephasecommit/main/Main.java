/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.infinispan.threephasecommit.main;

import cz.muni.fi.infinispan.threephasecommit.Coordinator;
import cz.muni.fi.infinispan.threephasecommit.Participant;

/**
 *
 * @author Simon
 */
public class Main {

    private static final TransactionDecision TRANSACTION_DECISION = TransactionDecision.commit;
    public static final String TRANSACTION_DATA = "Lorem ipsum dolor sit amet.";

    public enum TransactionDecision {
        commit, abort
    };
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Wrong count of parameters: "
                    + "run either with 'participant' or 'coordinator' as argument.");
            System.exit(0);
        }
        
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
