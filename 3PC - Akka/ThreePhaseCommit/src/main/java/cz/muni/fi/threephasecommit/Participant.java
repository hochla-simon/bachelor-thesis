/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.threephasecommit;

import akka.actor.UntypedActor;
import cz.muni.fi.threephasecommit.LockFileDemo.TransactionDecision;
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
                 } case abort: {
                     if (lock != null) {
                         LockFileDemo.releaseLock(lock);
                     }
                     getContext().stop(getSelf());
                     break;
                 } case doCommit: {
                    LockFileDemo.releaseLock(lock);
                    getSender().tell(Decision.haveCommited, getSelf());
                    getContext().stop(getSelf());
                }
            }
        } else {
            unhandled(msg);
        }
    }
}