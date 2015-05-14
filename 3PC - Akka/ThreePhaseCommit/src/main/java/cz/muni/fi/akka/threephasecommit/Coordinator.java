/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.akka.threephasecommit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.routing.BroadcastGroup;
import com.typesafe.config.ConfigFactory;
import cz.muni.fi.akka.threephasecommit.Participant.Decision;
import java.util.ArrayList;
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
    
    private final int nrOfParticipants;

    private final ActorRef listener;
    private final ActorRef workerRouter;
    
    private final Set<ActorRef> participantsHavingAgreed;
    private final Set<ActorRef> participantsHavingCommited;
    private final Set<ActorRef> participantsHavingAcknowledged;
    
    public Coordinator(List<String> participantNames, ActorRef listener) {
        
        List<String> paths = new ArrayList<String>();
        for (String participantName: participantNames) {
            paths.add("/user/" + participantName);
        }
        this.listener = listener;
        workerRouter = getContext().actorOf(new BroadcastGroup(paths).props(), "workerRouter");
        this.nrOfParticipants = paths.size();
        this.participantsHavingAgreed = new HashSet<ActorRef>();
        this.participantsHavingCommited = new HashSet<ActorRef>();
        this.participantsHavingAcknowledged = new HashSet<ActorRef>();
    }
    
    /**
     * Request the participants via the router to vote about the transaction.
     */
    @Override
    public void preStart() {
        workerRouter.tell(Request.canCommit, getSelf());
    }
    
    /**
     * Receive decision or acknowledgement from a participant and act according
     * to it. Wait for acknowledgement from all participants and than
     * print out the result via the listener.
     *
     * @param message recieved message
     */
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
    
    /**
     * Creates participants in the given paths (possibly in a different actor
     * system according to the coordination file), then create the coordinator
     * and perform the two phase commit.
     *
     * @param paths Paths to the participant actors
     */
    public static void run(final List<String> participantNames) {

        // Create an Akka system
        ActorSystem coordinatorSystem = ActorSystem.create("3PCCoordinatorSystem", ConfigFactory.load("coordinator"));

        // create the result listener, which will print the result and shutdown the system
        final ActorRef coordinatorListener = coordinatorSystem.actorOf(Props.create(Listener.class), "listener");

        //create participants
        for (String participantName: participantNames) {
            ActorRef participant = coordinatorSystem.actorOf(Props.create(Participant.class), participantName);
            coordinatorSystem.actorOf(Props.create(Terminator.class, participant), participantName + "Terminator");
        }

        // create the master
        ActorRef coordinator = coordinatorSystem.actorOf(Props.create(new UntypedActorFactory() {
            public UntypedActor create() {
                return new Coordinator(participantNames, coordinatorListener);
            }
        }), "coordinator");
        coordinatorSystem.actorOf(Props.create(Terminator.class, coordinator), "coordinatorTerminator");
    }
}
