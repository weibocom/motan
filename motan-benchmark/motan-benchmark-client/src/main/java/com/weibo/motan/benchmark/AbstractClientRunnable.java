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

package com.weibo.motan.benchmark;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public abstract class AbstractClientRunnable implements ClientRunnable {

    RunnableStatistics statistics;
    private CyclicBarrier cyclicBarrier;
    private CountDownLatch countDownLatch;
    private long startTime;
    private long endTime;
    private int statisticTime;
    private BenchmarkService benchmarkService;

    public AbstractClientRunnable(BenchmarkService benchmarkService, CyclicBarrier barrier, CountDownLatch latch, long startTime, long endTime) {
        this.cyclicBarrier = barrier;
        this.countDownLatch = latch;
        this.startTime = startTime;
        this.endTime = endTime;
        this.benchmarkService = benchmarkService;

        statisticTime = (int) ((endTime - startTime) / 1000000);
        statistics = new RunnableStatistics(statisticTime);
    }

    @Override
    public RunnableStatistics getStatistics() {
        return statistics;
    }

    @Override
    public void run() {
        try {
            cyclicBarrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
        callService();
        countDownLatch.countDown();
    }

    private void callService() {
        long beginTime = System.nanoTime() / 1000L;
        while (beginTime <= startTime) {
            // warm up
            beginTime = System.nanoTime() / 1000L;
            Object result = call(benchmarkService);
        }
        while (beginTime <= endTime) {
            beginTime = System.nanoTime() / 1000L;
            Object result = call(benchmarkService);
            long responseTime = System.nanoTime() / 1000L - beginTime;
            collectResponseTimeDistribution(responseTime);
            int currTime = (int) ((beginTime - startTime) / 1000000L);
            if (currTime >= statisticTime) {
                continue;
            }
            if (result != null) {
                statistics.TPS[currTime]++;
                statistics.RT[currTime] += responseTime;
            } else {
                statistics.errTPS[currTime]++;
                statistics.errRT[currTime] += responseTime;
            }
        }
    }

    private void collectResponseTimeDistribution(long time) {
        double responseTime = (double) (time / 1000L);
        if (responseTime >= 0 && responseTime <= 1) {
            statistics.above0sum++;
        } else if (responseTime > 1 && responseTime <= 5) {
            statistics.above1sum++;
        } else if (responseTime > 5 && responseTime <= 10) {
            statistics.above5sum++;
        } else if (responseTime > 10 && responseTime <= 50) {
            statistics.above10sum++;
        } else if (responseTime > 50 && responseTime <= 100) {
            statistics.above50sum++;
        } else if (responseTime > 100 && responseTime <= 500) {
            statistics.above100sum++;
        } else if (responseTime > 500 && responseTime <= 1000) {
            statistics.above500sum++;
        } else if (responseTime > 1000) {
            statistics.above1000sum++;
        }
    }

    protected abstract Object call(BenchmarkService benchmarkService);

}
