/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.akka.threephasecommit;

import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import com.typesafe.config.ConfigFactory;
import static cz.muni.fi.akka.threephasecommit.LockFileDemo.TRANSACTION_DATA;
import cz.muni.fi.akka.threephasecommit.LockFileDemo.TransactionDecision;
import java.nio.channels.FileLock;

/**
 *
 * @author Simon
 */
public class Participant extends UntypedActor{
    public static enum Decision {
        Yes, No, ACK, haveCommited
    }
    
    private FileLock lock = null;

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Coordinator.Request) {
            Coordinator.Request request = (Coordinator.Request)msg;
            switch (request) {
                case canCommit: {
                    Decision result;
                    if (TransactionDecision.commit.equals(LockFileDemo.decideTransaction())) {
                        result = Decision.Yes;
                        lock = LockFileDemo.lockFile();
                    } else {
                        result = Decision.No;
                    }
                    getSender().tell(result, getSelf());
                    break;
                } case preCommit: {
                    getSender().tell(Decision.ACK, getSelf());
                    break;
                }
                case abort: {
                    if (lock != null) {
                        LockFileDemo.releaseLock(lock);
                    }
                    getContext().stop(getSelf());
                    break;
                }
                case doCommit: {
                    LockFileDemo.writeToFile(TRANSACTION_DATA);
                    LockFileDemo.releaseLock(lock);
                    getSender().tell(Decision.haveCommited, getSelf());
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
        ActorSystem participantSystem = ActorSystem.create("3PCParticipantSystem", ConfigFactory.load("participant"));
    }
}