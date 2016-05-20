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

package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.transport.netty4.server.Netty4Server;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-7
 * 
 */
public class Netty4ServerExample {
    public static void main(String[] args) throws InterruptedException {
        URL url = new URL("netty", "localhost", 18080, "com.weibo.api.motan.procotol.example.IHello");

        Netty4Server nettyServer = new Netty4Server(url, new MessageHandler() {
            @Override
            public Object handle(Channel channel, Object message) {
                Request request = (Request) message;

                System.out.println("[server] get request: requestId: " + request.getRequestId() + " method: " + request.getMethodName());

                DefaultResponse response = new DefaultResponse();
                response.setRequestId(request.getRequestId());
                response.setValue("method: " + request.getMethodName() + " time: " + System.currentTimeMillis());

                return response;
            }
        });

        nettyServer.open();
        System.out.println("~~~~~~~~~~~~~ Server open ~~~~~~~~~~~~~");

        Thread.sleep(100000);
    }
}
