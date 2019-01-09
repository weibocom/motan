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

package com.weibo.api.motan.serialize;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by zhanglei28 on 2017/9/7.
 */
//TODO serialization base test case
public class FastJsonSerializationTest {
    FastJsonSerialization serialization = new FastJsonSerialization();

    @Test
    public void serialize() throws Exception {
        UserAttentions userAttentions = new UserAttentions();
        userAttentions.setUid(12312);
        byte[] bytes = serialization.serialize(userAttentions);
        UserAttentions u2 = serialization.deserialize(bytes, UserAttentions.class);
        Assert.assertEquals(u2.getUid(), userAttentions.getUid());
    }

    @Test
    public void serializeMulti() throws Exception {

        UserAttentions userAttentions = new UserAttentions();
        userAttentions.setUid(12312);
        Object[] params = new Object[]{userAttentions, 123, "xxx", false};
        byte[] bytes = serialization.serializeMulti(params);
        Class[] classes = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            classes[i] = params[i].getClass();
        }
        Object[] result = serialization.deserializeMulti(bytes, classes);
        Assert.assertEquals(params.length, result.length);
        Assert.assertEquals(((UserAttentions) params[0]).getUid(), ((UserAttentions) result[0]).getUid());
        for (int i = 1; i < params.length; i++) {
            Assert.assertEquals(params[i], result[i]);
        }
    }

}