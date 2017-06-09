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

package com.weibo.api.motan.serialize;

import com.google.protobuf.MessageLite;
import com.weibo.api.motan.codec.Serialization;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Created by zhanglei28 on 2017/5/15.
 * grpc pb可以直接解析grpc的body，因此功能上弱于ProtobufSerialization， 只能支持单个参数的pb对象，不支持null值与基本数据格式如int、boolean
 */
@SpiMeta(name = "grpc-pb")
public class GrpcPbSerialization implements Serialization {
    @Override
    public byte[] serialize(Object obj) throws IOException {
        if (obj == null) {
            throw new IllegalArgumentException("can't serialize null.");
        }
        if (MessageLite.class.isAssignableFrom(obj.getClass())) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(((MessageLite) obj).toByteArray());
            baos.flush();
            return baos.toByteArray();
        } else {
            throw new IllegalArgumentException("can't serialize " + obj.getClass());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException {
        if (MessageLite.class.isAssignableFrom(clazz)) {
            try {
                ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                Method method = clazz.getDeclaredMethod("parseFrom", java.io.InputStream.class);
                return (T) method.invoke(null, input);
            } catch (Exception e) {
                throw new MotanFrameworkException(e);
            }
        } else {
            throw new IllegalArgumentException("can't serialize " + clazz);
        }
    }


    @Override
    public byte[] serializeMulti(Object[] data) throws IOException {
        if (data.length == 1) {
            return serialize(data[0]);
        }
        throw new MotanServiceException("GrpcPbSerialization not support serialize multi Object");
    }

    @Override
    public Object[] deserializeMulti(byte[] data, Class<?>[] classes) throws IOException {
        if (classes.length != 1) {
            throw new MotanServiceException("only single value serialize was supported in GrpcPbSerialization");
        }
        Object[] objects = new Object[1];
        objects[0] = deserialize(data, classes[0]);
        return objects;
    }

    @Override
    public int getSerializationNumber() {
        return 1;
    }
}
