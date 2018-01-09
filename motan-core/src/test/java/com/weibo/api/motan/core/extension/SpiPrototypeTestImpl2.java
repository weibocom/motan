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

package com.weibo.api.motan.core.extension;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author maijunsheng
 * @version 创建时间：2013-5-29
 */
@SpiMeta(name = "spiPrototypeTest2")
public class SpiPrototypeTestImpl2 implements SpiPrototypeInterface {
    private static AtomicLong counter = new AtomicLong(0);
    private long index = 0;

    public SpiPrototypeTestImpl2() {
        index = counter.incrementAndGet();
    }

    @Override
    public long spiHello() {
        System.out.println("SpiPrototypeTestImpl_" + index + " say hello");
        return index;
    }

}
