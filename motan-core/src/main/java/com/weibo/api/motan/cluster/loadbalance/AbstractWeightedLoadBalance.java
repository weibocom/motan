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

package com.weibo.api.motan.cluster.loadbalance;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.DefaultThreadFactory;
import com.weibo.api.motan.core.StandardThreadExecutor;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zhanglei28
 * @date 2024/3/15.
 */
public abstract class AbstractWeightedLoadBalance<T> extends AbstractLoadBalance<T> {
    // Save all LBs that need to refresh weights
    static final ConcurrentHashSet<AbstractWeightedLoadBalance<?>> dynamicWeightedLoadBalances = new ConcurrentHashSet<>();
    private static final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);
    private static final ThreadPoolExecutor taskExecutor; // thread pool that performs referer refresh tasks
    public static final String WEIGHT_META_SUFFIX_KEY = "WEIGHT";
    public static final int MIN_WEIGHT = 1;
    public static final int MAX_WEIGHT = 500; // protective restrictions
    protected static final int DEFAULT_WEIGHT = 10;
    protected boolean supportDynamicWeight = true; // Whether the current cluster supports dynamic weights

    protected volatile List<WeightedRefererHolder<T>> weightedRefererHolders; // The currently effective WeightedRefererHolders

    static {
        // default refresh period is 3 seconds
        long refreshPeriod = MathUtil.parseLong(MotanGlobalConfigUtil.getConfig(MotanConstants.WEIGHT_REFRESH_PERIOD_SECOND_KEY), 3);
        // default max thread is 100
        int maxThread = MathUtil.parseInt(MotanGlobalConfigUtil.getConfig(MotanConstants.WEIGHT_REFRESH_MAX_THREAD_KEY), 100);
        taskExecutor = new StandardThreadExecutor(10, maxThread, 30, TimeUnit.SECONDS, 10000,
                new DefaultThreadFactory("AbstractWeightedLoadBalance-refreshWeight-", true), new ThreadPoolExecutor.CallerRunsPolicy());
        long initDelay = refreshPeriod + ThreadLocalRandom.current().nextInt(10);
        scheduledExecutor.scheduleWithFixedDelay(AbstractWeightedLoadBalance::refreshAll, initDelay, refreshPeriod, TimeUnit.SECONDS);
    }

    private static void refreshAll() {
        for (AbstractWeightedLoadBalance<?> loadBalance : dynamicWeightedLoadBalances) {
            if (loadBalance.supportDynamicWeight) {
                taskExecutor.execute(loadBalance::refreshHoldersDynamicWeightTask);
            }
        }
    }

    @Override
    public void destroy() {
        if (supportDynamicWeight) {
            dynamicWeightedLoadBalances.remove(this);
        }
    }

    public void init(URL clusterUrl) {
        super.init(clusterUrl);
        supportDynamicWeight = clusterUrl.getBooleanParameter(URLParamType.dynamicMeta.getName(), URLParamType.dynamicMeta.getBooleanValue());
        if (supportDynamicWeight) {
            dynamicWeightedLoadBalances.add(this);
        }
    }

    @Override
    public void onRefresh(List<Referer<T>> referers) {
        super.onRefresh(referers);
        refreshRefererHolders();
    }

    private void refreshRefererHolders() {
        List<Referer<T>> referers = getReferers();
        List<WeightedRefererHolder<T>> allHolders = new ArrayList<>(referers.size());
        List<WeightedRefererHolder<T>> newHolders = new ArrayList<>();
        for (Referer<T> referer : referers) {
            WeightedRefererHolder<T> holder = null;
            if (weightedRefererHolders != null) {
                // Check whether referer can be reused
                for (WeightedRefererHolder<T> refererHolder : weightedRefererHolders) {
                    if (refererHolder.getReferer() == referer) { // reuse same referer object
                        holder = refererHolder;
                        break;
                    }
                }
            }

            if (holder == null) { // create new holder
                int staticWeight = DEFAULT_WEIGHT;
                try {
                    staticWeight = getRefererWeight(referer, false, DEFAULT_WEIGHT);
                } catch (ExecutionException ignore) {
                }
                holder = new WeightedRefererHolder<>(referer, staticWeight);
                newHolders.add(holder);
            }
            allHolders.add(holder);
        }

        // Only refresh new holders dynamic weight
        if (!newHolders.isEmpty()) {
            refreshDynamicWeight(newHolders, 15 * 1000);
        }
        weightedRefererHolders = allHolders;
        // Finally, notify the subclass of weight changes
        notifyWeightChange();
    }

    /**
     * This method is used to notify the subclass that the weight has changed, and the subclass needs to handle weight refresh.
     * Weight changes are triggered by referer list changes or referer's dynamic weight changes.
     * <p>
     * NOTICE: The method implementation should be synchronized.
     */
    abstract void notifyWeightChange();

    protected void refreshHoldersDynamicWeightTask() {
        // Only refresh historical holders
        List<WeightedRefererHolder<T>> tempHolders = weightedRefererHolders;
        try {
            boolean needNotify = refreshDynamicWeight(tempHolders, 30 * 1000);
            if (needNotify) {
                notifyWeightChange();
            }
        } catch (Exception e) {
            LoggerUtil.warn("refreshHoldersDynamicWeightTask fail. cluster:" + clusterUrl.getIdentity() + ", e:" + e.getMessage());
        }
    }

    private boolean refreshDynamicWeight(List<WeightedRefererHolder<T>> holders, long taskTimeout) {
        final AtomicBoolean needNotify = new AtomicBoolean(false);
        if (holders != null) {
            CountDownLatch countDownLatch = new CountDownLatch(holders.size());
            for (WeightedRefererHolder<T> holder : holders) {
                if (holder.supportDynamicWeight) {
                    // Concurrently refresh holder dynamic weights
                    taskExecutor.execute(() -> {
                        try {
                            int oldWeight = holder.dynamicWeight;
                            holder.dynamicWeight = getRefererWeight(holder.referer, true, 0);
                            if (oldWeight != holder.dynamicWeight) { // dynamic weight changed
                                needNotify.set(true);
                            }
                        } catch (Exception e) {
                            if (e.getCause() instanceof MotanServiceException
                                    && ((MotanServiceException) e.getCause()).getStatus() == MotanErrorMsgConstant.SERVICE_NOT_SUPPORT_ERROR.getStatus()) {
                                holder.supportDynamicWeight = false; // if remote service not support dynamic weight, set holder supportDynamicWeight to false
                            } else {
                                LoggerUtil.warn("refresh dynamic weight fail! url:" + holder.getReferer().getUrl().getIdentity() + ", error:" + e.getMessage());
                            }
                        } finally {
                            countDownLatch.countDown();
                        }
                    });
                } else {
                    countDownLatch.countDown();
                }
            }
            try {
                countDownLatch.await(taskTimeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignore) {
            }
        }
        return needNotify.get();
    }

    /**
     * Get weight from referer.
     * Subclasses can override this method to implement custom logic
     *
     * @param referer         Referer to get the weight
     * @param fromDynamicMeta true：from dynamic meta, false: from static meta
     * @param defaultWeight   default weight
     * @return weight
     */
    protected int getRefererWeight(Referer<T> referer, boolean fromDynamicMeta, int defaultWeight) throws ExecutionException {
        Map<String, String> meta;
        if (fromDynamicMeta) {
            meta = MetaUtil.getRefererDynamicMeta(referer);
        } else {
            meta = MetaUtil.getRefererStaticMeta(referer);
        }
        String weightString = MetaUtil.getMetaValue(meta, WEIGHT_META_SUFFIX_KEY);
        // Other weight calculation logic can be expanded here, such as calculating based on the number of cpu cores.
        return adjustWeight(referer, weightString, defaultWeight);
    }

    protected int adjustWeight(Referer<T> referer, String weight, int defaultWeight) {
        int w = defaultWeight;
        if (weight != null) {
            try {
                int temp = Integer.parseInt(weight);
                if (temp < MIN_WEIGHT) {
                    temp = MIN_WEIGHT;
                } else if (temp > MAX_WEIGHT) {
                    temp = MAX_WEIGHT;
                }
                w = temp;
            } catch (NumberFormatException e) {
                LoggerUtil.warn("WeightedRefererHolder parse weight fail. " + referer.getUrl().getIdentity() + ", use default weight " + defaultWeight + ", org weight:" + weight + ", error:" + e.getMessage());
            }
        }
        return w;
    }


    //Retaining Referer’s backward extension compatibility，just use as a holder, not a wrapper
    protected static class WeightedRefererHolder<T> {
        public Referer<T> referer;
        public int staticWeight;
        public boolean supportDynamicWeight;
        public volatile int dynamicWeight = 0;

        public WeightedRefererHolder(Referer<T> referer, int staticWeight) {
            this.referer = referer;
            this.staticWeight = staticWeight;
            supportDynamicWeight = referer.getUrl().getBooleanParameter(URLParamType.dynamicMeta.getName(), URLParamType.dynamicMeta.getBooleanValue());
        }

        public Referer<T> getReferer() {
            return referer;
        }

        public int getWeight() {
            if (dynamicWeight > 0)
                return dynamicWeight;
            return staticWeight;
        }
    }
}
