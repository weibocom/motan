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

import com.weibo.api.motan.transport.async.MotanAsync;
import com.weibo.motan.demo.service.model.User;

import java.util.List;

@MotanAsync
public interface MotanDemoService {
    String hello(String name);

    User rename(User user, String name) throws Exception;

    User batchSave(List<User> userList);

    List<User> getUsers(List<Integer> ids);

}
