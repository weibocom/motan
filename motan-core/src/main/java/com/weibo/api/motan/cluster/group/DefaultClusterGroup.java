package com.weibo.api.motan.cluster.group;

import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.switcher.Switcher;
import com.weibo.api.motan.util.MathUtil;
import com.weibo.api.motan.util.MotanSwitcherUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultClusterGroup<T> implements ClusterGroup<T> {
    public static final String BACKUP_CLUSTER_SWITCHER_NAME = "feature.motan.backup.cluster.enable";
    protected static final String NO_REFERER_EXCEPTION_MESSAGE = "No available referers";
    protected static Switcher BACKUP_CLUSTER_SWITCHER = null;
    protected ClusterSelector<T> selector;
    protected Cluster<T> masterCluster;
    protected List<Cluster<T>> backupClusters;
    protected List<Cluster<T>> sandboxClusters;
    protected AtomicInteger backupIndex;

    static {
        if (MotanSwitcherUtil.canHoldSwitcher()) {
            BACKUP_CLUSTER_SWITCHER = MotanSwitcherUtil.getOrInitSwitcher(BACKUP_CLUSTER_SWITCHER_NAME, true);
        } else {
            MotanSwitcherUtil.initSwitcher(BACKUP_CLUSTER_SWITCHER_NAME, true);
        }
    }

    public DefaultClusterGroup(Cluster<T> masterCluster) {
        this.masterCluster = masterCluster;
    }

    @Override
    public Response call(Request request) {
        Cluster<T> cluster = masterCluster;
        if (selector != null) {
            cluster = selector.select(request);
            if (cluster == null) {
                throw new MotanServiceException("The ClusterSelector did not find an available Cluster");
            }
        }
        try {
            return cluster.call(request);
        } catch (MotanServiceException e) {
            if (backupClusters != null && !backupClusters.isEmpty() // backupCluster not empty
                    && cluster == masterCluster // and use master cluster to request
                    && e.getOriginMessage().contains(NO_REFERER_EXCEPTION_MESSAGE) // and is "no referer" exception
                    && MotanSwitcherUtil.isOpen(BACKUP_CLUSTER_SWITCHER, BACKUP_CLUSTER_SWITCHER_NAME)) { // and backup switcher is open
                return backupClusters.get(MathUtil.getNonNegative(backupIndex.incrementAndGet()) % backupClusters.size()).call(request);
            }
            throw e;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init() {
        // If there are no routable clusters, no selector is created.
        if (sandboxClusters != null) {
            selector = ExtensionLoader.getExtensionLoader(ClusterSelector.class)
                    .getExtension(masterCluster.getUrl().getParameter(URLParamType.clusterSelector.getName(),
                            DEFAULT_CLUSTER_SELECTOR));
            selector.init(this);
        }
        // The index counter is initialized only when backup clusters exists.
        if (backupClusters != null) {
            backupIndex = new AtomicInteger();
        }
    }

    @Override
    public void destroy() {
        if (selector != null) {
            selector.destroy();
        }
    }

    @Override
    public boolean isAvailable() {
        // The availability is determined by master cluster.
        return masterCluster.isAvailable();
    }

    @Override
    public Class<T> getInterface() {
        return masterCluster.getInterface();
    }

    @Override
    public String desc() {
        return masterCluster.desc();
    }

    @Override
    public URL getUrl() {
        return masterCluster.getUrl();
    }

    @Override
    public Cluster<T> getMasterCluster() {
        return masterCluster;
    }

    @Override
    public List<Cluster<T>> getBackupClusters() {
        return backupClusters;
    }

    public void setBackupClusters(List<Cluster<T>> backupClusters) {
        this.backupClusters = backupClusters;
    }

    @Override
    public List<Cluster<T>> getSandboxClusters() {
        return sandboxClusters;
    }

    public void setSandboxClusters(List<Cluster<T>> sandboxClusters) {
        this.sandboxClusters = sandboxClusters;
    }

    @Override
    public String toString() {
        return "DefaultClusterGroup {masterCluster=" + clusterToString(masterCluster) + ", backupClusters="
                + clustersToString(backupClusters)
                + ", sandboxClusters=" + clustersToString(sandboxClusters) + "}";
    }

    private String clustersToString(List<Cluster<T>> clusters) {
        if (clusters == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Cluster<T> cluster : clusters) {
            clusterToString(cluster, sb);
            sb.append(",");
        }
        if (clusters.size() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("]");
        return sb.toString();
    }

    private String clusterToString(Cluster<T> cluster) {
        if (cluster == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        clusterToString(cluster, sb);
        return sb.toString();
    }

    private void clusterToString(Cluster<T> cluster, StringBuilder sb) {
        if (cluster == null) {
            return;
        }
        List<Referer<T>> referers = cluster.getReferers();
        sb.append("[");
        if (referers != null) {
            for (Referer<T> refer : referers) {
                sb.append("{").append(refer.getUrl().toSimpleString()).append(", available:")
                        .append(refer.isAvailable()).append("}").append(",");
            }
            if (referers.size() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
        }
        sb.append("]");
    }
}
