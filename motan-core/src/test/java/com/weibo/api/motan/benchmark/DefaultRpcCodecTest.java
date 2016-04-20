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

package com.weibo.api.motan.benchmark;

import com.weibo.api.motan.exception.MotanBizException;
import com.weibo.api.motan.mock.MockChannel;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.protocol.rpc.DefaultRpcCodec;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Channel;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-21
 * 
 */
public class DefaultRpcCodecTest {
    private static final int loop = 100000;

    public static void main(String[] args) throws Exception {
        DefaultRpcCodec codec = new DefaultRpcCodec();

        MockChannel channel = new MockChannel(new URL("motan", "localhost", 18080, IHello.class.getName()));

        System.out.println("requestSize: " + requestSize(codec, channel, null).length);
        System.out.println("responseSize: " + responseSize(codec, channel, null).length);
        System.out.println("responseSize: " + exceptionResponseSize(codec, channel).length);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            builder.append("1");
        }

        String value = builder.toString();
        String[] arr = new String[] {value, value, value};

        Long[] sets = new Long[20];
        for (int i = 0; i < 20; i++) {
            sets[i] = 1000000000L + i;
        }

        System.out.println("requestSize 1k: " + requestSize(codec, channel, arr).length);
        System.out.println("responseSize 1k: " + responseSize(codec, channel, arr).length);

        byte[] data = null;
        for (int i = 0; i < loop; i++) {
            data = requestSize(codec, channel, sets);
            codec.decode(channel, "", data);
        }


        long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            data = requestSize(codec, channel, sets);
        }
        System.out.println("request encode performance: " + (System.nanoTime() - start) / loop + " ns");

        start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            codec.decode(channel, "", data);
        }

        System.out.println("request decode performance: " + (System.nanoTime() - start) / loop + " ns");

    }

    /**
     * 不带参数的Request大小
     */
    private static byte[] requestSize(DefaultRpcCodec codec, Channel channel, Object data) throws Exception {
        DefaultRequest request = new DefaultRequest();
        request.setInterfaceName(IHello.class.getName());
        request.setMethodName("hello");

        if (data != null) {
            request.setParamtersDesc("java.util.HashSet");
            request.setArguments(new Object[] {data});
        } else {
            request.setParamtersDesc("void");
        }
        request.setRequestId(System.currentTimeMillis());
        request.setAttachment("application", "helloApplication");
        request.setAttachment("module", "helloModule");
        request.setAttachment("version", "1.0");
        request.setAttachment("graph", "yf-rpc");

        byte[] bytes = codec.encode(channel, request);

        return bytes;
    }

    /**
     * 不带返回值的response大小
     */
    private static byte[] responseSize(DefaultRpcCodec codec, Channel channel, Object data) throws Exception {
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(System.currentTimeMillis());
        response.setProcessTime(System.currentTimeMillis());

        if (data != null) {
            response.setValue(data);
        }

        byte[] bytes = codec.encode(channel, response);

        return bytes;
    }

    /**
     * 不带参数的Request大小
     */
    private static byte[] exceptionResponseSize(DefaultRpcCodec codec, Channel channel) throws Exception {
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(System.currentTimeMillis());
        response.setProcessTime(System.currentTimeMillis());
        response.setException(new MotanBizException(new RuntimeException("hi, boy, i am biz exception.")));

        byte[] bytes = codec.encode(channel, response);

        return bytes;
    }
}
