/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.twophasecommit;

import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import com.typesafe.config.ConfigFactory;
import cz.muni.fi.twophasecommit.LockFileDemo.TransactionDecision;
import java.nio.channels.FileLock;

/**
 *
 * @author Simon
 */
public class Participant extends UntypedActor{
    public static enum Result {
        commit, abort
    }
    
    private FileLock lock = null;

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Coordinator.Request) {
            Coordinator.Request request = (Coordinator.Request)msg;
            switch(request) {
                case canCommit: {
                    Result result;
                    if (TransactionDecision.commit.equals(LockFileDemo.decideTransaction())) {
                        result = Result.commit;
                        lock = LockFileDemo.lockFile();
                    } else {
                        result = Result.abort;
                    }
                    System.out.println("Result from " + getSelf().path() + " is: " + result);
                    getSender().tell(result, getSelf());
                    break;
                } case commit: {
                    LockFileDemo.releaseLock(lock);
                    getContext().stop(getSelf());
                    break;
                } case abort: {
                    if (lock != null) {
                        LockFileDemo.releaseLock(lock);
                    }
                    getContext().stop(getSelf());
                    break;
                }
            }
        } else {
            unhandled(msg);
        }
    }
	
    public static void performTwoPhaseCommit() {
        ActorSystem participantSystem = ActorSystem.create("2PCParticipantSystem", ConfigFactory.load("participant"));
    }
}