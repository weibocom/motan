/**
 * Copyright (c) 2016, Blackboard Inc. All Rights Reserved.
 */
package com.weibo.api.motan.util;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.*;

/**
 * ClassName: RequestIdGeneratorTest Function: TODO
 *
 * @Author: dtang
 * @Date: 6/20/16, 9:07 PM
 */
public class RequestIdGeneratorTest {
    @Test
    public void testId() {
        RequestIdGenerator.offset.set(0);

        for (long i = 1L; i < RequestIdGenerator.MAX_COUNT_PER_MILLIS; i++) {
            long id = RequestIdGenerator.getRequestId();
            Assert.assertEquals(i, id & (RequestIdGenerator.MAX_COUNT_PER_MILLIS - 1));
        }
        Assert.assertEquals(1L, RequestIdGenerator.getRequestId() & (RequestIdGenerator.MAX_COUNT_PER_MILLIS - 1));
        Assert.assertEquals(2L, RequestIdGenerator.getRequestId() & (RequestIdGenerator.MAX_COUNT_PER_MILLIS - 1));
    }

    @Test
    public void testNoMillisCollide() {

        int threadNum = Runtime.getRuntime().availableProcessors() * 200;
        final CyclicBarrier cyclicBarrier = new CyclicBarrier(threadNum, new Runnable() {
            @Override
            public void run() {
                System.out.println("barrier start");
            }
        });
        final ConcurrentMap<Long, Object> memory = new ConcurrentHashMap<Long, Object>();
        ExecutorService executor = Executors.newFixedThreadPool(threadNum);
        List<Future<Boolean>> futureList = Lists.newArrayList();

        for (int i = 0; i < threadNum; i++) {
            futureList.add(executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    cyclicBarrier.await();
                    long id = RequestIdGenerator.getRequestId();
                    boolean result = memory.putIfAbsent(id, "") == null;
                    return result;

                }
            }));
        }
        Assert.assertEquals(threadNum, futureList.size());
        try {
            executor.shutdown();
            executor.awaitTermination(100, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assert.assertEquals(threadNum, memory.size());
        for(Future<Boolean> future: futureList){
            try {
                Assert.assertTrue(future.get());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
