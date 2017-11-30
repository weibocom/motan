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

package com.weibo.api.motan.protocol.v2motan;

import com.weibo.api.motan.codec.AbstractCodec;
import com.weibo.api.motan.codec.Serialization;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.serialize.DeserializableObject;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.support.DefaultRpcHeartbeatFactory;
import com.weibo.api.motan.util.ByteUtil;
import com.weibo.api.motan.util.ExceptionUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MathUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.weibo.api.motan.common.MotanConstants.*;


@SpiMeta(name = "motan2")
public class MotanV2Codec extends AbstractCodec {

    private static final byte MASK = 0x07;
    private static final int HEADER_SIZE = 13;


    static {
        initAllSerialziation();
    }

    @Override
    public byte[] encode(Channel channel, Object message) throws IOException {
        try {
            if (DefaultRpcHeartbeatFactory.isHeartbeatRequest(message)) {
                return encodeHeartbeat(((Request) message).getRequestId(), true);
            }
            if (DefaultRpcHeartbeatFactory.isHeartbeatResponse(message)) {
                return encodeHeartbeat(((Response) message).getRequestId(), false);
            }
            MotanV2Header header = new MotanV2Header();
            byte[] body = null;
            String serialName = channel.getUrl().getParameter(URLParamType.serialize.getName(), URLParamType.serialize.getValue());
            Serialization serialization = ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(serialName);
            if (serialization == null) {
                throw new MotanServiceException("can not found serialization " + serialName);
            }
            header.setSerialize(serialization.getSerializationNumber());

            GrowableByteBuffer buf = new GrowableByteBuffer(4096);
            //meta
            int index = HEADER_SIZE;
            buf.position(index);
            buf.putInt(0);//metasize

            if (message instanceof Request) {
                Request request = (Request) message;
                putString(buf, M2_PATH);
                putString(buf, request.getInterfaceName());
                putString(buf, M2_METHOD);
                putString(buf, request.getMethodName());
                if (request.getParamtersDesc() != null) {
                    putString(buf, M2_METHOD_DESC);
                    putString(buf, request.getParamtersDesc());
                }
                if (request.getAttachments() != null && request.getAttachments().get(URLParamType.group.getName()) != null) {
                    request.setAttachment(M2_GROUP, request.getAttachments().get(URLParamType.group.getName()));
                }

                putMap(buf, request.getAttachments());

                header.setRequestId(request.getRequestId());
                if (request.getArguments() != null) {
                    body = serialization.serializeMulti(request.getArguments());
                }

            } else if (message instanceof Response) {
                Response response = (Response) message;
                putString(buf, M2_PROCESS_TIME);
                putString(buf, String.valueOf(response.getProcessTime()));
                if (response.getException() != null) {
                    putString(buf, M2_ERROR);
                    putString(buf, ExceptionUtil.toMessage(response.getException()));
                    header.setStatus(MotanV2Header.MessageStatus.EXCEPTION.getStatus());
                }
                putMap(buf, response.getAttachments());

                header.setRequestId(response.getRequestId());
                header.setRequest(false);
                if (response.getException() == null) {
                    body = serialization.serialize(response.getValue());
                }
            }

            buf.position(buf.position() - 1);
            int metalength = buf.position() - index - 4;
            buf.putInt(index, metalength);

            //body
            if (body != null && body.length > 0) {
                if (channel.getUrl().getBooleanParameter(URLParamType.usegz.getName(), URLParamType.usegz.getBooleanValue())
                        && body.length > channel.getUrl().getIntParameter(URLParamType.mingzSize.getName(), URLParamType.mingzSize.getIntValue())) {
                    try {
                        body = ByteUtil.gzip(body);
                        header.setGzip(true);
                    } catch (IOException e) {
                        LoggerUtil.warn("MotanV2Codec encode gzip fail. so not gzip body.", e);
                    }
                }
                buf.putInt(body.length);
                buf.put(body);
            } else {
                buf.putInt(0);
            }

            //header
            int position = buf.position();
            buf.position(0);
            buf.put(header.toBytes());
            buf.position(position);
            buf.flip();
            byte[] result = new byte[buf.remaining()];
            buf.get(result);
            return result;
        } catch (Exception e) {
            String errmsg = "";
            if (message != null) {
                if (message instanceof Request) {
                    errmsg = "type:request, " + message.toString();
                } else {
                    errmsg = "type:response, " + message.toString();
                }
            }
            LoggerUtil.warn("motan2 encode error." + errmsg, e);
            if (ExceptionUtil.isMotanException(e)) {
                throw (RuntimeException) e;
            } else {
                throw new MotanFrameworkException("encode error!" + errmsg + ", origin errmsg:" + e.getMessage(), e,
                        MotanErrorMsgConstant.FRAMEWORK_ENCODE_ERROR);
            }
        }
    }


    private void putString(GrowableByteBuffer buf, String content) throws UnsupportedEncodingException {
        buf.put(content.getBytes("UTF-8"));
        buf.put("\n".getBytes("UTF-8"));
    }

    private void putMap(GrowableByteBuffer buf, Map<String, String> map) throws UnsupportedEncodingException {
        if (!map.isEmpty()) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                putString(buf, entry.getKey());
                putString(buf, entry.getValue());
            }
        }
    }

    private byte[] encodeHeartbeat(long requestId, boolean isRequest) {
        MotanV2Header header = new MotanV2Header();
        header.setHeartbeat(true);
        header.setRequestId(requestId);
        if (!isRequest) {
            header.setRequest(false);
        }
        GrowableByteBuffer buf = new GrowableByteBuffer(32);
        buf.put(header.toBytes());
        buf.putInt(0);//metasize
        buf.putInt(0);//bodysize
        buf.flip();
        byte[] result = new byte[buf.remaining()];
        buf.get(result);
        return result;
    }


    /**
     * decode data
     *
     * @return
     * @throws IOException
     */
    @Override
    public Object decode(Channel channel, String remoteIp, byte[] data) throws IOException {
        MotanV2Header header = MotanV2Header.buildHeader(data);
        Map<String, String> metaMap = new HashMap<String, String>();
        ByteBuffer buf = ByteBuffer.wrap(data);
        int metaSize = buf.getInt(HEADER_SIZE);
        int index = HEADER_SIZE + 4;
        if (metaSize > 0) {
            byte[] meta = new byte[metaSize];
            buf.position(index);
            buf.get(meta);
            metaMap = decodeMeta(meta);
            index += metaSize;
        }
        int bodySize = buf.getInt(index);
        index += 4;
        Object obj = null;
        if (bodySize > 0) {
            byte[] body = new byte[bodySize];
            buf.position(index);
            buf.get(body);
            if (header.isGzip()) {
                body = ByteUtil.unGzip(body);
            }
            //默认自适应序列化
            Serialization serialization = getSerializaiontByNum(header.getSerialize());
            obj = new DeserializableObject(serialization, body);
        }
        if (header.isRequest()) {
            if (header.isHeartbeat()) {
                return DefaultRpcHeartbeatFactory.getDefaultHeartbeatRequest(header.getRequestId());
            } else {
                DefaultRequest request = new DefaultRequest();
                request.setRequestId(header.getRequestId());
                request.setInterfaceName(metaMap.remove(M2_PATH));
                request.setMethodName(metaMap.remove(M2_METHOD));
                request.setParamtersDesc(metaMap.remove(M2_METHOD_DESC));
                request.setAttachments(metaMap);
                if (obj != null) {
                    request.setArguments(new Object[]{obj});
                }
                if (metaMap.get(M2_GROUP) != null) {
                    request.setAttachment(URLParamType.group.getName(), metaMap.get(M2_GROUP));
                }

                if (StringUtils.isNotBlank(metaMap.get(M2_VERSION))) {
                    request.setAttachment(URLParamType.version.getName(), metaMap.get(M2_VERSION));
                }

                return request;
            }

        } else {
            if (header.isHeartbeat()) {
                return DefaultRpcHeartbeatFactory.getDefaultHeartbeatResponse(header.getRequestId());
            }
            DefaultResponse response = new DefaultResponse();
            response.setRequestId(header.getRequestId());
            response.setProcessTime(MathUtil.parseLong(metaMap.remove(M2_PROCESS_TIME), 0));
            response.setAttachments(metaMap);
            if (header.getStatus() == MotanV2Header.MessageStatus.NORMAL.getStatus()) {//只解析正常消息
                response.setValue(obj);
            } else {
                String errmsg = metaMap.remove(M2_ERROR);
                Exception e = ExceptionUtil.fromMessage(errmsg);
                if (e == null) {
                    e = (Exception) new MotanServiceException("default remote exception. remote errmsg:" + errmsg);
                }
                response.setException(e);
            }
            return response;
        }

    }

    private Map<String, String> decodeMeta(byte[] meta) {
        Map<String, String> map = new HashMap<String, String>();
        if (meta != null && meta.length > 0) {
            String[] s = new String(meta).split("\n");
            for (int i = 0; i < s.length - 1; i++) {
                map.put(s[i++], s[i]);
            }
        }
        return map;
    }
}
