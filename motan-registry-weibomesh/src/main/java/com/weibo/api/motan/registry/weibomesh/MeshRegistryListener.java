/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.registry.weibomesh;

import com.google.common.collect.ImmutableList;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.CollectionUtil;
import com.weibo.api.motan.util.ConcurrentHashSet;
import com.weibo.api.motan.util.LoggerUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author zhanglei28
 * @date 2021/6/30.
 */
public class MeshRegistryListener implements NotifyListener {
    private static int DEFAULT_COPY = 2;
    private MeshRegistry meshRegistry;
    private ConcurrentHashSet<NotifyListener> listeners = new ConcurrentHashSet<>();
    private ImmutableList<URL> meshNodes;
    private URL subscribeUrl;
    private List<URL> backupNodes = null;

    public MeshRegistryListener(MeshRegistry meshRegistry, URL subscribeUrl) {
        this.meshRegistry = meshRegistry;
        URL meshNode = subscribeUrl.createCopy();
        URL meshUrl = meshRegistry.getUrl();
        meshNode.setHost(meshUrl.getHost());
        meshNode.setPort(meshUrl.getPort());
        if (StringUtils.isNotBlank(meshUrl.getParameter(URLParamType.fusingThreshold.getName()))) {
            meshNode.addParameter(URLParamType.fusingThreshold.getName(), meshUrl.getParameter(URLParamType.fusingThreshold.getName()));
        }
        int copy = meshUrl.getIntParameter(MeshRegistry.MESH_PARAM_COPY, DEFAULT_COPY);
        ImmutableList.Builder builder = ImmutableList.builder();
        for (int i = 0; i < copy; i++) {
            builder.add(meshNode);
        }
        this.meshNodes = builder.build();
        this.subscribeUrl = subscribeUrl;
    }

    @Override
    public void notify(URL registryUrl, List<URL> urls) {
        if (!CollectionUtil.isEmpty(urls)) {
            this.backupNodes = urls;
            if (!meshRegistry.isUseMesh()) { // 当前未使用mesh时，直接通知
                doNotify(false);
            }
        }
    }

    // 确定通知时调用。
    // 根据useMesh参数决定通知的节点，因此在调用此方法前需要自行判定是否正在使用mesh。
    public void doNotify(boolean useMesh) {
        for (NotifyListener listener : listeners) {
            try {
                if (useMesh) {
                    listener.notify(meshRegistry.getUrl(), meshNodes);
                } else {
                    if (CollectionUtil.isEmpty(backupNodes)) {
                        LoggerUtil.info("mesh registry backupNodes is empty, not notify. url:" + subscribeUrl.toSimpleString());
                        return; // 没有后备节点时，不做通知。
                    }
                    listener.notify(meshRegistry.getUrl(), backupNodes);
                }
            } catch (Exception e) {
                LoggerUtil.warn("MeshRegistryListner notify fail. listner url:" + subscribeUrl.toSimpleString());
            }
        }
    }

    public List<URL> getUrls() {
        if (!meshRegistry.isUseMesh() && !CollectionUtil.isEmpty(backupNodes)) {
            return backupNodes;
        }
        return meshNodes;
    }

    public List<URL> getBackupNodes() {
        return backupNodes;
    }

    public List<URL> getMeshNodes() {
        return meshNodes;
    }

    public void addListener(NotifyListener listener) {
        listeners.add(listener);
    }

    public boolean removeListener(NotifyListener listener) {
        return listeners.remove(listener);
    }
}
