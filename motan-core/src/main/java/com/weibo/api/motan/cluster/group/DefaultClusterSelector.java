package com.weibo.api.motan.cluster.group;

import java.util.List;

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
    protected Cluster<T> defaultSandboxCluster; // Default sandbox cluster

    @Override
    public Cluster<T> select(Request request) {
        if (defaultSandboxCluster != null && !CollectionUtil.isEmpty(defaultSandboxCluster.getReferers())) {
            // Check the route group of the request
            String routeGroup = request.getAttachment(MotanConstants.ROUTE_GROUP_KEY);
            if (routeGroup != null) {
                routeGroup = routeGroup.trim();
                // If the route group is sandbox, directly return the default sandbox cluster
                if (DEFAULT_ROUTE_GROUP_SANDBOX.equals(routeGroup)) {
                    return defaultSandboxCluster;
                }
                
                // Check the comma-separated group list
                String[] routeGroupList = routeGroup.split(",");
                String ownGroup = clusterGroup.getUrl().getGroup();
                for (String group : routeGroupList) {
                    group = group.trim();
                    if (group.equals(ownGroup)) {
                        return defaultSandboxCluster;
                    }
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
            // Only save the first sandbox cluster as the default sandbox cluster
            this.defaultSandboxCluster = sandboxClusters.get(0);
        }
    }
    @Override
    public void destroy() {
    }

}
