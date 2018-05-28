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

package com.weibo.motan.demo.service;

import com.weibo.motan.demo.service.model.User;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Created by zhanglei28 on 2017/8/30.
 */
@Path("/rest")
public interface RestfulService {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<User> getUsers(@QueryParam("uid") int uid);

    @GET
    @Path("/primitive")
    @Produces(MediaType.TEXT_PLAIN)
    String testPrimitiveType();

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    Response add(@FormParam("id") int id, @FormParam("name") String name);

    @GET
    @Path("/exception")
    @Produces(MediaType.APPLICATION_JSON)
    void testException();
}
