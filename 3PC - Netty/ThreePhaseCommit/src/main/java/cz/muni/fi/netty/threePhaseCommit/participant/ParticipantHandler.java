/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package cz.muni.fi.netty.threephasecommit.participant;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.IOException;
import cz.muni.fi.netty.threephasecommit.main.Main;
import cz.muni.fi.netty.threephasecommit.main.Main.TransactionDecision;
import cz.muni.fi.netty.threephasecommit.main.LockFileDemo;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles a client-side channel.
 */
@Sharable
public class ParticipantHandler extends SimpleChannelInboundHandler<String> {
    
    private Decision decision = null;
    
    private enum Decision {
        Yes, No
    }
            
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws IOException, InterruptedException {
        
        switch(msg) {
            case "canCommit": {
                if (TransactionDecision.commit == Main.decideTransaction()) {
                    decision = Decision.Yes;
                    LockFileDemo.lockFile();
                } else {
                    decision = Decision.No;
                }
                ctx.writeAndFlush(decision.name() + "\r\n");
                break;
            }
            case "preCommit": {
                String line = "ACK";
                ctx.writeAndFlush(line + "\r\n");
                break;
            }
            case "doCommit": {
                LockFileDemo.writeToFile(Main.TRANSACTION_DATA);
                LockFileDemo.releaseLock();
                String line = "haveCommited";
                ctx.writeAndFlush(line + "\r\n");
                printResult("commited");
                break;
            }
            case "abort": {
                if (decision == Decision.Yes) {
                    LockFileDemo.releaseLock();
                }
                printResult("aborted");
                break;
            }
        }
        
    }
    
    private static void printResult(String result) {
        System.out.println("Transaction has been " + result + ".");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ctx);
        ctx.close();
    }
}