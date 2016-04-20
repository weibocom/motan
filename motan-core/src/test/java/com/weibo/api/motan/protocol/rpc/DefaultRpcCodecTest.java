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

package com.weibo.api.motan.protocol.rpc;

import java.util.Arrays;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.mock.MockChannel;
import com.weibo.api.motan.protocol.example.Model;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Channel;

/**
 * @author maijunsheng
 * @version 创建时间：2013-5-26
 * 
 */
public class DefaultRpcCodecTest extends TestCase {
    protected Codec rpcCodec = new DefaultRpcCodec();
    protected URL url = new URL("motan", "localhost", 18080, "com.weibo.api.motan.protocol.example.IHello");
    protected Channel channel = new MockChannel(url);
    protected String basicInterface = "com.weibo.api.motan.protocol.example.IHello";
    protected String basicMethod = "hello";


    @Test
    public void testVoidTypeRequest() throws Exception {
        DefaultRequest request = getRequest("void", null);
        testCodecRequest(request);
    }

    @Test
    public void testOriginalTypeRequest() throws Exception {
        DefaultRequest request = getRequest("java.lang.Integer", new Object[] {1});
        testCodecRequest(request);
    }

    @Test
    public void testStringTypeRequest() throws Exception {
        DefaultRequest request = getRequest("java.lang.String", new Object[] {"hello"});
        testCodecRequest(request);
    }

    @Test
    public void testObjectTypeRequest() throws Exception {
        DefaultRequest request =
                getRequest("com.weibo.api.motan.protocol.example.Model", new Object[] {new Model("world", 12, Model.class)});
        testCodecRequest(request);
    }

    @Test
    public void testNullRequest() throws Exception {
        DefaultRequest request = getRequest("com.weibo.api.motan.protocol.example.Model", new Object[] {null});
        testCodecRequest(request);
    }

    @Test
    public void testHalfNullRequest() throws Exception {
        DefaultRequest request =
                getRequest("com.weibo.api.motan.protocol.example.Model,com.weibo.api.motan.protocol.example.Model", new Object[] {null,
                        new Model("world", 12, Model.class)});
        testCodecRequest(request);
    }

    @Test
    public void testHalfNullRequest1() throws Exception {
        DefaultRequest request =
                getRequest("com.weibo.api.motan.protocol.example.Model[]", new Object[] {new Model[] {null,
                        new Model("world", 12, Model.class)}});
        testCodecRequest(request);
    }


    @Test
    public void testMultiTypeRequest() throws Exception {
        DefaultRequest request =
                getRequest("com.weibo.api.motan.protocol.example.Model,java.lang.Integer,java.lang.String", new Object[] {
                        new Model("world", 12, Model.class), 1, "hello"});
        testCodecRequest(request);
    }

    public @Test void testOriginalTypeArrayRequest() throws Exception {
        DefaultRequest request = getRequest("int[]", new Object[] {new int[] {1, 2}});
        testCodecRequest(request);
    }

    @Test
    public void testStringArrayRequest() throws Exception {
        DefaultRequest request = getRequest("java.lang.String[]", new Object[] {new String[] {"hello", "world"}});
        testCodecRequest(request);
    }

    @Test
    public void testObjectArrayRequest() throws Exception {
        DefaultRequest request =
                getRequest("com.weibo.api.motan.protocol.example.Model[]", new Object[] {new Model[] {new Model("hello", 11, Model.class),
                        new Model("world", 12, Model.class)}});
        testCodecRequest(request);
    }

    @Test
    public void testCodecRequest(Request request) throws Exception {
        byte[] bytes = rpcCodec.encode(channel, request);

        Request result = (Request) rpcCodec.decode(channel, "", bytes);

        Assert.assertTrue(equals(request, result));
    }

    @Test
    public void testOriginalTypeResponse() throws Exception {
        DefaultResponse response = new DefaultResponse();

        response.setValue(1);

        testCodecResponse(response);
    }

    @Test
    public void testStringResponse() throws Exception {
        DefaultResponse response = new DefaultResponse();

        response.setValue("hello");

        testCodecResponse(response);
    }

    @Test
    public void testObjectResponse() throws Exception {
        DefaultResponse response = new DefaultResponse();

        response.setValue(new Model("world", 12, Model.class));

        testCodecResponse(response);
    }

    @Test
    public void testException() throws Exception {
        DefaultResponse response = new DefaultResponse();
        response.setException(new MotanServiceException("process thread pool is full, reject", MotanErrorMsgConstant.SERVICE_REJECT));

        byte[] bytes = rpcCodec.encode(channel, response);

        Response result = (Response) rpcCodec.decode(channel, "", bytes);

        Assert.assertTrue(result.getException().getMessage().equals(response.getException().getMessage()));
        Assert.assertTrue(result.getException().getClass().equals(response.getException().getClass()));
    }

    @Test
    public void testCodecResponse(Response respose) throws Exception {
        byte[] bytes = rpcCodec.encode(channel, respose);

        Response result = (Response) rpcCodec.decode(channel, "", bytes);

        Assert.assertTrue(result.getValue().toString().equals(respose.getValue().toString()));
    }

    // 获取基础request，不包括请求方法和参数描述，使用默认接口类和分组
    protected DefaultRequest getRequest(String paramtersDesc, Object[] params) {
        DefaultRequest request = new DefaultRequest();
        request.setInterfaceName(basicInterface);
        request.setMethodName(basicMethod);
        request.setParamtersDesc(paramtersDesc);
        if (params != null) {
            request.setArguments(params);
        }
        return request;
    }

    protected boolean equals(Request src, Request target) {
        return toString(src).equals(toString(target));
    }

    private String toString(Request request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getInterfaceName()).append("|").append(request.getMethodName()).append("|")
                .append(request.getParamtersDesc());

        if (request.getArguments() != null) {
            for (Object obj : request.getArguments()) {

                builder.append("|").append(toString(obj));
            }
        }

        return builder.toString();
    }

    private String toString(Object obj) {
        if (obj == null) {
            return null;
        }

        if (!obj.getClass().isArray()) {
            return obj.toString();
        }

        Class<?> clz = obj.getClass().getComponentType();

        int dimension = 1;
        while (clz.isArray()) {
            clz = clz.getComponentType();
            dimension++;
        }

        if (dimension == 1) {
            if (clz == int.class) {
                return Arrays.toString((int[]) obj);
            } else if (clz == short.class) {
                return Arrays.toString((short[]) obj);
            } else if (clz == long.class) {
                return Arrays.toString((long[]) obj);
            } else if (clz == byte.class) {
                return Arrays.toString((byte[]) obj);
            } else if (clz == char.class) {
                return Arrays.toString((char[]) obj);
            } else if (clz == float.class) {
                return Arrays.toString((float[]) obj);
            } else if (clz == double.class) {
                return Arrays.toString((double[]) obj);
            } else if (clz == boolean.class) {
                return Arrays.toString((boolean[]) obj);
            } else {
                return Arrays.toString((Object[]) obj);
            }
        }

        return obj.getClass().getName();
    }
}
