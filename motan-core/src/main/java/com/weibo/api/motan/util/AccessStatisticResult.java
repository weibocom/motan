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

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-26
 * 
 */
public class AccessStatisticResult {
    public int totalCount = 0;
    public int maxCount = -1;
    public int minCount = -1;

    public int slowCount = 0;
    public int bizExceptionCount = 0;
    public int otherExceptionCount = 0;

    public Histogram histogram = null;

    public double costTime = 0;
    public double bizTime = 0;

    public long slowThreshold = 200;
    public long[] intervalCounts = new long[5];

}
