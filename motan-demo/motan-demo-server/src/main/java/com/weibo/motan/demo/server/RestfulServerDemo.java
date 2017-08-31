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

package com.weibo.motan.demo.server;

import com.weibo.motan.demo.service.RestfulService;
import com.weibo.motan.demo.service.model.User;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

/**
 * Created by zhanglei28 on 2017/8/30.
 */
public class RestfulServerDemo implements RestfulService {
    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:motan_demo_server_restful.xml");
        System.out.println("restful server start...");
        Thread.sleep(Long.MAX_VALUE);
    }

    @Override
    public List<User> getUsers(@CookieParam("uid") int uid) {
        return Arrays.asList(new User(uid, "name" + uid));
    }

    @Override
    public String testPrimitiveType() {
        return "helloworld!";
    }

    @Override
    public Response add(@FormParam("id") int id, @FormParam("name") String name) {
        return Response.ok().cookie(new NewCookie("ck", String.valueOf(id))).entity(new User(id, name)).build();
    }

    @Override
    public void testException() {
        throw new UnsupportedOperationException("unsupport");
    }
}
