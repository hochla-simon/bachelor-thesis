/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.akka.twophasecommit;

import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import com.typesafe.config.ConfigFactory;
import cz.muni.fi.akka.twophasecommit.LockFileDemo.TransactionDecision;
import java.nio.channels.FileLock;

/**
 *
 * @author Simon
 */
public class Participant extends UntypedActor{
    public static enum Decision {
        commit, abort, ACK
    }
    
    private FileLock lock = null;

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Coordinator.Request) {
            Coordinator.Request request = (Coordinator.Request) msg;
            switch(request) {
                case canCommit: {
                    Decision decision;
                    if (TransactionDecision.commit.equals(LockFileDemo.decideTransaction())) {
                        decision = Decision.commit;
                        //lock resources
                        lock = LockFileDemo.lockFile();
                    } else {
                        decision = Decision.abort;
                    }
                    System.out.println("Result from " + getSelf().path() + " is: " + decision);
                    getSender().tell(decision, getSelf());
                    break;
                } case commit: {
                    //release locked resources
                    LockFileDemo.releaseLock(lock);
                    //acknowledge having received the result
                    getSender().tell("ACK", getSelf());
                    getContext().stop(getSelf());
                    break;
                } case abort: {
                    //release resources if they have been locked
                    if (lock != null) {
                        LockFileDemo.releaseLock(lock);
                    }
                    //acknowledge having received the result
                    getSender().tell("ACK", getSelf());
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
}