package com.weibo.api.motan.cluster.group;

import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.core.extension.Scope;
import com.weibo.api.motan.core.extension.Spi;
import com.weibo.api.motan.rpc.Request;

@Spi(scope = Scope.PROTOTYPE)
public interface ClusterSelector<T> {

    // Select a cluster based on the Request
    Cluster<T> select(Request request);

    void init(ClusterGroup<T> clusterGroup);

    void destroy();
}
