/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.akka.threephasecommit;

import cz.muni.fi.akka.threephasecommit.main.Main;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import com.typesafe.config.ConfigFactory;
import cz.muni.fi.akka.threephasecommit.main.Main.TransactionDecision;
import cz.muni.fi.akka.threephasecommit.main.LockFileDemo;

/**
 *
 * @author Simon
 */
public class Participant extends UntypedActor{
    public static enum Decision {
        Yes, No, ACK, haveCommited
    }
    
    private Decision decision = null;

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Coordinator.Request) {
            Coordinator.Request request = (Coordinator.Request)msg;
            switch (request) {
                case canCommit: {
                    if (TransactionDecision.commit == Main.decideTransaction()) {
                        decision = Decision.Yes;
                        LockFileDemo.lockFile();
                    } else {
                        decision = Decision.No;
                    }
                    getSender().tell(decision, getSelf());
                    break;
                }
                case preCommit: {
                    getSender().tell(Decision.ACK, getSelf());
                    break;
                }
                case abort: {
                    //release resources if they have been lock
                    if (Decision.Yes == decision) {
                        LockFileDemo.releaseLock();
                    }
                    printResult("aborted");
                    getContext().stop(getSelf());
                    break;
                }
                case doCommit: {
                    LockFileDemo.writeToFile(Main.TRANSACTION_DATA);
                    //release resources
                    LockFileDemo.releaseLock();
                    //acknowledge having received the final decision
                    getSender().tell(Decision.haveCommited, getSelf());
                    printResult("commited");
                    getContext().stop(getSelf());
                }
            }
        } else {
            unhandled(msg);
        }
    }
    
    /**
     * Create actor system for so that the coordinator could create participant
     * actors in it remotely.
     */
    public static void run() {
        ActorSystem.create("3PCParticipantSystem", ConfigFactory.load("participant"));
    }
    
    private void printResult(String result) {
        System.out.println("Transaction has been " + result + ".");
    }
}