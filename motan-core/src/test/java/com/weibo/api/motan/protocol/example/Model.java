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

package com.weibo.api.motan.protocol.example;

import java.io.Serializable;

/**
 * @author maijunsheng
 * @version 创建时间：2013-5-26
 * 
 */
public class Model implements Serializable {
    private static final long serialVersionUID = -6642850886054518156L;

    private String name;
    private int age;
    private Class<?> type;
    private long[] addTimes = null; // add attention/fan/filter times

    public Model() {}

    public Model(String name, int age, Class<?> type) {
        this.name = name;
        this.age = age;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public String toString() {
        return name + "," + age + "," + type.getName();
    }

    public long[] getAddTimes() {
        return addTimes;
    }

    public void setAddTimes(long[] addTimes) {
        this.addTimes = addTimes;
    }

}
