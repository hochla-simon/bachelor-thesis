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
package cz.muni.fi.netty.lock.coordinator;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import static cz.muni.fi.netty.lock.main.Main.SITES_COUNT;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles a server-side channel.
 */
@Sharable
public class CoordinatorHandler extends SimpleChannelInboundHandler<String> {

    /**
     * Group of participants communicating with the coordinator
     */
    private static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        
    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        channels.add(ctx.channel());
        if (channels.size() == SITES_COUNT) {
            electLeader();
        }
    }

    /**
     * Elect a new member to acquire the lock and repeat the
     * process when the member terminates until the pool is not empty.
     */
    private void electLeader() {
        final Channel channelWithMinId = determineLeader();
        channelWithMinId.writeAndFlush("canLock\r\n");
        informParticipantsAboutNewLeader(channelWithMinId);
    }
    
    /**
     * Determine which member will acquire the lock based on the respective
     * ID. Return the channel with the minimal ID.
     *
     * @return channel with the minimal ID
     */
    private Channel determineLeader() {
        Map<String, Channel> channelIds = new TreeMap<>();
        for (Channel c : channels) {
            channelIds.put(c.id().asLongText(), c);
        }
        String minId = Collections.min(channelIds.keySet());
        Channel channelWithMinId = channelIds.get(minId);
        return channelWithMinId;
    }
    
    /**
     * Inform members which member has acquired the lock.
     *
     * @param channelWithMinId reference to the new leader
     */
    private void informParticipantsAboutNewLeader(Channel channelWithMinId) {
        for (Channel c : channels) {
            if (c != channelWithMinId) {
                c.writeAndFlush("The lock acquired the member with ID: "
                        + channelWithMinId.id().asLongText() + "\r\n");
            }
        }
    }
    
    /**
     * Remove the leader from the channels after it terminated and elect a new
     * leader from the pool if it is not empty.
     */
    private class LeaderClosedListener implements ChannelFutureListener {

        private final Channel leader;

        public LeaderClosedListener(Channel leader) {
            this.leader = leader;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            channels.remove(leader);
            if (!channels.isEmpty()) {
                electLeader();
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Logger.getLogger(CoordinatorHandler.class.getName()).log(Level.SEVERE, null, cause);
        ctx.close();
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext chc, String msg) throws Exception {
        if (msg.equals("lockReleased")) {
           channels.remove(chc.channel());
           electLeader();
        }
    }
}