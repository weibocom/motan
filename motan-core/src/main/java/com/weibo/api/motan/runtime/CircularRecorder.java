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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhanglei28
 * @date 2024/3/1.
 */
public class CircularRecorder<T> {
    int size;
    AtomicInteger index = new AtomicInteger(0);
    Entry<?>[] array;

    public CircularRecorder(int size) {
        this.size = size;
        array = new Entry[size];
    }

    public void add(T t) {
        int i = index.getAndIncrement();
        if (i < 0) {
            i = -i;
        }
        array[i % size] = new Entry<>(t, System.currentTimeMillis());
    }

    // return the latest records map. map key is the record timestamp.
    @SuppressWarnings("unchecked")
    public Map<String, T> getRecords() {
        Map<String, T> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Entry<?> temp = array[i];
            if (temp != null) {
                map.put(i + ":" + temp.timestamp, (T) temp.value);
            }
        }
        return map;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            array[i] = null;
        }
        index.set(0);
    }

    static class Entry<T> {
        T value;
        long timestamp;

        public Entry(T value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}
