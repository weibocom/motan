package com.weibo.api.motan.cluster.group;

import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.util.CollectionUtil;

import java.util.List;

@SpiMeta(name = "default")
public class DefaultClusterSelector<T> implements ClusterSelector<T> {
    public static final String DEFAULT_ROUTE_GROUP_SANDBOX = "sandbox";
    protected ClusterGroup<T> clusterGroup;
    protected Cluster<T> defaultSandboxCluster; // Default sandbox cluster
    protected Cluster<T> defaultGreyCluster; // Default grey cluster

    @Override
    public Cluster<T> select(Request request) {
        String routeGroup = request.getAttachment(MotanConstants.ROUTE_GROUP_KEY);
        if (routeGroup != null) {
            String sandboxGroup = null;
            String greyGroup = null;
            if (defaultSandboxCluster != null && !CollectionUtil.isEmpty(defaultSandboxCluster.getReferers())) {
                sandboxGroup = defaultSandboxCluster.getUrl().getGroup();
            }
            if (defaultGreyCluster != null && !CollectionUtil.isEmpty(defaultGreyCluster.getReferers())) {
                greyGroup = defaultGreyCluster.getUrl().getGroup();
            }

            // default sandbox value
            if (sandboxGroup != null && DEFAULT_ROUTE_GROUP_SANDBOX.equals(routeGroup.trim())) {
                return defaultSandboxCluster;
            }
            // Check the comma-separated group list
            String[] routeGroupList = routeGroup.split(",");
            for (String group : routeGroupList) {
                group = group.trim();
                if (sandboxGroup != null && sandboxGroup.equals(group)) {
                    return defaultSandboxCluster;
                }
                if (greyGroup != null && greyGroup.equals(group)) {
                    return defaultGreyCluster;
                }
            }
        }

        // If no matching rule is found, return the master cluster
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
        List<Cluster<T>> greyClusters = clusterGroup.getGreyClusters();
        if (!CollectionUtil.isEmpty(greyClusters)) {
            // Only save the first grey cluster as the default grey cluster
            this.defaultGreyCluster = greyClusters.get(0);
        }
    }

    @Override
    public void destroy() {
    }

}
