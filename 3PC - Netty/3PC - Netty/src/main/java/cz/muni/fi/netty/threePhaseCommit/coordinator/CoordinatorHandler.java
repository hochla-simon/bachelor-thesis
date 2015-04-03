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
package cz.muni.fi.netty.threePhaseCommit.coordinator;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import static cz.muni.fi.netty.threePhaseCommit.coordinator.CoordinatorDemo.SITES_COUNT;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * Handles a server-side channel.
 */
@Sharable
public class CoordinatorHandler extends SimpleChannelInboundHandler<String> {
    
    private static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    
    private int canCommitCounter = 0;
    private int acknowledgedCounter = 0;
    private int haveCommited = 0;
    private boolean done = false;
    
    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        channels.add(ctx.channel());
        if (channels.size() == SITES_COUNT) {
            for (Channel c : channels) {
                c.writeAndFlush("canCommit?\r\n");
                
            }
        }
        ctx.flush();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String request) {
        switch(request) {
            case "Yes": {
                canCommitCounter++;
                if (canCommitCounter == SITES_COUNT) {
                    for (Channel c : channels) {
                        c.writeAndFlush("preCommit\r\n");
                    }
                }
                break;
            }
            case "No": {
                for (Channel c : channels) {
                    c.writeAndFlush("abort\r\n")
                            .addListener(ChannelFutureListener.CLOSE);
                }
                break;
            }
            case "ACK": {
                acknowledgedCounter++;
                if (acknowledgedCounter == SITES_COUNT) {
                    for (Channel c : channels) {
                        c.writeAndFlush("doCommit\r\n");
                    }
                }
                break;
            }
            case "haveCommited": {
                haveCommited++;
                if (haveCommited == SITES_COUNT) {
                    done = true;
                }
                ctx.close();
                break;
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}