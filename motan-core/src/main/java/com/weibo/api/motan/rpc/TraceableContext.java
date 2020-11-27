package com.weibo.api.motan.rpc;

import com.alibaba.fastjson.JSON;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author sunnights
 */
public class TraceableContext {
    protected AtomicLong receiveTime = new AtomicLong();
    protected AtomicLong sendTime = new AtomicLong();
    protected Map<String, String> traceInfoMap = new ConcurrentHashMap<>();

    public long getReceiveTime() {
        return receiveTime.get();
    }

    public void setReceiveTime(long receiveTime) {
        this.receiveTime.compareAndSet(0, receiveTime);
    }

    public long getSendTime() {
        return sendTime.get();
    }

    public void setSendTime(long sendTime) {
        this.sendTime.compareAndSet(0, sendTime);
    }

    public void addTraceInfo(String key, String value) {
        traceInfoMap.put(key, value);
    }

    public String getTraceInfo(String key) {
        return traceInfoMap.get(key);
    }

    public Map<String, String> getTraceInfoMap() {
        return traceInfoMap;
    }

    @Override
    public String toString() {
        return "send: " + sendTime + ", receive: " + receiveTime + ", info: " + JSON.toJSONString(traceInfoMap);
    }
}
