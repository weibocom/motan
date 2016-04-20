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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public abstract class AbstractBenchmarkClient {

    private static final int WARMUPTIME = 30;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private int concurrents;
    private int runTime;
    private String classname;
    private String params;
    private ClientStatistics statistics;

    /**
     *
     * @param concurrents 并发线程数
     * @param runtime benchmark实际运行时间
     * @param classname 测试的类名
     * @param params 测试String时，指String的size，单位为k
     */
    public void start(int concurrents, int runtime, String classname, String params) {
        this.concurrents = concurrents;
        this.runTime = runtime;
        this.classname = classname;
        this.params = params;

        printStartInfo();

        // prepare runnables
        long currentTime = System.nanoTime() / 1000L;
        long startTime = currentTime + WARMUPTIME * 1000 * 1000L;
        long endTime = currentTime + runTime * 1000 * 1000L;

        List<ClientRunnable> runnables = new ArrayList<>();
        CyclicBarrier cyclicBarrier = new CyclicBarrier(this.concurrents);
        CountDownLatch countDownLatch = new CountDownLatch(this.concurrents);
        for (int i = 0; i < this.concurrents; i++) {
            ClientRunnable runnable = getClientRunnable(classname, params, cyclicBarrier, countDownLatch, startTime, endTime);
            runnables.add(runnable);
            Thread thread = new Thread(runnable, "benchmarkclient-" + i);
            thread.start();
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<RunnableStatistics> runnableStatisticses = new ArrayList<>();
        for (ClientRunnable runnable : runnables) {
            runnableStatisticses.add(runnable.getStatistics());
        }
        statistics = new ClientStatistics(runnableStatisticses);
        statistics.collectStatistics();

        printStatistics();

        System.exit(0);
    }

    private void printStartInfo() {
        Date currentDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(Calendar.SECOND, runTime);
        Date finishDate = calendar.getTime();

        StringBuilder startInfo = new StringBuilder(dateFormat.format(currentDate));
        startInfo.append(" ready to start client benchmark");
        startInfo.append(", concurrent num is ").append(concurrents);
        startInfo.append(", the benchmark will end at ").append(dateFormat.format(finishDate));

        System.out.println(startInfo.toString());
    }

    private void printStatistics() {
        System.out.println("----------Benchmark Statistics--------------");
        System.out.println("Concurrents: " + concurrents);
        System.out.println("Runtime: " + runTime + " seconds");
        System.out.println("ClassName: " + classname);
        System.out.println("Params: " + params);
        statistics.printStatistics();
    }

    public abstract ClientRunnable getClientRunnable(String classname, String params, CyclicBarrier barrier, CountDownLatch latch, long startTime, long endTime);
}
