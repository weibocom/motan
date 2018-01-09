/*
 * Copyright 2009-2016 Weibo, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.weibo.motan.demo.service;

import com.weibo.api.motan.transport.async.MotanAsync;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@MotanAsync
public interface TestInterface<K, V> extends TestSuperInterface {
    ArrayList<String> xxx(ConcurrentHashMap<String, Boolean> map);

    List<K> methodRaw();

    Map<K, V> methodRaw(List<String> list);

    List<String> methodType();

    List<?> methodWildcard();

    List<? extends Number> methodBoundedWildcard();

    <T extends List<String>> Map<K, V> methodTypeLiteral();

    <T extends List<T>> void method(String p1, T p2, List<?> p3, List<T> p4);
}
