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
package cz.muni.fi.netty.leaderelection.coordinator;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import static cz.muni.fi.netty.leaderelection.coordinator.CoordinatorDemo.SITES_COUNT;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Handles a server-side channel.
 */
@Sharable
public class CoordinatorHandler extends SimpleChannelInboundHandler<String> {

    private static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        
    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        channels.add(ctx.channel());
        if (channels.size() == SITES_COUNT) {
            Map<String, Channel> channelIds = new TreeMap<>();
            for (Channel c : channels) {
                channelIds.put(c.id().asLongText(), c);
                c.isActive();
            }
            String minId = Collections.min(channelIds.keySet());
            Channel channelWithMinId = channelIds.get(minId);
            channelWithMinId.writeAndFlush("Your are the leader now.\r\n");
            channelWithMinId.writeAndFlush(channels);
            for (Channel c : channels) {
                if (c != channelWithMinId) {
                    c.writeAndFlush("Current leader is leader with ID: " + minId + "\r\n");
                }
            }
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String request) {
        if ("finished".equals(request)) {
            channels.remove(ctx.channel());
            
            if (!channels.isEmpty()) {
                Map<String, Channel> channelIds = new TreeMap<>();
                for (Channel c : channels) {
                    channelIds.put(c.id().asLongText(), c);
                    c.isActive();
                }
                String minId = Collections.min(channelIds.keySet());
                Channel channelWithMinId = channelIds.get(minId);
                channelWithMinId.writeAndFlush("Your are the leader now.\r\n");
                for (Channel c : channels) {
                    if (c != channelWithMinId) {
                        c.writeAndFlush("Current leader is leader with ID: " + minId + "\r\n");
                    }
                }
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