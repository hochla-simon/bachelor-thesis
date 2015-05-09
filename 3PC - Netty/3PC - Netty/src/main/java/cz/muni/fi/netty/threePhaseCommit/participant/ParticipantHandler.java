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
import java.nio.channels.FileLock;
import cz.muni.fi.netty.threephasecommit.main.LockFileDemo;
import static cz.muni.fi.netty.threephasecommit.main.LockFileDemo.TRANSACTION_DATA;
import cz.muni.fi.netty.threephasecommit.main.LockFileDemo.TransactionDecision;
import static cz.muni.fi.netty.threephasecommit.main.LockFileDemo.writeToFile;
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
            case "canCommit": {
                String line;
                if (TransactionDecision.commit == LockFileDemo.decideTransaction()) {
                    line = "Yes";
                    lock = LockFileDemo.lockFile();
                } else {
                    line = "No";
                }
                ctx.writeAndFlush(line + "\r\n");
                break;
            }
            case "preCommit": {
                String line = "ACK";
                ctx.writeAndFlush(line + "\r\n");
                break;
            }
            case "doCommit": {
                writeToFile(TRANSACTION_DATA);
                LockFileDemo.releaseLock(lock);
                String line = "haveCommited";
                ctx.writeAndFlush(line + "\r\n");
                printResult("commited");
                break;
            }
            case "abort": {
                if (lock != null) {
                    LockFileDemo.releaseLock(lock);
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
        Logger.getLogger(LockFileDemo.class.getName()).log(Level.SEVERE, null, ctx);
        ctx.close();
    }
}