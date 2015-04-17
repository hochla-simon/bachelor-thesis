/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.threephasecommit;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Simon
 */
public class CoordinatorDemo {
    public static void main(String[] args) {
//        List<String> paths = Arrays.asList("/user/p1", "/user/p2",
//                "/user/p3");
        List<String> paths = Arrays.asList("/user/p1");
        
        Coordinator.performTwoPhaseCommit(paths);
    }
}
