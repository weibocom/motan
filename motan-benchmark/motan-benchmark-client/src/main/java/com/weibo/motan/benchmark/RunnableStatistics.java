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

public class RunnableStatistics {
    public int statisticTime;
    // Transaction per second
    public long[] TPS;
    // response times per second
    public long[] RT;
    // error Transaction per second
    public long[] errTPS;
    // error response times per second
    public long[] errRT;

    public long above0sum;      // [0,1]
    public long above1sum;      // (1,5]
    public long above5sum;      // (5,10]
    public long above10sum;     // (10,50]
    public long above50sum;     // (50,100]
    public long above100sum;    // (100,500]
    public long above500sum;    // (500,1000]
    public long above1000sum;   // > 1000

    public RunnableStatistics(int statisticTime) {
        this.statisticTime = statisticTime;
        TPS = new long[statisticTime];
        RT = new long[statisticTime];
        errTPS = new long[statisticTime];
        errRT = new long[statisticTime];
    }
}
