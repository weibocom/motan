package com.weibo.api.motan.registry.zookeeper;

import com.weibo.api.motan.util.ByteUtil;
import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.SerializableSerializer;

import java.io.ObjectStreamConstants;
import java.io.UnsupportedEncodingException;

public class StringSerializer extends SerializableSerializer {
    @Override
    public Object deserialize(byte[] bytes) throws ZkMarshallingError {
        try {
            if (ByteUtil.bytes2short(bytes, 0) == ObjectStreamConstants.STREAM_MAGIC) {
                return super.deserialize(bytes);
            }
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ZkMarshallingError(e);
        }
    }

    @Override
    public byte[] serialize(Object obj) throws ZkMarshallingError {
        try {
            return obj.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ZkMarshallingError(e);
        }
    }
}
