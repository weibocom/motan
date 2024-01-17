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

package com.weibo.api.motan.rpc;

import java.util.Map;

/**
 * Request
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-16
 */
public interface Request {

    /**
     * service interface
     */
    String getInterfaceName();

    /**
     * service method name
     */
    String getMethodName();

    /**
     * service method param desc (sign)
     */
    String getParamtersDesc();

    /**
     * service method param
     */
    Object[] getArguments();

    /**
     * get framework param
     */
    Map<String, String> getAttachments();

    /**
     * set framework param
     */
    void setAttachment(String name, String value);

    /**
     * request id
     */
    long getRequestId();

    /**
     * retries
     */
    int getRetries();

    /**
     * set retries
     */
    void setRetries(int retries);

    /**
     * set rpc protocol version. for compatible different version.
     * this value must set by server end while decode finish.
     * it only used in local, will not send to remote.
     *
     * @param rpcProtocolVersion protocol version. see @RpcProtocolVersion
     */
    void setRpcProtocolVersion(byte rpcProtocolVersion);

    byte getRpcProtocolVersion();

    /**
     * set the serialization number.
     * same to the protocol version, this value only used in server end for compatible.
     *
     * @param number
     */
    void setSerializeNumber(int number);

    int getSerializeNumber();

    /**
     * @param key attachment key
     * @return attachment value or null if key does not exist
     * @since 1.2.3
     */
    default String getAttachment(String key) {
        Map<String, String> attachments = getAttachments();
        if (attachments != null) {
            return attachments.get(key);
        }
        return null;
    }
}
