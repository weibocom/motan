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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RefererConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.config.ServiceConfig;
import com.weibo.api.motan.serialize.protobuf.gen.UserProto.Address;
import com.weibo.api.motan.serialize.protobuf.gen.UserProto.User;

public class TestProtoBuf {
	private ServiceConfig<HelloService> serviceConfig;
	private RefererConfig<HelloService> refererConfig;
	private HelloService service;

	@Before
	public void setUp() {
		ProtocolConfig protocolConfig = new ProtocolConfig();
		protocolConfig.setId("testMotan");
		protocolConfig.setName("motan");
		protocolConfig.setSerialization("protobuf");
		protocolConfig.setCodec("motan");

		RegistryConfig registryConfig = new RegistryConfig();
		registryConfig.setAddress("127.0.0.1");
		registryConfig.setPort(8002);

		serviceConfig = new ServiceConfig<HelloService>();
		serviceConfig.setRef(new HelloServiceImpl());
		serviceConfig.setInterface(HelloService.class);
		serviceConfig.setProtocol(protocolConfig);
		serviceConfig.setExport("testMotan:8002");
		serviceConfig.setRegistry(registryConfig);
		serviceConfig.setShareChannel(true);

		serviceConfig.export();

		refererConfig = new RefererConfig<HelloService>();
		refererConfig.setDirectUrl("127.0.0.1:8002");
		refererConfig.setProtocol(protocolConfig);
		refererConfig.setInterface(HelloService.class);

		service = refererConfig.getRef();
	}

	@Test
	public void testPrimitiveType() {
		Assert.assertEquals("-1", service.sumAsString(Integer.MAX_VALUE, Integer.MIN_VALUE));
		Assert.assertEquals("-1", service.sumAsString(-2, 1));

		Assert.assertEquals((Long) 100L, service.boxIfNotZero(100));
		Assert.assertNull(service.boxIfNotZero(0));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testException() {
		service.testException();
	}

	@Test
	public void testNull() {
		Assert.assertTrue(service.isNull(null));

		User user = User.newBuilder().setId(120).setName("zhou").build();

		Assert.assertFalse(service.isNull(user));
	}

	@Test
	public void testProtobuf() {
		Address address = service.queryByUid(1);
		Assert.assertEquals(1, address.getId());

		User user = User.newBuilder().setId(120).setName("zhou").setGender(false).addAddress(address).build();

		Assert.assertTrue(service.isUserAddress(user, address));

		User newOne = service.copy(user);
		Assert.assertEquals(user.getId(), newOne.getId());
		Assert.assertEquals(user.getName(), newOne.getName());
		Assert.assertEquals(user.getGender(), newOne.getGender());
		Assert.assertEquals(user.getAddress(0).getId(), newOne.getAddress(0).getId());
	}

	@After
	public void tearDown() {
		refererConfig.destroy();
		serviceConfig.unexport();
	}

}
