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

package com.weibo.api.motan.common;

/**
 * channel 节点的状态
 * 
 * @author maijunsheng
 * @version 创建时间：2013-6-3
 * 
 */
public enum ChannelState {
	/** 未初始化状态 **/
	UNINIT(0),
	/** 初始化完成 **/
	INIT(1),
	/** 存活可用状态 **/
	ALIVE(2),
	/** 不存活可用状态 **/
	UNALIVE(3),
	/** 关闭状态 **/
	CLOSE(4);

	public final int value;

	private ChannelState(int value) {
		this.value = value;
	}

	public boolean isAliveState() {
		return this == ALIVE;
	}
	
	public boolean isUnAliveState() {
		return this == UNALIVE;
	}

	public boolean isCloseState() {
		return this == CLOSE;
	}

	public boolean isInitState() {
		return this == INIT;
	}
	
	public boolean isUnInitState() {
		return this == UNINIT;
	}
}
