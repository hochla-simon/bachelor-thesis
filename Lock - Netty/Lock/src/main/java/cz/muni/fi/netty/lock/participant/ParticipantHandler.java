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
package cz.muni.fi.netty.lock.participant;

import static cz.muni.fi.netty.lock.participant.LockFileDemo.lockFile;
import static cz.muni.fi.netty.lock.participant.LockFileDemo.releaseLock;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.IOException;
import java.nio.channels.FileLock;

/**
 * Handles a client-side channel.
 */
@Sharable
public class ParticipantHandler extends SimpleChannelInboundHandler<String> {
    
    private FileLock lock = null;
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws IOException, InterruptedException {
        
        if (msg.equals("canLock")) {
            leaderProcedure();
            ChannelFuture future = ctx.writeAndFlush("finished\r\n");
            future.addListener(ChannelFutureListener.CLOSE);

        } else {
            System.out.println(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
    
    private void leaderProcedure() throws InterruptedException {
        System.out.println("I have acquired the lock.");
        lockFile();
        System.out.println("Going to wait for 3 seconds...");
        Thread.sleep(3000);
        System.out.println("Done.");
        releaseLock();
        System.out.println("Releasing the lock.");
    }
}