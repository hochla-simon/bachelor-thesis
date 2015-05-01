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
import akka.actor.UntypedActorFactory;
import akka.routing.BroadcastGroup;
import com.typesafe.config.ConfigFactory;
import cz.muni.fi.twophasecommit.Participant.Result;
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
    
    private final List<String> paths;

    private final int nrOfParticipants;

    private final ActorRef listener;
    private final ActorRef workerRouter;
    
    private final Set<ActorRef> participantsHavingCommmited;
    
    public Coordinator(List<String> paths, ActorRef listener) {
        this.paths = paths;
        this.listener = listener;
        workerRouter = getContext().actorOf(new BroadcastGroup(paths).props(), "workerRouter");
        this.nrOfParticipants = paths.size();
        this.participantsHavingCommmited = new HashSet<ActorRef>();
    }
    
    @Override
    public void preStart() {
            workerRouter.tell(Request.canCommit, getSelf());
    }
    
    @Override
    public void onReceive(Object message) {
        if (message instanceof Result) {
            Result result = (Result) message;
            switch (result) {
                case commit: {
                    participantsHavingCommmited.add(getSender());
                    if (participantsHavingCommmited.size() == nrOfParticipants) {
                        listener.tell("Transaction has been commited.", getSelf());
                        // Stops this actor and all its supervised children
                        getContext().stop(getSelf());
                    }
                    break;
                }
                case abort: {
                    listener.tell("Transaction has been aborted.", getSelf());
                    // Stops this actor and all its supervised children
                    getContext().stop(getSelf());
                    break;
                }
            }
        } else {
            unhandled(message);
        }
    }
    
     public static void performTwoPhaseCommit(final List<String> paths) {
        
        // Create an Akka system
        ActorSystem coordinatorSystem = ActorSystem.create("2PCCoordinatorSystem", ConfigFactory.load("coordinator"));

        // create the result listener, which will print the result and shutdown the system
        final ActorRef coordinatorListener = coordinatorSystem.actorOf(Props.create(Listener.class), "listener");
		
        //create participants
        ActorRef participant1 = coordinatorSystem.actorOf(Props.create(Participant.class), "p1");
        ActorRef participant2 = coordinatorSystem.actorOf(Props.create(Participant.class), "p2");
        ActorRef participant3 = coordinatorSystem.actorOf(Props.create(Participant.class), "p3");
        
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
