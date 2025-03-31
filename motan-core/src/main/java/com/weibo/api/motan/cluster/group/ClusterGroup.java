package com.weibo.api.motan.cluster.group;

import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.rpc.Caller;

import java.util.List;

public interface ClusterGroup<T> extends Caller<T> {
    String DEFAULT_CLUSTER_SELECTOR = URLParamType.clusterSelector.getValue();

    Cluster<T> getMasterCluster();

    List<Cluster<T>> getSandboxClusters();

    List<Cluster<T>> getBackupClusters();
} 