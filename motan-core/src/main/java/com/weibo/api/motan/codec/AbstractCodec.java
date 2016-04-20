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

package com.weibo.api.motan.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;

/**
 * @author maijunsheng
 * @version 创建时间：2013-5-24
 * 
 */
public abstract class AbstractCodec implements Codec {
    protected void serialize(ObjectOutput output, Object message, Serialization serialize) throws IOException {
        if (message == null) {
            output.writeObject(null);
            return;
        }

        output.writeObject(serialize.serialize(message));
    }

    protected Object deserialize(byte[] value, Class<?> type, Serialization serialize) throws IOException {
        if (value == null) {
            return null;
        }

        return serialize.deserialize(value, type);
    }

    public ObjectOutput createOutput(OutputStream outputStream) {
        try {
            return new ObjectOutputStream(outputStream);
        } catch (Exception e) {
            throw new MotanFrameworkException(this.getClass().getSimpleName() + " createOutput error", e,
                    MotanErrorMsgConstant.FRAMEWORK_ENCODE_ERROR);
        }
    }

    public ObjectInput createInput(InputStream in) {
        try {
            return new ObjectInputStream(in);
        } catch (Exception e) {
            throw new MotanFrameworkException(this.getClass().getSimpleName() + " createInput error", e,
                    MotanErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
        }
    }

}
