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

package com.weibo.api.motan.protocol.v2motan;

import com.weibo.api.motan.exception.MotanServiceException;

import java.nio.ByteBuffer;

/**
 * Created by zhanglei28 on 2017/5/3.
 */
public class MotanV2Header {
    public static final short MAGIC = (short) 0xF1F1;
    private int version = 1;//rpc协议版本号。motan1对应 0， motan2对应1
    private boolean heartbeat = false;//是否心跳消息。
    private boolean gzip = false; //是否gzip压缩消息
    private boolean oneway = false;//是否单向消息。单向消息不需要response
    private boolean proxy = false;// 是否需要代理请求。motan agent使用。
    private boolean request = true; //消息类型是否是request
    private int status = 0; //消息状态。最大能表示8种状态，最大值为7。 0表示正常消息，1表示异常消息。其他待扩展
    private int serialize = 1;// 消息body序列化方式，最大支持32种方式，最大值31。0 hessian、1 grpc-pb、2 json、3 msgpack、4 hprose、5 pb、6 simple、7 grpc-pb-json
    private long requestId;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(boolean heartbeat) {
        this.heartbeat = heartbeat;
    }

    public boolean isGzip() {
        return gzip;
    }

    public void setGzip(boolean gzip) {
        this.gzip = gzip;
    }

    public boolean isOneway() {
        return oneway;
    }

    public void setOneway(boolean oneway) {
        this.oneway = oneway;
    }

    public boolean isProxy() {
        return proxy;
    }

    public void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    public boolean isRequest() {
        return request;
    }

    public void setRequest(boolean request) {
        this.request = request;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getSerialize() {
        return serialize;
    }

    public void setSerialize(int serialize) {
        this.serialize = serialize;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public byte[] toBytes() {
        ByteBuffer buf = ByteBuffer.allocate(13);
        buf.putShort(MAGIC);
        byte msgType = (byte) 0x00;
        if (heartbeat) {
            msgType = (byte) (msgType | 0x10);
        }
        if (gzip) {
            msgType = (byte) (msgType | 0x08);
        }
        if (oneway) {
            msgType = (byte) (msgType | 0x04);
        }
        if (proxy) {
            msgType = (byte) (msgType | 0x02);
        }
        if (!request) {
            msgType = (byte) (msgType | 0x01);
        }

        buf.put(msgType);
        byte vs = 0x08;
        if (version != 1) {
            vs = (byte) ((version << 3) & 0xf8);
        }
        if (status != 0) {
            vs = (byte) (vs | (status & 0x07));
        }
        buf.put(vs);
        byte se = 0x08;
        if (serialize != 1) {
            se = (byte) ((serialize << 3) & 0xf8);
        }
        buf.put(se);
        buf.putLong(requestId);
        buf.flip();
        return buf.array();

    }

    public static MotanV2Header buildHeader(byte[] headerBytes) {
        ByteBuffer buf = ByteBuffer.wrap(headerBytes);
        short mg = buf.getShort();
        if (mg != MAGIC) {
            throw new MotanServiceException("decode motan v2 header fail. magicnum not correct. magic:" + mg);
        }
        MotanV2Header header = new MotanV2Header();
        byte b = buf.get();
        if ((b & 0x10) == 0x10) {
            header.setHeartbeat(true);
        }
        if ((b & 0x08) == 0x08) {
            header.setGzip(true);
        }
        if ((b & 0x04) == 0x04) {
            header.setOneway(true);
        }
        if ((b & 0x02) == 0x02) {
            header.setProxy(true);
        }
        if ((b & 0x01) == 0x01) {
            header.setRequest(false);
        }

        b = buf.get();
        header.setVersion((b >>> 3) & 0x1f);
        header.setStatus(b & 0x07);

        b = buf.get();
        header.setSerialize((b >>> 3) & 0x1f);

        header.setRequestId(buf.getLong());

        return header;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MotanV2Header that = (MotanV2Header) o;

        if (version != that.version) return false;
        if (heartbeat != that.heartbeat) return false;
        if (gzip != that.gzip) return false;
        if (oneway != that.oneway) return false;
        if (proxy != that.proxy) return false;
        if (request != that.request) return false;
        if (status != that.status) return false;
        if (serialize != that.serialize) return false;
        return requestId == that.requestId;
    }

    @Override
    public int hashCode() {
        int result = version;
        result = 31 * result + (heartbeat ? 1 : 0);
        result = 31 * result + (gzip ? 1 : 0);
        result = 31 * result + (oneway ? 1 : 0);
        result = 31 * result + (proxy ? 1 : 0);
        result = 31 * result + (request ? 1 : 0);
        result = 31 * result + status;
        result = 31 * result + serialize;
        result = 31 * result + (int) (requestId ^ (requestId >>> 32));
        return result;
    }

    public static enum MessageStatus {
        NORMAL(0),
        EXCEPTION(1);

        private final int status;

        private MessageStatus(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}
