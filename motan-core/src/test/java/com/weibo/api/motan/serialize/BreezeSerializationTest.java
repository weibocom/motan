/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
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

package com.weibo.api.motan.serialize;

import com.weibo.api.motan.codec.Serialization;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by zhanglei28 on 2019/4/3.
 */
public class BreezeSerializationTest {
    @Test
    public void serialize() throws Exception {
        Serialization serialization = new BreezeSerialization();
        BaseSerializeTest.testCompatibility(serialization);
        BaseSerializeTest.testSerialization(serialization);
        BaseSerializeTest.testSerializeMulti(serialization);
    }

    @Test
    public void getSerializationNumber() throws Exception {
        assertEquals(8, new BreezeSerialization().getSerializationNumber());
    }

    @Test
    // test serialize exception for motan1 protocol
    public void testException() throws IOException {
        MotanFrameworkException exception = new MotanFrameworkException("just test");
        BreezeSerialization serialization = new BreezeSerialization();
        byte[] bytes = serialization.serialize(exception);
        try {
            MotanFrameworkException result = serialization.deserialize(bytes, MotanFrameworkException.class);
            fail();
        } catch (MotanServiceException mse) {
            assertTrue(mse.getMessage().contains(exception.getClass().getName()));
            assertTrue(mse.getMessage().contains("just test"));
        }
    }

}
