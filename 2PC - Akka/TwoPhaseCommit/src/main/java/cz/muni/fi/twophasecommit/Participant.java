/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.twophasecommit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
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
        if (msg == Coordinator.Request.canCommit) {
            Result result;
            if (TransactionDecision.commit.equals(LockFileDemo.decideTransaction())) {
                result = Result.commit;
                lock = LockFileDemo.lockFile();
            } else {
                result = Result.abort;
            }
            getSender().tell(result, getSelf());
        } else if (msg == Coordinator.Request.commit) {
            LockFileDemo.releaseLock(lock);
            getContext().stop(getSelf());
        } else if (msg == Coordinator.Request.abort) {
            if (lock != null) {
                LockFileDemo.releaseLock(lock);
            }
            getContext().stop(getSelf());
        } else {
            unhandled(msg);
        }
    }
}