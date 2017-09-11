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
package com.weibo.api.motan.serialize.protobuf;

import com.weibo.api.motan.serialize.protobuf.gen.UserProto.Address;
import com.weibo.api.motan.serialize.protobuf.gen.UserProto.User;

public class HelloServiceImpl implements HelloService {

	@Override
	public String sumAsString(int a, int b) {
		return String.valueOf(a + b);
	}

	@Override
	public Long boxIfNotZero(int value) {
		return value == 0 ? null : (long) value;
	}

	@Override
	public Address queryByUid(int uid) {
		return Address.newBuilder().setId(uid).setProvince("北京").setCity("北京").setStreet("无").setPhone("1233444")
				.build();
	}

	@Override
	public boolean isUserAddress(User user, Address address) {
		for (Address item : user.getAddressList()) {
			if (item.getId() == address.getId()) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isNull(User user) {
		return user == null;
	}

	@Override
	public void testException() {
		throw new UnsupportedOperationException();
	}

	@Override
	public User copy(User origin) {
		return origin;
	}

}
