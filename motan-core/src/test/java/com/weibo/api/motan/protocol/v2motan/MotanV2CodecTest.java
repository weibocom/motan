/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.protocol.v2motan;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.mock.MockChannel;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.serialize.DeserializableObject;
import com.weibo.api.motan.transport.Channel;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by zhanglei28 on 2017/5/10.
 */
public class MotanV2CodecTest {
    static MotanV2Codec codec = new MotanV2Codec();

    @Test
    public void testEncode() throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        URL url = new URL("motan2", "localhost", 0, "testService", params);
        url.getParameters().put(URLParamType.serialize.getName(), URLParamType.serialize.getValue());
        Channel channel = new MockChannel(url);

        // encode request
        DefaultRequest request = new DefaultRequest();
        request.setInterfaceName("com.weibo.test.TestService");
        request.setRequestId(new Random().nextLong());
        request.setMethodName("testHello");
        request.setArguments(new Object[]{"hi!"});
        Map<String, String> attachs = new HashMap<String, String>();
        attachs.put("group", "testgroup");
        request.setAttachments(attachs);
        byte[] bytes = codec.encode(channel, request);

        Request newReq = (Request) codec.decode(channel, "localhost", bytes);
        checkRequest(request, newReq);

        request.setAttachments(null);
        bytes = codec.encode(channel, request);
        newReq = (Request) codec.decode(channel, "localhost", bytes);
        checkRequest(request, newReq);

        request.setArguments(new Object[]{"123", 456, true });
        bytes = codec.encode(channel, request);
        newReq = (Request) codec.decode(channel, "localhost", bytes);
        checkRequest(request, newReq);

        //encode response
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(8908790);
        response.setProcessTime(5);
        response.setValue("xxede");
        Map<String, String> resAttachs = new HashMap<String, String>();
        resAttachs.put("res", "testres");
        resAttachs.put("xxx","eee");
        response.setAttachments(resAttachs);

        bytes = codec.encode(channel, response);
        Response newRes = (Response) codec.decode(channel, "localhost", bytes);
        checkResponse(response, newRes);

    }

    private void checkRequest(Request expect, Request real) throws IOException {
        assertEquals(expect.toString(), real.toString());
        assertTrue(real.getArguments()[0] instanceof DeserializableObject);

        Class[] classes = new Class[expect.getArguments().length];
        for (int i = 0; i < expect.getArguments().length; i++) {
            classes[i] = expect.getArguments()[i].getClass();
        }
        Object[] result = ((DeserializableObject) real.getArguments()[0]).deserializeMulti(classes);
        for(int i = 0; i < expect.getArguments().length; i++){
            assertEquals(expect.getArguments()[i], result[i]);
        }
        checkMap(expect.getAttachments(), real.getAttachments());

    }

    private void checkResponse(Response expect, Response real)throws  Exception{
        assertTrue(real.getValue() instanceof DeserializableObject);
        assertEquals(expect.getValue(), ((DeserializableObject)real.getValue()).deserialize(String.class));
        checkMap(expect.getAttachments(), real.getAttachments());
    }

    private void checkMap(Map<String, String> map1, Map<String, String> map2){
        if(map1 == null || map2 == null){
            if(map1 == null && map2 == null){
                return;
            }
            throw new RuntimeException("map not both null! ");
        }
        if(map1.size() != map2.size()){
            throw new RuntimeException("map size not equals!");
        }
        for(Map.Entry<String, String> entry : map1.entrySet()){
            assertEquals(entry.getValue(), map2.get(entry.getKey()));
        }
    }


}