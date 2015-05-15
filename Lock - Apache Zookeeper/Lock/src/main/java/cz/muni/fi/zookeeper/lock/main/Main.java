/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.zookeeper.lock.main;

import cz.muni.fi.zookeeper.lock.Lock;

public class Main {
    
    public static final String HOST = "127.0.0.1";
    public static final int PORT = 2181;
    
    public static void main(String args[]) {
        Lock.electionTest();
    }
        
}