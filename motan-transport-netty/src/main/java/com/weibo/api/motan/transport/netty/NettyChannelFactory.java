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

import org.apache.commons.pool.BasePoolableObjectFactory;

import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.LoggerUtil;

/**
 * @author maijunsheng
 * @version 创建时间：2013-5-31
 * 
 */
public class NettyChannelFactory extends BasePoolableObjectFactory {
	private String factoryName = "";
	private NettyClient nettyClient;

	public NettyChannelFactory(NettyClient nettyClient) {
		super();

		this.nettyClient = nettyClient;
		this.factoryName = "NettyChannelFactory_" + nettyClient.getUrl().getHost() + "_"
				+ nettyClient.getUrl().getPort();
	}

	public String getFactoryName() {
		return factoryName;
	}

	@Override
	public String toString() {
		return factoryName;
	}

	@Override
	public Object makeObject() throws Exception {
		NettyChannel nettyChannel = new NettyChannel(nettyClient);
		nettyChannel.open();

		return nettyChannel;
	}

	@Override
	public void destroyObject(final Object obj) throws Exception {
		if (obj instanceof NettyChannel) {
			NettyChannel client = (NettyChannel) obj;
			URL url = nettyClient.getUrl();

			try {
				client.close();

				LoggerUtil.info(factoryName + " client disconnect Success: " + url.getUri());
			} catch (Exception e) {
				LoggerUtil.error(factoryName + " client disconnect Error: " + url.getUri(), e);
			}
		}
	}

	@Override
	public boolean validateObject(final Object obj) {
		if (obj instanceof NettyChannel) {
			final NettyChannel client = (NettyChannel) obj;
			try {
				return client.isAvailable();
			} catch (final Exception e) {
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public void activateObject(Object obj) throws Exception {
		if (obj instanceof NettyChannel) {
			final NettyChannel client = (NettyChannel) obj;
			if (!client.isAvailable()) {
				client.open();
			}
		}
	}

	@Override
	public void passivateObject(Object obj) throws Exception {
	}
}
