/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.twophasecommit;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Simon
 */
public class CoordinatorDemo {
    public static void main(String[] args) {
		 List<String> paths = Arrays.asList("/user/p1", "/user/p2");
		if (args.length == 0 || args[0].equals("participant"))
			Participant.performTwoPhaseCommit();
		if (args.length == 0 || args[0].equals("coordinator"))
			Coordinator.performTwoPhaseCommit(paths);
    }
}
