/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.threephasecommit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.routing.BroadcastGroup;
import cz.muni.fi.threephasecommit.Participant.Decision;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Simon
 */
public class Coordinator extends UntypedActor {
    
    public static enum Request {
        canCommit, preCommit, doCommit, abort
    }
    
    private final List<String> paths;

    private final int nrOfParticipants;

    private final ActorRef listener;
    private final ActorRef workerRouter;
    
    private final Set<ActorRef> participantsHavingAgreed;
    private final Set<ActorRef> participantsHavingCommited;
    private final Set<ActorRef> participantsHavingAcknowledged;
    
    public Coordinator(List<String> paths, ActorRef listener) {
        this.paths = paths;
        this.listener = listener;
        workerRouter = getContext().actorOf(new BroadcastGroup(paths).props(), "workerRouter");
        this.nrOfParticipants = paths.size();
        this.participantsHavingAgreed = new HashSet<ActorRef>();
        this.participantsHavingCommited = new HashSet<ActorRef>();
        this.participantsHavingAcknowledged = new HashSet<ActorRef>();
    }
    
    @Override
    public void preStart() {
        workerRouter.tell(Request.canCommit, getSelf());
    }
    
    @Override
    public void onReceive(Object message) {
        if (message instanceof Decision) {
            Decision result = (Decision) message;
            switch(result) {
                case No: {
                    workerRouter.tell(Request.abort, getSelf());
                    listener.tell("Transaction has been aborted.", getSelf());
                    getContext().stop(getSelf());
                    break;
                }
                case Yes: {
                    participantsHavingAgreed.add(getSender());
                    if (participantsHavingAgreed.size() == nrOfParticipants) {
                        workerRouter.tell(Request.preCommit, getSelf());
                    }
                    break;
                }
                case ACK: {
                    participantsHavingAcknowledged.add(getSender());
                    if (participantsHavingAcknowledged.size() == nrOfParticipants) {
                        workerRouter.tell(Request.doCommit, getSelf());
                        listener.tell("Transaction has been commited.", getSelf());
                    }
                    break;
                }
                case haveCommited: {
                    participantsHavingCommited.add(getSender());
                    if (participantsHavingCommited.size() == nrOfParticipants) {
                        workerRouter.tell(Request.doCommit, getSelf());
                        listener.tell("Transaction finished successfully.", getSelf());
                        getContext().stop(getSelf());
                    }
                    break;
                }
            }
        } else {
            unhandled(message);
        }
    }
    
    public static void performTwoPhaseCommit(final List<String> paths) {
        
        // Create an Akka system
        ActorSystem system = ActorSystem.create("2PCSystem");

        // create the result listener, which will print the result and shutdown the system
        final ActorRef listener = system.actorOf(Props.create(Listener.class), "listener");

        //create participants
        ActorRef participant1 = system.actorOf(Props.create(Participant.class), "p1");
//        ActorRef participant2 = system.actorOf(Props.create(Participant.class), "p2");
//        ActorRef participant3  = system.actorOf(Props.create(Participant.class), "p3");
        
        // create the master
        ActorRef coordinator = system.actorOf(Props.create(new UntypedActorFactory() {
            public UntypedActor create() {
                return new Coordinator(paths, listener);
            }
        }), "coordinator");
        
//        system.actorOf(Props.create(Terminator.class, participant1), "terminator");
        system.actorOf(Props.create(Terminator.class, coordinator), "terminator2");
    }
}
