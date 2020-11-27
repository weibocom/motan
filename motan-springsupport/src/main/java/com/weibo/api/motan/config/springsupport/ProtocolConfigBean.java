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

package com.weibo.api.motan.config.springsupport;

import com.weibo.api.motan.config.ProtocolConfig;
import org.springframework.beans.factory.BeanNameAware;

/**
 * @author fld
 *
 * Created by fld on 16/5/13.
 */
public class ProtocolConfigBean extends ProtocolConfig implements BeanNameAware {

    @Override
    public void setBeanName(String name) {
        setId(name);
        MotanNamespaceHandler.protocolDefineNames.add(name);
    }
}
