/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.netty.leaderelection.main;

import cz.muni.fi.netty.leaderelection.coordinator.Coordinator;
import cz.muni.fi.netty.leaderelection.electioncandidate.ElectionCandidate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Simon
 */
public class Demo {
    /**
    * Count of the participating electable sites
    */
    public static final int SITES_COUNT = 3;
        
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Wrong count of arguments: "
                    + "run either with 'candidate' or 'coordinator' as argument.");
            System.exit(0);
        }
        try {
            switch (args[0]) {
                case "candidate":
                    ElectionCandidate.run();
                    break;
                case "coordinator":
                    Coordinator.run();
                    break;
                default:
                    System.out.println("Wrong type of argument: "
                            + "run either with 'candidate' or 'coordinator' as argument.");
                    System.exit(0);
            }
        } catch (Exception e) {
            Logger.getLogger(Demo.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
