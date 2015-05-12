/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.akka.twophasecommit;

import cz.muni.fi.akka.twophasecommit.main.Main;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import com.typesafe.config.ConfigFactory;
import cz.muni.fi.akka.twophasecommit.main.Main.TransactionDecision;
import cz.muni.fi.akka.twophasecommit.main.LockFileDemo;

/**
 *
 * @author Simon
 */
public class Participant extends UntypedActor{
    public static enum Decision {
        commit, abort, ACK
    }
    
    private Decision decision = null;

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Coordinator.Request) {
            Coordinator.Request request = (Coordinator.Request) msg;
            switch(request) {
                case canCommit: {
                    if (TransactionDecision.commit == Main.decideTransaction()) {
                        decision = Decision.commit;
                        //lock resources
                        LockFileDemo.lockFile();
                    } else {
                        decision = Decision.abort;
                    }
//                    System.out.println("Result from " + getSelf().path() + " is: " + decision);
                    getSender().tell(decision, getSelf());
                    break;
                } case commit: {
                    //write to file the commited data
                    LockFileDemo.writeToFile(Main.TRANSACTION_DATA);
                    //release locked resources
                    LockFileDemo.releaseLock();
                    //acknowledge having received the result
                    getSender().tell("ACK", getSelf());
                    printResult("commited");
                    getContext().stop(getSelf());
                    break;
                } case abort: {
                    //release resources if they have been locked
                    if (decision == Decision.commit) {
                        LockFileDemo.releaseLock();
                    }
                    //acknowledge having received the result
                    getSender().tell("ACK", getSelf());
                    printResult("aborted");
                    getContext().stop(getSelf());
                    break;
                }
            }
        } else {
            unhandled(msg);
        }
    }
	
    /**
     * Create actor system for so that the coordinator could create participant actors in it remotely. 
     */
    public static void run() {
        ActorSystem.create("2PCParticipantSystem", ConfigFactory.load("participant"));
    }
    
    private void printResult(String result) {
        System.out.println("Transaction has been " + result + ".");
    }
}