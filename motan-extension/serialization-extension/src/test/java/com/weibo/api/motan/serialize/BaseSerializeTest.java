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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.codec.Serialization;

public class BaseSerializeTest {
    Serialization serializer = null;
    BaseModel model;
    
    @Before
    public void setUp() throws Exception {
        model = BaseModel.getRandom();
    }

    @After
    public void tearDown() throws Exception {}
    
    @Test
    public void testBaseCase() throws Exception{
        if(serializer != null){
            byte[] bytes = serializer.serialize(model);
            assertTrue(bytes.length > 0);
            BaseModel result = serializer.deserialize(bytes, BaseModel.class);
            assertNotNull(result);
            assertEquals(model, result);
        }
    }
    
    /**
     * test no default constructor
     * @throws Exception
     */
    protected void testBigDecimal() throws Exception{
        BigDecimal bd  = new BigDecimal(123445);
        byte[] bytes = serializer.serialize(bd);
        BigDecimal result = serializer.deserialize(bytes, BigDecimal.class);
        assertEquals(bd, result);
    }
    

}
