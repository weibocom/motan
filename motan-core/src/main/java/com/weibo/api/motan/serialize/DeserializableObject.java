package com.weibo.api.motan.serialize;

import com.weibo.api.motan.codec.Serialization;

import java.io.IOException;

/**
 * Created by zhanglei28 on 2017/5/9.
 */
public class DeserializableObject {
    private Serialization serialization;
    private byte[] objBytes;

    public DeserializableObject(Serialization serialization, byte[] objBytes) {
        this.serialization = serialization;
        this.objBytes = objBytes;
    }

    public <T> T deserialize(Class<T> clz) throws IOException{
        return serialization.deserialize(objBytes, clz);
    }

    public Object[] deserializeMulti(Class<?>[] paramTypes) throws IOException {
        Object[] ret = null;
        if(paramTypes != null && paramTypes.length > 0){
            if(paramTypes.length == 1){
                ret = new Object[1];
                ret[0] = serialization.deserialize(objBytes, paramTypes[0]);
            }else{
                ret = serialization.deserialize(objBytes, Object[].class);
            }
        }
        return ret;
    }
}
