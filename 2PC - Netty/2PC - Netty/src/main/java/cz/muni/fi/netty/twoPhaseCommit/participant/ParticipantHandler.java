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
package cz.muni.fi.netty.twophasecommit.participant;

import cz.muni.fi.netty.twophasecommit.main.LockFileDemo;
import static cz.muni.fi.netty.twophasecommit.main.LockFileDemo.TRANSACTION_DATA;
import cz.muni.fi.netty.twophasecommit.main.LockFileDemo.TransactionDecision;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles a client-side channel.
 */
@Sharable
public class ParticipantHandler extends SimpleChannelInboundHandler<String> {
    
    private FileLock lock = null;
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws IOException, InterruptedException {
        
        switch(msg) {
            case "canCommit?": {
                String decision;
                if (TransactionDecision.commit.equals(LockFileDemo.decideTransaction())) {
                    decision = "commit";
                    //lock resources
                    lock = LockFileDemo.lockFile();
                } else {
                    decision = "abort";
                }
                ctx.writeAndFlush(decision + "\r\n");
                break;
            }
            case "commited": {
                LockFileDemo.writeToFile(TRANSACTION_DATA);
                //release locked resources
                LockFileDemo.releaseLock(lock);
                //acknowledge having received the result
                ctx.writeAndFlush("ACK" + "\r\n");
                printResult("commited");
                ctx.close();
                break;
            }
            case "aborted": {
                //release locked resources if they have been locked
                if (lock != null) {
                    LockFileDemo.releaseLock(lock);
                }
                //acknowledge having received the result
                ctx.writeAndFlush("ACK" + "\r\n");
                printResult("aborted");
                ctx.close();
                break;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Logger.getLogger(ParticipantHandler.class.getName()).log(Level.SEVERE, null, cause);
        ctx.close();
    }
    
    private static void printResult(String result) {
        System.out.println("Transaction has been " + result + ".");
    }
}