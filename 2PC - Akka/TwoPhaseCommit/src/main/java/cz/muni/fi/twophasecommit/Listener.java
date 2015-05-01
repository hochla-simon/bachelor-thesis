/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.twophasecommit;

import akka.actor.UntypedActor;

/**
 *
 * @author Simon
 */
public class Listener extends UntypedActor {

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
