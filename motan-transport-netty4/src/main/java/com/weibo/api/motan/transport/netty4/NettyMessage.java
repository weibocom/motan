package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.protocol.rpc.RpcProtocolVersion;

/**
 * @author sunnights
 */
public class NettyMessage {
    private boolean isRequest;
    private long requestId;
    private byte[] data;
    private long startTime;
    private RpcProtocolVersion version;

    public NettyMessage(boolean isRequest, long requestId, byte[] data, RpcProtocolVersion version) {
        this.isRequest = isRequest;
        this.requestId = requestId;
        this.data = data;
        this.version = version;
    }

    public boolean isRequest() {
        return isRequest;
    }

    public void setRequest(boolean request) {
        isRequest = request;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public RpcProtocolVersion getVersion() {
        return version;
    }

    public void setVersion(RpcProtocolVersion version) {
        this.version = version;
    }
}
