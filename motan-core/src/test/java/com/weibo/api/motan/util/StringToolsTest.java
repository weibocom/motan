/*
 *
 *   Copyright 2009-2022 Weibo, Inc.
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

package com.weibo.api.motan.util;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author zhanglei28
 * @date 2022/8/2.
 */
public class StringToolsTest {

    @Test
    public void testSplitSet() {
        checkSet("", ",");
        checkSet(null, ",");
        checkSet(null, null);
        checkSet(" leftSpace", ",", "leftSpace");
        checkSet(" leftSpace,rightSpace  ", ",", "leftSpace", "rightSpace");
        checkSet(" value1 , , ,  value2  ", ",", "value1", "value2");
        checkSet(" value1 , ,sameValue ,  sameValue  ", ",", "value1", "sameValue");
    }

    private void checkSet(String target, String regex, String... expectString) {
        Set<String> result = StringTools.splitSet(target, regex);
        if (expectString == null || expectString.length == 0) {
            assertEquals(0, result.size());
        } else {
            assertEquals(expectString.length, result.size());
            for (String s : expectString) {
                assertTrue(result.contains(s));
            }
        }
    }
}