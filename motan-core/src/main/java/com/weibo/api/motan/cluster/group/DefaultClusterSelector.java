package com.weibo.api.motan.cluster.group;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.util.CollectionUtil;

@SpiMeta(name = "default")
public class DefaultClusterSelector<T> implements ClusterSelector<T>{
    public static final String DEFAULT_ROUTE_GROUP_SANDBOX = "sandbox";
    protected ClusterGroup<T> clusterGroup;
    protected Map<String, Cluster<T>> routeGroups; // routable cluster groups. 

    @Override
    public Cluster<T> select(Request request) {
        if (routeGroups != null) {
            // Request-specified route group
            String routeGroup = request.getAttachment(MotanConstants.ROUTE_GROUP_KEY);
            if (routeGroup != null) {
                Cluster<T> cluster = routeGroups.get(routeGroup.trim());
                if (cluster != null && !CollectionUtil.isEmpty(cluster.getReferers())) {
                    // If the cluster has referer, the cluster should be returned.
                    // If all referers in the cluster are unavailable, an exception should be triggered to make the business aware.
                    return cluster;
                }
            }
        }
        return clusterGroup.getMasterCluster();
    }

    @Override
    public void init(ClusterGroup<T> clusterGroup) {
        if (clusterGroup == null) {
            throw new MotanFrameworkException("ClusterGroup cannot be null");
        }
        this.clusterGroup = clusterGroup;
        List<Cluster<T>> sandboxClusters = clusterGroup.getSandboxClusters();
        if (!CollectionUtil.isEmpty(sandboxClusters)) {
            if (routeGroups == null) {
                routeGroups = new HashMap<>();
            }
            routeGroups.put(DEFAULT_ROUTE_GROUP_SANDBOX, sandboxClusters.get(0)); // The first cluster is the default sandbox cluster
            for (Cluster<T> cluster : sandboxClusters) {
                routeGroups.put(cluster.getUrl().getGroup(), cluster);
            }
        }
    }

    @Override
    public void destroy() {
    }

}
