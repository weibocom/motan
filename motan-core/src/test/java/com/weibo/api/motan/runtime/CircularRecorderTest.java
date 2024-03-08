/*
 *
 *   Copyright 2009-2024 Weibo, Inc.
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

package com.weibo.api.motan.runtime;

import junit.framework.TestCase;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @author zhanglei28
 * @date 2024/3/7.
 */
public class CircularRecorderTest extends TestCase {


    public void testCircularRecorder() throws InterruptedException {
        int size = 10;
        CircularRecorder<Integer> circularRecorder = new CircularRecorder<>(size);
        assertEquals(size, circularRecorder.array.length);
        int concurrent = 10;
        int times = 3000;
        final CountDownLatch latch = new CountDownLatch(concurrent);
        for (int i = 0; i < concurrent; i++) {
            new Thread(() -> {
                for (int j = 0; j < times; j++) {
                    circularRecorder.add(j);
                    if (j > size) { // concurrent add/get
                        Map<String, Integer> temp = circularRecorder.getRecords();
                        assertEquals(size, temp.size());
                    }
                }
                latch.countDown();
            }).start();
        }
        latch.await();
        Map<String, Integer> records = circularRecorder.getRecords();
        assertEquals(size, records.size());
        assertEquals(concurrent * times, circularRecorder.index.get());

        // clear
        circularRecorder.clear();
        records = circularRecorder.getRecords();
        assertEquals(0, records.size());
        assertEquals(0, circularRecorder.index.get());
    }
}