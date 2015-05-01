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
import com.typesafe.config.ConfigFactory;
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
    
    public static void performThreePhaseCommit(final List<String> paths) {

        // Create an Akka system
        ActorSystem coordinatorSystem = ActorSystem.create("3PCCoordinatorSystem", ConfigFactory.load("coordinator"));

        // create the result listener, which will print the result and shutdown the system
        final ActorRef coordinatorListener = coordinatorSystem.actorOf(Props.create(Listener.class), "listener");

        //create participants
        ActorRef participant1 = coordinatorSystem.actorOf(Props.create(Participant.class), "p1");
        ActorRef participant2 = coordinatorSystem.actorOf(Props.create(Participant.class), "p2");

        // create the master
        ActorRef coordinator = coordinatorSystem.actorOf(Props.create(new UntypedActorFactory() {
            public UntypedActor create() {
                return new Coordinator(paths, coordinatorListener);
            }
        }), "coordinator");

        // system.actorOf(Props.create(Terminator.class, participant1), "terminator");
        coordinatorSystem.actorOf(Props.create(Terminator.class, coordinator), "terminator2");
    }
}
