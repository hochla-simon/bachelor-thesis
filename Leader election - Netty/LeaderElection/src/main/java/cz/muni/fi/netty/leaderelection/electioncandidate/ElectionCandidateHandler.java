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
package cz.muni.fi.netty.leaderelection.electioncandidate;

import cz.muni.fi.netty.leaderelection.coordinator.CoordinatorHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles a client-side channel.
 */
@Sharable
public class ElectionCandidateHandler extends SimpleChannelInboundHandler<String> {
    
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws IOException, InterruptedException {
        
        //if I have become the leader, perform the leader procedure, otherwise
        //print the message containg info about the new leader
        if (msg.equals("Your are the leader now.")) {
            leaderProcedure();
            ctx.close();
        } else {
            System.out.println(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Logger.getLogger(ElectionCandidateHandler.class.getName()).log(Level.SEVERE, null, cause);
        ctx.close();
    }
    
    /**
     * Print to console that I have become the leader, wait for 5 seconds and terminate.
     * @throws InterruptedException 
     */
    private void leaderProcedure() throws InterruptedException {
        System.out.println("I am the leader now.");
        System.out.println("Going to wait for 5 seconds...");
        Thread.sleep(5000);
        System.out.println("Done.");
        System.out.println("Closing.");
    }
}