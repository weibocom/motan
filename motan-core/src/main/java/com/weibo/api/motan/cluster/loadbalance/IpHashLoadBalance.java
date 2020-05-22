package com.weibo.api.motan.cluster.loadbalance;

import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.util.NetUtils;
import java.util.List;

/**
 * IP hash 负载均衡
 *
 * @author cszxyang
 * @version V1.0 created at: 2020-05-21
 */
@SpiMeta(name = "ipHash")
public class IpHashLoadBalance<T> extends AbstractLoadBalance<T> {

    @Override
    protected Referer<T> doSelect(Request request) {
        List<Referer<T>> refererList = getReferers();
        int hash = getHash(NetUtils.getLocalAddress().getHostAddress());
        for (int i = 0; i < refererList.size(); i++) {
            Referer<T> ref = refererList.get((hash + i) % refererList.size());
            if (ref.isAvailable()) {
                return ref;
            }
        }
        return null;
    }

    @Override
    protected void doSelectToHolder(Request request, List<Referer<T>> refersHolder) {
        List<Referer<T>> refererList = getReferers();
        int hash = getHash(NetUtils.getLocalAddress().getHostAddress());
        for (int i = 0, refCount = 0; refCount < MAX_REFERER_COUNT && i < refererList.size(); i++) {
            Referer<T> ref = refererList.get((hash + i) % refererList.size());
            if (ref.isAvailable()) {
                refersHolder.add(ref);
                refCount++;
            }
        }
    }

    private int getHash(String hostStr) {
        int hash = 89;
        String[] ipByteStrings = hostStr.split("\\.");
        for (String ipByteStr : ipByteStrings) {
            hash = (hash * 113 + Integer.parseInt(ipByteStr)) % 6271;
        }
        return hash;
    }
}