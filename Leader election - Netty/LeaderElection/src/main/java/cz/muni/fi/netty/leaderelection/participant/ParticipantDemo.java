/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.netty.leaderelection.participant;

/**
 *
 * @author Simon
 */
public class ParticipantDemo {
    public static void main(String[] args) {
        try {
            Participant.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
