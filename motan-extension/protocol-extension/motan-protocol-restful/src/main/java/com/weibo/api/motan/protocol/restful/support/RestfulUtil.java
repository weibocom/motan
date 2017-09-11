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
package com.weibo.api.motan.protocol.restful.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.jboss.resteasy.util.Base64;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.StringTools;

public class RestfulUtil {
    public static final String ATTACHMENT_HEADER = "X-Attach";
    public static final String EXCEPTION_HEADER = "X-Exception";

    public static boolean isRpcRequest(MultivaluedMap<String, String> headers) {
        return headers != null && headers.containsKey(ATTACHMENT_HEADER);
    }

    public static void encodeAttachments(MultivaluedMap<String, Object> headers, Map<String, String> attachments) {
        if (attachments == null || attachments.isEmpty())
            return;

        StringBuilder value = new StringBuilder();
        for (Map.Entry<String, String> entry : attachments.entrySet()) {
            value.append(StringTools.urlEncode(entry.getKey())).append("=")
                    .append(StringTools.urlEncode(entry.getValue())).append(";");
        }

        if (value.length() > 1)
            value.deleteCharAt(value.length() - 1);

        headers.add(ATTACHMENT_HEADER, value.toString());
    }

    public static Map<String, String> decodeAttachments(MultivaluedMap<String, String> headers) {
        String value = headers.getFirst(ATTACHMENT_HEADER);

        Map<String, String> result = Collections.emptyMap();
        if (value != null && !value.isEmpty()) {
            result = new HashMap<String, String>();
            for (String kv : value.split(";")) {
                String[] pair = kv.split("=");
                if (pair.length == 2) {
                    result.put(StringTools.urlDecode(pair[0]), StringTools.urlDecode(pair[1]));
                }
            }
        }

        return result;
    }

    public static Response serializeError(Exception ex) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(ex);
            oos.flush();

            String info = new String(Base64.encodeBytesToBytes(baos.toByteArray()), MotanConstants.DEFAULT_CHARACTER);
            return Response.status(Status.EXPECTATION_FAILED).header(EXCEPTION_HEADER, ex.getClass()).entity(info)
                    .build();
        } catch (IOException e) {
            LoggerUtil.error("serialize " + ex.getClass() + " error", e);

            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("serialization " + ex.getClass() + " error")
                    .build();
        }
    }

    public static Exception getCause(BuiltResponse resp) {
        if (resp == null || resp.getStatus() != Status.EXPECTATION_FAILED.getStatusCode())
            return null;

        String exceptionClass = resp.getHeaderString(EXCEPTION_HEADER);
        if (!StringUtils.isBlank(exceptionClass)) {
            String body = resp.readEntity(String.class);
            resp.close();

            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(
                        Base64.decode(body.getBytes(MotanConstants.DEFAULT_CHARACTER)));
                ObjectInputStream ois = new ObjectInputStream(bais);
                return (Exception) ois.readObject();
            } catch (Exception e) {
                LoggerUtil.error("deserialize " + exceptionClass + " error", e);
            }
        }

        return null;
    }

}
