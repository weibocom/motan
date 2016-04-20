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

package com.weibo.api.motan.protocol.example;

import java.util.concurrent.atomic.AtomicLong;

public class MockWorld implements IWorld {
    public AtomicLong count = new AtomicLong();
    public AtomicLong stringCount = new AtomicLong();
    public AtomicLong sleepCount = new AtomicLong();

    @Override
    public String world() {
        count.incrementAndGet();
        return "mockWorld";
    }

    @Override
    public String world(String world) {
        long num = stringCount.incrementAndGet();
        return world + num;
    }

    @Override
    public String worldSleep(String world, int sleep) {
        long num = sleepCount.incrementAndGet();
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException ignore) {}
        return world + num;
    }

}
