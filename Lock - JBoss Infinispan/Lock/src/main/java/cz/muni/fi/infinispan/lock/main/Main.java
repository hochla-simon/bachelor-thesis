/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.infinispan.lock.main;

import cz.muni.fi.infinispan.lock.Lock;

public class Main {
    
    //site's decision in transaction
    private static final TransactionDecision TRANSACTION_DECISION = TransactionDecision.commit;
    
    public enum TransactionDecision {
        commit, abort
    };
    
    public static void main(String[] args) {
       Lock lock = new Lock();
       lock.askForLock();
    }
    
    /** 
     * Returns the site's transaction decision
     * @return the site's decision to commit or abort
     */
    public static TransactionDecision decideTransaction() {
        return TRANSACTION_DECISION;
    }
}