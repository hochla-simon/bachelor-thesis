/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.fi.muni.two_phase_commit;

import static cz.fi.muni.two_phase_commit.Coordinator.coordinatorTest;
import static cz.fi.muni.two_phase_commit.Site.siteTest;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author simon
 */
public class Demo {
    public static void main(String args[]) throws Exception {
        try {
            //uncommited the test you want to run
            siteTest(args);
//            coordinatorTest(args);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SyncPrimitive.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
