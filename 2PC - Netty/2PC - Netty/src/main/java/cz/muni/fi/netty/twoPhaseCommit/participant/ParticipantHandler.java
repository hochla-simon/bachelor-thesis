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
package cz.muni.fi.netty.twoPhaseCommit.participant;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileLock;

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
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                String line;
                    if ("commit".equals(LockFileDemo.decideTransaction())) {
                        line = "commit";
                        lock = LockFileDemo.lockFile();
                    } else {
                        line = "abort";
                    }
                ctx.writeAndFlush(line + "\r\n");
                break;
            }
            case "commited": {
                LockFileDemo.releaseLock(lock);
                ctx.close();
                return;
            }
            case "aborted": {
                if (lock != null) {
                    LockFileDemo.releaseLock(lock);
                }
                ctx.close();
                return;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}