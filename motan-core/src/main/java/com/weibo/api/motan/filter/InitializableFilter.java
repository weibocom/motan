/*
 * Copyright 2009-2016 Weibo, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.weibo.api.motan.filter;

import com.weibo.api.motan.core.extension.Scope;
import com.weibo.api.motan.core.extension.Spi;
import com.weibo.api.motan.rpc.Caller;

/**
 * 
 * @Description InitializableFilter
 * @author zhanglei
 * @date Nov 11, 2016
 *
 */
@Spi(scope = Scope.PROTOTYPE)
public interface InitializableFilter extends Filter {
    /**
     * init with caller eg. referer or provider be careful when using SINGLETON scope
     * 
     * @param caller
     */
    void init(Caller<?> caller);

}
