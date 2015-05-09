/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.akka.twophasecommit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.routing.BroadcastGroup;
import com.typesafe.config.ConfigFactory;
import cz.muni.fi.akka.twophasecommit.Participant.Decision;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Simon
 */
public class Coordinator extends UntypedActor {
    
    public static enum Request {
        canCommit, commit, abort
    }
    
    private final int nrOfParticipants;

    private final ActorRef listener;
    private final ActorRef workerRouter;
    
    private final Set<ActorRef> participantsHavingCommmited;
    private final Set<ActorRef> participantsHavingAcknowledged;
    
    private String result;
    
    public Coordinator(List<String> paths, ActorRef listener) {
        this.listener = listener;
        workerRouter = getContext().actorOf(new BroadcastGroup(paths).props(), "workerRouter");
        this.nrOfParticipants = paths.size();
        this.participantsHavingCommmited = new HashSet<ActorRef>();
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
     * Receive decision or acknowledgement from a participant and act according to it.
     * Wait for acknowledgement from all participants and than print out the result via the listener.
     * @param message recieved message
     */
    @Override
    public void onReceive(Object message) {
        if (message instanceof Decision) {
            Decision decision = (Decision) message;
            switch (decision) {
                case commit: {
                    participantsHavingCommmited.add(getSender());
                    if (participantsHavingCommmited.size() == nrOfParticipants) {
                        result = "commited";
                    }
                    break;
                }
                case abort: {
                        result = "aborted";
                    break;
                }
                case ACK: {
                    participantsHavingAcknowledged.add(getSender());
                    listener.tell("Transaction has been " + result + ".", getSelf());
                    // Stops this actor and all its supervised children
                    getContext().stop(getSelf());
                }
            }
        } else {
            unhandled(message);
        }
    }
    
    /**
     * Creates participants in the given paths (possibly in a different actor system according to the coordination file),
     * then create the coordinator and perform the two phase commit.
     * @param paths Paths to the participant actors
     */
    public static void run(final List<String> paths) {
        
        // Create an Akka system
        ActorSystem coordinatorSystem = ActorSystem.create("2PCCoordinatorSystem", ConfigFactory.load("coordinator"));

        // create the result listener, which will print the result and shutdown the system
        final ActorRef coordinatorListener = coordinatorSystem.actorOf(Props.create(Listener.class), "listener");
		
        //create participants (they can be created remotely in different actor systems according to the coordination)
        ActorRef participant1 = coordinatorSystem.actorOf(Props.create(Participant.class), "p1");
        ActorRef participant2 = coordinatorSystem.actorOf(Props.create(Participant.class), "p2");
        
        //create the coordinator
        ActorRef coordinator = coordinatorSystem.actorOf(Props.create(new UntypedActorFactory() {
            public UntypedActor create() {
                return new Coordinator(paths, coordinatorListener);
            }
        }), "coordinator");
        
        //set the terminators triggered when the actors have finished
        coordinatorSystem.actorOf(Props.create(Terminator.class, coordinator), "coordinatorTerminator");
        coordinatorSystem.actorOf(Props.create(Terminator.class, participant1), "participant1Terminator");
        coordinatorSystem.actorOf(Props.create(Terminator.class, participant2), "participant2Terminator");
    }
}
