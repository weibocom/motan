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

package com.weibo.api.motan.benchmark;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-9
 *
 */
public class JdkProxyTest {

    public static void main(String[] args) {
        final ICall call = new Call();
        int count = 100000;

        ICall proxy = (ICall) Proxy.newProxyInstance(call.getClass().getClassLoader(), new Class[]{ICall.class}, new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return method.invoke(call, args);
            }

        });

        for (int i = 0; i < count; i++) {
            call.call_1kb_string();
            call.empty_method();
        }

        for (int i = 0; i < count; i++) {
            proxy.call_1kb_string();
            proxy.empty_method();
        }

        for (int i = 0; i < count / 100; i++) {
            call.sleep_1ms();
            proxy.sleep_1ms();
        }

        double[] costProxy = run(proxy, count, "proxy");
        double[] costCall = run(call, count, "call");


        System.out.println("************************");

        for (int i = 0; i < costProxy.length; i++) {
            System.out.println("proxy = call * " + (costProxy[i] / costCall[i]));
        }
    }

    private static double[] run(ICall call, int count, String name) {
        double[] costs = new double[3];

        long start = System.nanoTime();

        for (int i = 0; i < count; i++) {
            call.empty_method();
        }

        long cost = System.nanoTime() - start;
        costs[0] = cost;

        System.out.println(name + " emptyMethod count: " + count + " cost: " + cost + " avg: " + (cost / count) + "ns");

        start = System.nanoTime();

        for (int i = 0; i < count; i++) {
            call.call_1kb_string();
        }

        cost = System.nanoTime() - start;
        costs[1] = cost;

        System.out.println(name + " call_1kb_string count: " + count + " cost: " + cost + " avg: " + (cost / count) + "ns");

        start = System.nanoTime();

        for (int i = 0; i < count / 100; i++) {
            call.sleep_1ms();
        }

        cost = System.nanoTime() - start;
        costs[2] = cost;

        System.out.println(name + " sleep_1ms count: " + count + " cost: " + cost + " avg: " + (cost * 100 / count) + "ns");

        return costs;
    }

}


interface ICall {
    String call_1kb_string();

    void empty_method();

    void sleep_1ms();
}


class Call implements ICall {
    public String call_1kb_string() {

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < 1024; i++) {
            builder.append("i");
        }

        String response = "call_1kb_string(" + builder.toString() + ")";
        return response;
    }

    public void sleep_1ms() {
        try {
            Thread.sleep(1);
        } catch (Exception e) {}
    }

    public void empty_method() {}
}
