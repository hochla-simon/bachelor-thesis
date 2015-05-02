/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.netty.lock.coordinator;

/**
 *
 * @author Simon
 */
public class CoordinatorDemo {
     public static final int SITES_COUNT = 1;
        
     public static void main(String[] args) {
        try {
            Coordinator.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
