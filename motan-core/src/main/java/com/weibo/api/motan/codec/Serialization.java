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

package com.weibo.api.motan.codec;

import com.weibo.api.motan.core.extension.Scope;
import com.weibo.api.motan.core.extension.Spi;

import java.io.IOException;

/** 
 * @author maijunsheng
 * @version 创建时间：2013-5-22
 * 
 */
@Spi(scope=Scope.PROTOTYPE)
public interface Serialization {

	byte[] serialize(Object obj) throws IOException;

	<T> T deserialize(byte[] bytes, Class<T> clz) throws IOException;

	byte[] serializeMulti(Object[] data) throws IOException;

	Object[] deserializeMulti(byte[] data, Class<?>[] classes) throws IOException;

	/**
	 * serializaion的唯一编号，用于传输协议中指定序列化方式。每种序列化的编号必须唯一。
	 * @return 由于编码规范限制，序列化方式最大支持32种，因此返回值必须在0-31之间。
	 */
	int getSerializationNumber();
}
