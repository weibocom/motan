package com.weibo.api.motan.cluster.group;

import java.util.List;
import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.rpc.Caller;

public interface ClusterGroup<T> extends Caller<T> {

    Cluster<T> getMasterCluster();

    List<Cluster<T>> getSandboxClusters();

    List<Cluster<T>> getBackupClusters();
    
} 