package com.weibo.api.motan.registry.zookeeper;

import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.SerializableSerializer;

import java.io.ObjectStreamConstants;

public class StringSerializer extends SerializableSerializer {
    @Override
    public Object deserialize(byte[] bytes) throws ZkMarshallingError {
        if (getTag(bytes) == ObjectStreamConstants.STREAM_MAGIC) {
            return super.deserialize(bytes);
        }
        return new String(bytes);
    }

    @Override
    public byte[] serialize(Object obj) throws ZkMarshallingError {
        return obj.toString().getBytes();
    }

    private short getTag(byte[] bytes) {
        short s = 0;
        if (bytes.length > 2) {
            for (int i = 0; i < 2; i++) {
                s <<= 8;
                s |= (bytes[i] & 0x00ff);
            }
        }
        return s;
    }
}
