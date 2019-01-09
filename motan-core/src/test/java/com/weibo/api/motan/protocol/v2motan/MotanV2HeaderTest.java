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

package com.weibo.api.motan.protocol.v2motan;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by zhanglei28 on 2017/5/4.
 */
public class MotanV2HeaderTest {
    @Test
    public void testToBytes() throws Exception {
        MotanV2Header header = new MotanV2Header();
        header.setProxy(true);
        checkEquals(header);

        header.setRequestId(123l);
        checkEquals(header);


        header.setGzip(true);
        checkEquals(header);

        header.setOneway(true);
        checkEquals(header);

        header.setHeartbeat(true);
        checkEquals(header);

        header.setRequest(false);
        checkEquals(header);

        header.setVersion(6);
        checkEquals(header);

        header.setSerialize(3);
        checkEquals(header);

        header.setStatus(5);
        checkEquals(header);

        header.setRequestId(-798729l);
        checkEquals(header);
    }

    private void checkEquals(MotanV2Header header){
        byte[] headerBytes = header.toBytes();
        assertTrue(headerBytes.length == 13);
        assertEquals(header, MotanV2Header.buildHeader(headerBytes));
    }


}