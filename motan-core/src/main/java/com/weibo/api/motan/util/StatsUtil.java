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

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.weibo.api.motan.closable.Closable;
import com.weibo.api.motan.closable.ShutDownHook;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.util.StatsUtil.AccessStatus;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.weibo.api.motan.common.MotanConstants.APPLICATION_STATISTIC;

/**
 * 
 * @author maijunsheng
 * @version 创建时间：2013-6-24
 */
public class StatsUtil {

    public static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    protected static ConcurrentMap<String, AccessStatisticItem> accessStatistics = new ConcurrentHashMap<String, AccessStatisticItem>();
    protected static List<StatisticCallback> statisticCallbacks = new CopyOnWriteArrayList<StatisticCallback>();
    public static String SEPARATE = "\\|";
    protected static ScheduledFuture<?> scheduledFuture;
    public static final String HISTOGRAM_NAME = MetricRegistry.name(AccessStatisticItem.class, "costTimeMillis");

    static {
        scheduledFuture = executorService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                // access statistic
                logAccessStatistic(true);
                // memory
                logMemoryStatistic();
                // callbacks
                logStatisticCallback();
            }
        }, MotanConstants.STATISTIC_PEROID, MotanConstants.STATISTIC_PEROID, TimeUnit.SECONDS);
        ShutDownHook.registerShutdownHook(new Closable() {
            @Override
            public void close() {
                if(!executorService.isShutdown()){
                    executorService.shutdown();
                }
            }
        });
    }

    public static void registryStatisticCallback(StatisticCallback callback) {
        if (callback == null) {
            LoggerUtil.warn("StatsUtil registryStatisticCallback is null");
            return;
        }

        statisticCallbacks.add(callback);
    }

    public static void unRegistryStatisticCallback(StatisticCallback callback) {
        if (callback == null) {
            LoggerUtil.warn("StatsUtil unRegistryStatisticCallback is null");
            return;
        }

        statisticCallbacks.remove(callback);
    }

    /**
     * callStatus: 0 is normal, 1 is bizExceptin, 2 is otherException
     *
     * @param name
     * @param currentTimeMillis
     * @param costTimeMillis
     * @param bizProcessTime
     * @param accessStatus
     */
    @Deprecated
    public static void accessStatistic(String name, long currentTimeMillis, long costTimeMillis, long bizProcessTime,
                                       AccessStatus accessStatus) {
        accessStatistic(name, URLParamType.application.getValue(), URLParamType.module.getValue(), currentTimeMillis, costTimeMillis, bizProcessTime, accessStatus);
    }

    public static void accessStatistic(String name, String application, String module, long currentTimeMillis, long costTimeMillis,
                                       long bizProcessTime, AccessStatus accessStatus) {
        if (name == null || name.isEmpty()) {
            return;
        }

        if (StringUtils.isBlank(application)) {
            application = URLParamType.application.getValue();
        }
        if (StringUtils.isBlank(module)){
            module = URLParamType.module.getValue();
        }

        name = name + "|" + application + "|" + module;

        try {
            AccessStatisticItem item = getStatisticItem(name, currentTimeMillis);

            item.statistic(currentTimeMillis, costTimeMillis, bizProcessTime, accessStatus);
        } catch (Exception e) {
        }
    }

    public static AccessStatisticItem getStatisticItem(String name, long currentTime) {
        AccessStatisticItem item = accessStatistics.get(name);

        if (item == null) {
            accessStatistics.putIfAbsent(name, new AccessStatisticItem(name, currentTime));
            item = accessStatistics.get(name);
        }

        return item;
    }

    public static ConcurrentMap<String, AccessStatisticResult> getTotalAccessStatistic() {
        return getTotalAccessStatistic(MotanConstants.STATISTIC_PEROID);
    }

    public static ConcurrentMap<String, AccessStatisticResult> getTotalAccessStatistic(int peroid) {
        if (peroid > MotanConstants.STATISTIC_PEROID) {
            throw new RuntimeException("peroid need <= " + MotanConstants.STATISTIC_PEROID);
        }

        long currentTimeMillis = System.currentTimeMillis();

        ConcurrentMap<String, AccessStatisticResult> totalResults = new ConcurrentHashMap<String, AccessStatisticResult>();

        for (Map.Entry<String, AccessStatisticItem> entry : accessStatistics.entrySet()) {
            AccessStatisticItem item = entry.getValue();

            AccessStatisticResult result = item.getStatisticResult(currentTimeMillis, MotanConstants.STATISTIC_PEROID);

            String key = entry.getKey();
            String[] keys = key.split(SEPARATE);
            if (keys.length != 3) {
                continue;
            }
            String application = keys[1];
            String module = keys[2];
            key = application + "|" + module;
            AccessStatisticResult appResult = totalResults.get(key);
            if (appResult == null) {
                totalResults.putIfAbsent(key, new AccessStatisticResult());
                appResult = totalResults.get(key);
            }


            appResult.totalCount += result.totalCount;
            appResult.bizExceptionCount += result.bizExceptionCount;
            appResult.slowCount += result.slowCount;
            appResult.costTime += result.costTime;
            appResult.bizTime += result.bizTime;
            appResult.otherExceptionCount += result.otherExceptionCount;

        }

        return totalResults;

    }

    public static void logAccessStatistic(boolean clear) {
        DecimalFormat mbFormat = new DecimalFormat("#0.00");
        long currentTimeMillis = System.currentTimeMillis();

        ConcurrentMap<String, AccessStatisticResult> totalResults = new ConcurrentHashMap<String, AccessStatisticResult>();

        for (Map.Entry<String, AccessStatisticItem> entry : accessStatistics.entrySet()) {
            AccessStatisticItem item = entry.getValue();

            AccessStatisticResult result = item.getStatisticResult(currentTimeMillis, MotanConstants.STATISTIC_PEROID);

            if (clear) {
                item.clearStatistic(currentTimeMillis, MotanConstants.STATISTIC_PEROID);
            }

            String key = entry.getKey();
            String[] keys = key.split(SEPARATE);
            if (keys.length != 3) {
                continue;
            }
            String application = keys[1];
            String module = keys[2];
            key = application + "|" + module;
            AccessStatisticResult appResult = totalResults.get(key);
            if (appResult == null) {
                totalResults.putIfAbsent(key, new AccessStatisticResult());
                appResult = totalResults.get(key);
            }


            appResult.totalCount += result.totalCount;
            appResult.bizExceptionCount += result.bizExceptionCount;
            appResult.slowCount += result.slowCount;
            appResult.costTime += result.costTime;
            appResult.bizTime += result.bizTime;
            appResult.otherExceptionCount += result.otherExceptionCount;

            Snapshot snapshot =
                    InternalMetricsFactory.getRegistryInstance(entry.getKey())
                            .histogram(HISTOGRAM_NAME).getSnapshot();

            if (application.equals(APPLICATION_STATISTIC)) {
                continue;
            }
            if (result.totalCount == 0) {
                LoggerUtil
                        .accessStatsLog("[motan-accessStatistic] app: "
                                + application
                                + " module: "
                                + module
                                + " item: "
                                + keys[0]
                                + " total_count: 0 slow_count: 0 biz_excp: 0 other_excp: 0 avg_time: 0.00ms biz_time: 0.00ms avg_tps: 0 max_tps: 0 min_tps: 0");
            } else {
                LoggerUtil
                        .accessStatsLog(
                                "[motan-accessStatistic] app: {} module: {} item: {} total_count: {} slow_count: {} p75: {} p95: {} p98: {} p99: {} p999: {} biz_excp: {} other_excp: {} avg_time: {}ms biz_time: {}ms avg_tps: {} max_tps: {} min_tps: {} ",
                                application, module, keys[0], result.totalCount, result.slowCount,
                                mbFormat.format(snapshot.get75thPercentile()), mbFormat.format(snapshot.get95thPercentile()),
                                mbFormat.format(snapshot.get98thPercentile()), mbFormat.format(snapshot.get99thPercentile()),
                                mbFormat.format(snapshot.get999thPercentile()), result.bizExceptionCount, result.otherExceptionCount,
                                mbFormat.format(result.costTime / result.totalCount), mbFormat.format(result.bizTime / result.totalCount),
                                (result.totalCount / MotanConstants.STATISTIC_PEROID), result.maxCount, result.minCount);
            }

        }

        if (!totalResults.isEmpty()) {
            for (Map.Entry<String, AccessStatisticResult> entry : totalResults.entrySet()) {
                String application = entry.getKey().split(SEPARATE)[0];
                String module = entry.getKey().split(SEPARATE)[1];
                AccessStatisticResult totalResult = entry.getValue();
                Snapshot snapshot =
                        InternalMetricsFactory.getRegistryInstance(entry.getKey())
                                .histogram(HISTOGRAM_NAME).getSnapshot();
                if (totalResult.totalCount > 0) {
                    LoggerUtil
                            .accessStatsLog(
                                    "[motan-totalAccessStatistic] app: {} module: {} total_count: {} slow_count: {} p75: {} p95: {} p98: {} p99: {} p999: {} biz_excp: {} other_excp: {} avg_time: {}ms biz_time: {}ms avg_tps: {}",
                                    application, module, totalResult.totalCount, totalResult.slowCount,
                                    mbFormat.format(snapshot.get75thPercentile()), mbFormat.format(snapshot.get95thPercentile()),
                                    mbFormat.format(snapshot.get98thPercentile()), mbFormat.format(snapshot.get99thPercentile()),
                                    mbFormat.format(snapshot.get999thPercentile()), totalResult.bizExceptionCount,
                                    totalResult.otherExceptionCount, mbFormat.format(totalResult.costTime / totalResult.totalCount),
                                    mbFormat.format(totalResult.bizTime / totalResult.totalCount),
                                    (totalResult.totalCount / MotanConstants.STATISTIC_PEROID));
                } else {
                    LoggerUtil.accessStatsLog("[motan-totalAccessStatistic] app: " + application + " module: " + module
                            + " total_count: 0 slow_count: 0 biz_excp: 0 other_excp: 0 avg_time: 0.00ms biz_time: 0.00ms avg_tps: 0");
                }

            }
        } else {
            LoggerUtil.accessStatsLog("[motan-totalAccessStatistic] app: " + URLParamType.application.getValue() + " module: "
                    + URLParamType.module.getValue()
                    + " total_count: 0 slow_count: 0 biz_excp: 0 other_excp: 0 avg_time: 0.00ms biz_time: 0.00ms avg_tps: 0");
        }

    }

    public static void logMemoryStatistic() {
        LoggerUtil.accessStatsLog("[motan-memoryStatistic] {} ", memoryStatistic());
    }

    public static String memoryStatistic() {
        Runtime runtime = Runtime.getRuntime();

        double freeMemory = (double) runtime.freeMemory() / (1024 * 1024);
        double maxMemory = (double) runtime.maxMemory() / (1024 * 1024);
        double totalMemory = (double) runtime.totalMemory() / (1024 * 1024);
        double usedMemory = totalMemory - freeMemory;
        double percentFree = ((maxMemory - usedMemory) / maxMemory) * 100.0;

        double percentUsed = 100 - percentFree;

        DecimalFormat mbFormat = new DecimalFormat("#0.00");
        DecimalFormat percentFormat = new DecimalFormat("#0.0");

        StringBuilder sb = new StringBuilder();
        sb.append(mbFormat.format(usedMemory)).append("MB of ").append(mbFormat.format(maxMemory)).append(" MB (")
                .append(percentFormat.format(percentUsed)).append("%) used");
        return sb.toString();
    }

    public static void logStatisticCallback() {
        for (StatisticCallback callback : statisticCallbacks) {
            try {
                String msg = callback.statisticCallback();

                if (msg != null && !msg.isEmpty()) {
                    LoggerUtil.accessStatsLog("[motan-statisticCallback] {}", msg);
                }
            } catch (Exception e) {
                LoggerUtil.error("StatsUtil logStatisticCallback Error: " + e.getMessage(), e);
            }
        }
    }

    public enum AccessStatus {
        NORMAL, BIZ_EXCEPTION, OTHER_EXCEPTION
    }

}


class AccessStatisticItem {
    private String name;
    private int currentIndex;
    private AtomicInteger[] costTimes = null;
    private AtomicInteger[] bizProcessTimes = null;
    private AtomicInteger[] totalCounter = null;
    private AtomicInteger[] slowCounter = null;
    private AtomicInteger[] bizExceptionCounter = null;
    private AtomicInteger[] otherExceptionCounter = null;

    private Histogram histogram = null;


    private int length;

    public AccessStatisticItem(String name, long currentTimeMillis) {
        this(name, currentTimeMillis, MotanConstants.STATISTIC_PEROID * 2);
    }

    public AccessStatisticItem(String name, long currentTimeMillis, int length) {
        this.name = name;
        this.costTimes = initAtomicIntegerArr(length);
        this.bizProcessTimes = initAtomicIntegerArr(length);
        this.totalCounter = initAtomicIntegerArr(length);
        this.slowCounter = initAtomicIntegerArr(length);
        this.bizExceptionCounter = initAtomicIntegerArr(length);
        this.otherExceptionCounter = initAtomicIntegerArr(length);
        this.length = length;
        this.currentIndex = getIndex(currentTimeMillis, length);
        this.histogram =
                InternalMetricsFactory.getRegistryInstance(name)
                        .histogram(StatsUtil.HISTOGRAM_NAME);
    }

    private AtomicInteger[] initAtomicIntegerArr(int size) {
        AtomicInteger[] arrs = new AtomicInteger[size];
        for (int i = 0; i < arrs.length; i++) {
            arrs[i] = new AtomicInteger(0);
        }

        return arrs;
    }

    /**
     * currentTimeMillis: 此刻记录的时间 (ms) costTimeMillis: 这次操作的耗时 (ms)
     *
     * @param currentTimeMillis
     * @param costTimeMillis
     * @param bizProcessTime
     * @param accessStatus
     */
    void statistic(long currentTimeMillis, long costTimeMillis, long bizProcessTime, AccessStatus accessStatus) {
        int tempIndex = getIndex(currentTimeMillis, length);

        if (currentIndex != tempIndex) {
            synchronized (this) {
                // 这一秒的第一条统计，把对应的存储位的数据置0
                if (currentIndex != tempIndex) {
                    reset(tempIndex);
                    currentIndex = tempIndex;
                }
            }
        }

        costTimes[currentIndex].addAndGet((int) costTimeMillis);
        bizProcessTimes[currentIndex].addAndGet((int) bizProcessTime);
        totalCounter[currentIndex].incrementAndGet();

        if (costTimeMillis >= MotanConstants.SLOW_COST) {
            slowCounter[currentIndex].incrementAndGet();
        }

        if (accessStatus == AccessStatus.BIZ_EXCEPTION) {
            bizExceptionCounter[currentIndex].incrementAndGet();
        } else if (accessStatus == AccessStatus.OTHER_EXCEPTION) {
            otherExceptionCounter[currentIndex].incrementAndGet();
        }
        histogram.update(costTimeMillis);
        String[] names = name.split("\\|");
        String appName = names[1] + "|" + names[2];
        InternalMetricsFactory.getRegistryInstance(appName).histogram(StatsUtil.HISTOGRAM_NAME)
                .update(costTimeMillis);
    }

    private int getIndex(long currentTimeMillis, int periodSecond) {
        return (int) ((currentTimeMillis / 1000) % periodSecond);
    }

    private void reset(int index) {
        costTimes[index].set(0);
        totalCounter[index].set(0);
        bizProcessTimes[index].set(0);
        slowCounter[index].set(0);
        bizExceptionCounter[index].set(0);
        otherExceptionCounter[index].set(0);
    }

    AccessStatisticResult getStatisticResult(long currentTimeMillis, int peroidSecond) {
        long currentTimeSecond = currentTimeMillis / 1000;
        currentTimeSecond--; // 当前这秒还没完全结束，因此数据不全，统计从上一秒开始，往前推移peroidSecond

        int startIndex = getIndex(currentTimeSecond * 1000, length);

        AccessStatisticResult result = new AccessStatisticResult();

        for (int i = 0; i < peroidSecond; i++) {
            int currentIndex = (startIndex - i + length) % length;

            result.costTime += costTimes[currentIndex].get();
            result.bizTime += bizProcessTimes[currentIndex].get();
            result.totalCount += totalCounter[currentIndex].get();
            result.slowCount += slowCounter[currentIndex].get();
            result.bizExceptionCount += bizExceptionCounter[currentIndex].get();
            result.otherExceptionCount += otherExceptionCounter[currentIndex].get();

            if (totalCounter[currentIndex].get() > result.maxCount) {
                result.maxCount = totalCounter[currentIndex].get();
            } else if (totalCounter[currentIndex].get() < result.minCount || result.minCount == -1) {
                result.minCount = totalCounter[currentIndex].get();
            }
        }

        return result;
    }

    void clearStatistic(long currentTimeMillis, int peroidSecond) {
        long currentTimeSecond = currentTimeMillis / 1000;
        currentTimeSecond--; // 当前这秒还没完全结束，因此数据不全，统计从上一秒开始，往前推移peroidSecond

        int startIndex = getIndex(currentTimeSecond * 1000, length);

        for (int i = 0; i < peroidSecond; i++) {
            int currentIndex = (startIndex - i + length) % length;

            reset(currentIndex);
        }
    }

}
