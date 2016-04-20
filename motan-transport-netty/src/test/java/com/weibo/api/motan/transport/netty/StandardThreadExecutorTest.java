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

package com.weibo.api.motan.transport.netty;

import java.util.concurrent.RejectedExecutionException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-20
 * 
 */
public class StandardThreadExecutorTest extends TestCase {
    private int minThread = 2;
    private int maxThread = 10;
    private int queueSize = 10;

    StandardThreadExecutor executor = new StandardThreadExecutor(minThread, maxThread, queueSize);

    // 如果该case有问题，有可能是否因为是在执行这个case的环境负载比较高等导致的问题，可以考虑把sleepTime调高成1s或者2s
    private int sleepTime = 100;
    public static Object obj = new Object();

    @Test
    public void testWaitRunnable() {
        Assert.assertEquals(executor.getPoolSize(), 0);

        for (int i = 1; i <= minThread; i++) {
            executor.execute(new WaitRunnable());
            Assert.assertEquals(executor.getPoolSize(), i);
            Assert.assertEquals(executor.getSubmittedTasksCount(), i);
            Assert.assertEquals(executor.getQueue().size(), 0);
        }

        for (int i = minThread + 1; i <= maxThread; i++) {
            executor.execute(new WaitRunnable());
            Assert.assertEquals(executor.getPoolSize(), i);
            Assert.assertEquals(executor.getSubmittedTasksCount(), i);
            Assert.assertEquals(executor.getQueue().size(), 0);
        }

        for (int i = 1; i <= queueSize; i++) {
            executor.execute(new WaitRunnable());
            Assert.assertEquals(executor.getPoolSize(), maxThread);
            Assert.assertEquals(executor.getSubmittedTasksCount(), i + maxThread);
            Assert.assertEquals(executor.getQueue().size(), i);
        }

        // reject task
        try {
            executor.execute(new WaitRunnable());
            Assert.assertTrue(false);
        } catch (RejectedExecutionException e) {} catch (Exception e) {
            Assert.assertTrue(false);
        }

        Assert.assertEquals(executor.getPoolSize(), maxThread);
        Assert.assertEquals(executor.getSubmittedTasksCount(), queueSize + maxThread);
        Assert.assertEquals(executor.getQueue().size(), queueSize);

        sleep(sleepTime);

        synchronized (obj) {
            obj.notifyAll();
        }

        sleep(sleepTime);

        Assert.assertEquals(executor.getSubmittedTasksCount(), queueSize);
        Assert.assertEquals(executor.getQueue().size(), 0);

        sleep(sleepTime);

        synchronized (obj) {
            obj.notifyAll();
        }

        sleep(sleepTime);

        Assert.assertEquals(executor.getSubmittedTasksCount(), 0);
        Assert.assertEquals(executor.getQueue().size(), 0);
    }

    @Test
    public void testPrestartAllCoreThreads() {
        Assert.assertEquals(executor.getPoolSize(), 0);

        executor.prestartAllCoreThreads();

        Assert.assertEquals(executor.getPoolSize(), minThread);
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (Exception e) {}
    }
}


class WaitRunnable implements Runnable {
    @Override
    public void run() {
        synchronized (StandardThreadExecutorTest.obj) {
            try {
                StandardThreadExecutorTest.obj.wait();
            } catch (Exception e) {}
        }
    }

}
