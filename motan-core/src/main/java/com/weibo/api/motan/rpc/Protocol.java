/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.weibo.api.motan.rpc;

import com.weibo.api.motan.core.extension.Scope;
import com.weibo.api.motan.core.extension.Spi;

/**
 * 
 * protocol
 * 
 * <pre>
 * 只负责点到点的通讯
 * </pre>
 * 
 * @author fishermen
 * @version V1.0 created at: 2013-5-16
 */
@Spi(scope = Scope.SINGLETON)
public interface Protocol {
    /**
     * 暴露服务
     * 
     * @param <T>
     * @param provider
     * @param url
     * @return
     */
    <T> Exporter<T> export(Provider<T> provider, URL url);

    /**
     * 引用服务
     * 
     * @param <T>
     * @param clz
     * @param url
     * @param serviceUrl
     * @return
     */
    <T> Referer<T> refer(Class<T> clz, URL url, URL serviceUrl);

    /**
     * <pre>
	 * 		1） exporter destroy
	 * 		2） referer destroy
	 * </pre>
     * 
     */
    void destroy();
}
