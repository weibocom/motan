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
package com.weibo.api.motan.protocol.restful;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

public class RestHelloResource implements HelloResource {

    public List<User> hello(int id) {
        return Arrays.asList(new User(id, "de"));
    }

    @Override
    public String testPrimitiveType() {
        return "helloworld";
    }

    @Override
    public Response add(int id, String name) {
        return Response.ok().cookie(new NewCookie("ck", String.valueOf(id))).entity(new User(id, name)).build();
    }

    @Override
    public void testException() {
        throw new UnsupportedOperationException("unsupport");
    }

}
