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

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public class TestPojoRunnable extends AbstractClientRunnable {
    Person person = new Person();

    public TestPojoRunnable(BenchmarkService service, String params, CyclicBarrier barrier, CountDownLatch latch, long startTime, long endTime) {
        super(service, barrier, latch, startTime, endTime);
        person.setName("motan");
        person.setFullName(new FullName("first", "last"));
        person.setBirthday(new Date());
        List<String> phoneNumber = new ArrayList<String>();
        phoneNumber.add("123");
        person.setPhoneNumber(phoneNumber);
        person.setEmail(phoneNumber);
        Map<String, String> address = new HashMap<String, String>();
        address.put("hat", "123");
        person.setAddress(address);
    }

    @Override
    protected Object call(BenchmarkService benchmarkService) {
        Object result = benchmarkService.echoService(person);
        return result;
    }
}
