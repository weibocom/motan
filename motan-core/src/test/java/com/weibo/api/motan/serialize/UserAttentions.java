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

package com.weibo.api.motan.serialize;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 该case主要用于验证 hessian 序列化的一个bug，见 testHessianBug，如果有bug，那么会导致exception
 * 
 * @author maijunsheng
 * 
 */
public class UserAttentions implements Serializable, Cloneable {

    private static final long serialVersionUID = 13L;

    private long uid; // 用户UID
    private long[] attentions = null; // 关注/粉丝集合
    private long lastAddtime; // 最后关注/粉丝时间
    private int count; // 关注数/粉丝数
    private long[] addTimes = null; // add attention/fan/filter times

    private Timestamp timeStamp = new Timestamp(System.currentTimeMillis());

    public static UserAttentions INVALID_USERATTENTIONS = new UserAttentions(0, new long[0], 0, 0L);
    public static UserAttentions INVALID_REINFORCED_USERATTENTIONS = new UserAttentions(0L, new long[0], new long[0], 0, 0L);

    public UserAttentions() {}

    public UserAttentions(long uid, long[] attentions, int count) {
        this.uid = uid;
        this.attentions = attentions;
        this.count = count;
    }

    public UserAttentions(long uid, long[] attentions, long[] addTimes, int count) {
        this.uid = uid;
        this.attentions = attentions;
        this.addTimes = addTimes;
        this.count = count;
    }

    public UserAttentions(long uid, long[] attentions, int count, long lastAddtime) {
        this.uid = uid;
        this.attentions = attentions;
        this.count = count;
        this.lastAddtime = lastAddtime;
    }

    public UserAttentions(long uid, long[] attentions, long[] addTimes, int count, long lastAddTime) {
        this.uid = uid;
        this.attentions = attentions;
        this.addTimes = addTimes;
        this.count = count;
        this.lastAddtime = lastAddTime;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public long[] getAttentions() {
        return attentions;
    }

    public void setAttentions(long[] attentions) {
        this.attentions = attentions;
    }

    public long getLastAddtime() {
        return lastAddtime;
    }

    public void setLastAddtime(long lastAddtime) {
        this.lastAddtime = lastAddtime;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long[] getAddTimes() {
        return addTimes;
    }

    public void setAddTimes(long[] addTimes) {
        this.addTimes = addTimes;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Timestamp getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Timestamp timeStamp) {
        this.timeStamp = timeStamp;
    }


}
