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

package com.weibo.api.motan.rpc;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * rpc call statistic.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-23
 */

public class RpcStats {

    private static final String SEPERATOR_METHOD_AND_PARAM = "|";

    private static ConcurrentHashMap<String, StatInfo> serviceStat = new ConcurrentHashMap<String, RpcStats.StatInfo>();
    private static ConcurrentHashMap<String, ConcurrentHashMap<String, StatInfo>> methodStat =
            new ConcurrentHashMap<String, ConcurrentHashMap<String, StatInfo>>();

    private static ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    /**
     * call before invoke the request
     * 
     * @param url
     * @param request
     */
    public static void beforeCall(URL url, Request request) {
        String uri = url.getUri();
        onBeforeCall(getServiceStat(uri));
        onBeforeCall(getMethodStat(uri, request.getMethodName(), request.getParamtersDesc()));
    }

    /**
     * call after invoke the request
     * 
     * @param url
     * @param request
     * @param success
     * @param procTimeMills
     */
    public static void afterCall(URL url, Request request, boolean success, long procTimeMills) {
        String uri = url.getUri();
        onAfterCall(getServiceStat(uri), success, procTimeMills);
        onAfterCall(getMethodStat(uri, request.getMethodName(), request.getParamtersDesc()), success, procTimeMills);
    }

    public static StatInfo getServiceStat(URL url) {
        return getServiceStat(url.getUri());
    }

    public static StatInfo getMethodStat(URL url, Request request) {
        return getMethodStat(url.getUri(), request.getMethodName(), request.getParamtersDesc());
    }

    private static StatInfo getServiceStat(String uri) {
        StatInfo stat = serviceStat.get(uri);
        if (stat == null) {
            stat = new StatInfo();
            serviceStat.putIfAbsent(uri, stat);
            stat = serviceStat.get(uri);
        }
        return stat;
    }

    private static StatInfo getMethodStat(String uri, String methodName, String methodParaDesc) {
        ConcurrentHashMap<String, StatInfo> sstats = methodStat.get(uri);
        if (sstats == null) {
            sstats = new ConcurrentHashMap<String, StatInfo>();
            methodStat.putIfAbsent(uri, sstats);
            sstats = methodStat.get(uri);
        }

        String methodNameAndParams = methodName + SEPERATOR_METHOD_AND_PARAM + methodParaDesc;
        StatInfo mstat = sstats.get(methodNameAndParams);
        if (mstat == null) {
            mstat = new StatInfo();
            sstats.putIfAbsent(methodNameAndParams, mstat);
            mstat = sstats.get(methodNameAndParams);
        }
        return mstat;
    }

    private static void onBeforeCall(StatInfo statInfo) {
        statInfo.activeCount.incrementAndGet();
    }

    private static void onAfterCall(StatInfo statInfo, boolean success, long procTimeMills) {
        statInfo.activeCount.decrementAndGet();
        if (!success) {
            statInfo.failCount.incrementAndGet();
        }
        statInfo.totalCountTime.inc(1, procTimeMills);
        statInfo.latestCountTime.inc(1, procTimeMills);
    }

    private static void startCleaner() {

    }

    private static void cleanLatestStat() {
        if (serviceStat.size() == 0) {
            return;
        }
        for (StatInfo si : serviceStat.values()) {
            si.resetLatestStat();
        }

    }

    public static class StatInfo {

        private AtomicInteger activeCount = new AtomicInteger();
        private AtomicLong failCount = new AtomicLong();
        private CountTime totalCountTime = new CountTime();
        private CountTime latestCountTime = new CountTime();

        public int getActiveCount() {
            return activeCount.get();
        }

        public long getFailCount() {
            return failCount.get();
        }

        public CountTime getTotalCountTime() {
            return totalCountTime;
        }

        public CountTime getLatestCountTime() {
            return latestCountTime;
        }

        public void resetLatestStat() {
            latestCountTime.reset();
        }
    }

    public static class CountTime {
        private AtomicLong count;
        private AtomicLong timeMills;

        public CountTime() {
            count = new AtomicLong();
            timeMills = new AtomicLong();
        }

        private void inc(int incCount, long incTimeMills) {
            count.getAndAdd(incCount);
            timeMills.getAndAdd(incTimeMills);
        }

        public long getCount() {
            return count.get();
        }

        public void reset() {
            count.set(0);
            timeMills.set(0);
        }

    }


}
