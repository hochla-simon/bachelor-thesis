/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.akka.threephasecommit;

import akka.actor.UntypedActor;

/**
 *
 * @author Simon
 */
public class Listener extends UntypedActor {

    /**
     * Print the received message and shutdown the system.
     *
     * @param message received message
     */
    public void onReceive(Object message) {
        if (message instanceof String) {
            String result = (String) message;
            System.out.println(result);
            getContext().system().shutdown();
        } else {
            unhandled(message);
        }
    }
}
