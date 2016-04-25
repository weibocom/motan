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

package com.weibo.api.motan.registry;

import com.weibo.api.motan.rpc.URL;

import java.util.Collection;


/**
 * Register service to Restery center.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-16
 */

public interface RegistryService {

    /**
     * register service to registry
     *
     * @param url
     */
    void register(URL url);

    /**
     * unregister service to registry
     *
     * @param url
     */
    void unregister(URL url);

    /**
     * set service status to available, so clients could use it
     *
     * @param url service url to be available, <b>null</b> means all services
     */
    void available(URL url);

    /**
     * set service status to unavailable, client should not discover services of unavailable state
     *
     * @param url service url to be unavailable, <b>null</b> means all services
     */
    void unavailable(URL url);

    Collection<URL> getRegisteredServiceUrls();
}
