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

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.registry.Registry;
import com.weibo.api.motan.registry.support.AbstractRegistryFactory;
import com.weibo.api.motan.rpc.URL;
import org.apache.commons.lang3.StringUtils;

/**
 * @author zhanglei28
 * @date 2021/2/26.
 */
@SpiMeta(name = "weibomesh")
public class MeshRegistryFactory extends AbstractRegistryFactory {
    @Override
    protected Registry createRegistry(URL url) {
        return new MeshRegistry(url, new DefaultHttpMeshTransport());
    }

    @Override
    protected String getRegistryUri(URL url) {
        // registry uri with proxy registry
        String proxyRegistry = url.getParameter(URLParamType.proxyRegistryUrlString.name());
        if (StringUtils.isBlank(proxyRegistry)) {
            proxyRegistry = url.getParameter(url.getParameter(URLParamType.meshRegistryName.name()));
        }
        String registryUri = url.getUri() + "?proxyRegistry=" + proxyRegistry;
        return registryUri;
    }
}
