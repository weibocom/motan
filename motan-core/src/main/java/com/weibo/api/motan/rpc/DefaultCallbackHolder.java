/*
 *
 *   Copyright 2009-2023 Weibo, Inc.
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

package com.weibo.api.motan.rpc;

import com.weibo.api.motan.util.AsyncUtil;
import com.weibo.api.motan.util.LoggerUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author zhanglei28
 * @date 2023/10/13.
 */
public class DefaultCallbackHolder implements Callbackable {
    private final List<Pair<Runnable, Executor>> taskList = new ArrayList<>();
    private volatile boolean isFinished = false;

    public void addFinishCallback(Runnable runnable, Executor executor) {
        if (!isFinished) {
            synchronized (this) {
                if (!isFinished) {
                    taskList.add(Pair.of(runnable, executor));
                    return;
                }
            }
        }
        process(runnable, executor);
    }

    @Override
    public void onFinish() {
        if (!isFinished) {
            synchronized (this) {
                if (!isFinished) {
                    for (Pair<Runnable, Executor> pair : taskList) {
                        process(pair.getKey(), pair.getValue());
                    }
                    isFinished = true;
                }
            }
        }
    }

    private void process(Runnable runnable, Executor executor) {
        if (executor == null) {
            executor = AsyncUtil.getDefaultCallbackExecutor();
        }
        try {
            executor.execute(runnable);
        } catch (Exception e) {
            LoggerUtil.error("Callbackable response exec callback task error, e: ", e);
        }
    }
}
