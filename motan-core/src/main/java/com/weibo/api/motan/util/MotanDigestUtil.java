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

package com.weibo.api.motan.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.weibo.api.motan.common.MotanConstants;


/**
 *
 * 摘要算法辅助类
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-22
 */

public class MotanDigestUtil {

    private static Logger log = LoggerFactory.getLogger(MotanDigestUtil.class);

    private static ThreadLocal<CRC32> crc32Provider = new ThreadLocal<CRC32>() {
        @Override
        protected CRC32 initialValue() {
            return new CRC32();
        }
    };

    public static long getCrc32(String str) {
        try {
            return getCrc32(str.getBytes(MotanConstants.DEFAULT_CHARACTER));
        } catch (UnsupportedEncodingException e) {
            log.warn(String.format("Error: getCrc32, str=%s", str), e);
            return -1;
        }
    }

    public static long getCrc32(byte[] b) {
        CRC32 crc = crc32Provider.get();
        crc.reset();
        crc.update(b);
        return crc.getValue();
    }

    /*
     * 全小写32位MD5
     */
    public static String md5LowerCase(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plainText.getBytes());
            byte b[] = md.digest();
            int i;
            StringBuilder buf = new StringBuilder("");
            for (byte element : b) {
                i = element;
                if (i < 0) {
                    i += 256;
                }
                if (i < 16) {
                    buf.append("0");
                }
                buf.append(Integer.toHexString(i));
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("md5 digest error!", e);
        }
        return null;
    }
}
