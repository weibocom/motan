/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.weibo.api.motan.transport.netty;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.weibo.api.motan.util.LoggerUtil;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-7
 * 
 */
public class NettyServerChannelManage extends SimpleChannelHandler {
	private ConcurrentMap<String, Channel> channels = new ConcurrentHashMap<String, Channel>();

	private int maxChannel = 0;

	public NettyServerChannelManage(int maxChannel) {
		super();
		this.maxChannel = maxChannel;
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		Channel channel = ctx.getChannel();

		String channelKey = getChannelKey((InetSocketAddress) channel.getLocalAddress(),
				(InetSocketAddress) channel.getRemoteAddress());

		if (channels.size() > maxChannel) {
			// 超过最大连接数限制，直接close连接
			LoggerUtil.warn("NettyServerChannelManage channelConnected channel size out of limit: limit={} current={}",
					maxChannel, channels.size());

			channel.close();
		} else {
			channels.put(channelKey, channel);
			ctx.sendUpstream(e);
		}
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		Channel channel = ctx.getChannel();

		String channelKey = getChannelKey((InetSocketAddress) channel.getLocalAddress(),
				(InetSocketAddress) channel.getRemoteAddress());

		channels.remove(channelKey);
		ctx.sendUpstream(e);
	}

	public Map<String, Channel> getChannels() {
		return channels;
	}

	/**
	 * close所有的连接
	 */
	public void close() {
		for (Map.Entry<String, Channel> entry : channels.entrySet()) {
			try {
				Channel channel = entry.getValue();

				if (channel != null) {
					channel.close();
				}
			} catch (Exception e) {
				LoggerUtil.error("NettyServerChannelManage close channel Error: " + entry.getKey(), e);
			}
		}
	}

	/**
	 * remote address + local address 作为连接的唯一标示
	 * 
	 * @param local
	 * @param remote
	 * @return
	 */
	private String getChannelKey(InetSocketAddress local, InetSocketAddress remote) {
		String key = "";
		if (local == null || local.getAddress() == null) {
			key += "null-";
		} else {
			key += local.getAddress().getHostAddress() + ":" + local.getPort() + "-";
		}

		if (remote == null || remote.getAddress() == null) {
			key += "null";
		} else {
			key += remote.getAddress().getHostAddress() + ":" + remote.getPort();
		}

		return key;
	}
}