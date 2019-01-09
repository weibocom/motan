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

package com.weibo.api.motan.util;

import com.weibo.api.motan.rpc.Application;
import com.weibo.api.motan.util.StatsUtil.AccessStatus;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-14
 * 
 */
public class StatsUtilTest {

    @Test
    public void test() {
        final int loop = 10;
        final int count = 10000;
        String name1 = "hello";
        String name2 = "world";
        Application app1 = new Application("application1", "module1");
        Application app2 = new Application("application2", "module2");

        long start = System.currentTimeMillis();

        for (int j = 0; j < loop; j++) {
            for (int i = 0; i < count; i++) {
                long currentTimeMillis = System.currentTimeMillis();
                long costTime = currentTimeMillis - start;
                long bizTime = costTime / 2;
                StatsUtil.accessStatistic(name1, "application1", "module1", currentTimeMillis, costTime, bizTime, AccessStatus.NORMAL);
                StatsUtil.accessStatistic(name2, "application1", "module1", currentTimeMillis, costTime, bizTime, AccessStatus.NORMAL);
                StatsUtil.accessStatistic(name1, "application2", "module2", currentTimeMillis, costTime, bizTime, AccessStatus.NORMAL);
            }
        }

        try {
            Thread.sleep(1000);
        } catch (Exception e) {}
        for (Map.Entry<String, AccessStatisticResult> entry : StatsUtil.getTotalAccessStatistic().entrySet()) {
            if ("application1|module1".equals(entry.getKey())) {
                Assert.assertEquals(loop * count * 2, entry.getValue().totalCount);
            }
            if ("application2|module2".equals(entry.getKey())) {
                Assert.assertEquals(loop * count, entry.getValue().totalCount);
            }
        }

    }
}
