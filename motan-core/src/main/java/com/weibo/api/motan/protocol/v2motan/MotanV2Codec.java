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
import com.weibo.api.motan.util.ExceptionUtil;
import com.weibo.api.motan.util.MathUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author maijunsheng
 * @version 创建时间：2013-5-22
 */
@SpiMeta(name = "motan2")
public class MotanV2Codec extends AbstractCodec {

    private static final String defaultV2Serialization = "grpc-pb";
    private static final byte MASK = 0x07;
    private static final int HEADER_SIZE = 13;


    static {
        initAllSerialziation();
    }

    @Override
    public byte[] encode(Channel channel, Object message) throws IOException {
        try {
            MotanV2Header header = new MotanV2Header();
            byte[] body = null;
            String serialName = channel.getUrl().getParameter(URLParamType.serialize.getName(), defaultV2Serialization);
            Serialization serialization = ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(serialName);
            if (serialization == null) {
                throw new MotanServiceException("can not found serialization " + serialName);
            }
            header.setSerialize(serialization.getSerializationNumber());

            ByteBuffer buf = ByteBuffer.allocate(4096);
            //meta
            int index = HEADER_SIZE;
            buf.position(index);
            buf.putInt(0);//metasize

            if (message instanceof Request) {
                Request request = (Request) message;
                putString(buf, "M_p");
                putString(buf, request.getInterfaceName());
                putString(buf, "M_m");
                putString(buf, request.getMethodName());
                if(request.getParamtersDesc() != null){
                    putString(buf, "M_md");
                    putString(buf, request.getParamtersDesc());
                }
                if(request.getAttachments() != null && request.getAttachments().get(URLParamType.group.getName()) != null){
                    request.setAttachment("M_g", request.getAttachments().get(URLParamType.group.getName()));
                }

                putMap(buf, request.getAttachments());

                header.setRequestId(request.getRequestId());
                body = serialization.serialize(request.getArguments().length == 1 ? request.getArguments()[0] : request.getArguments());

            } else if (message instanceof Response) {
                Response response = (Response) message;
                putString(buf, "M_pt");
                putString(buf, String.valueOf(response.getProcessTime()));
                if (response.getException() != null) {
                    putString(buf, "M_e");
                    putString(buf, ExceptionUtil.toMessage(response.getException()));
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
                buf.putInt(body.length);
                buf.put(body);
            } else {
                buf.putInt(0);
            }


            //header
            //TODO gzip etc.
            int position = buf.position();
            buf.position(0);
            buf.put(header.toBytes());
            buf.position(position);
            buf.flip();
            return buf.array();
        } catch (Exception e) {
            if (ExceptionUtil.isMotanException(e)) {
                throw (RuntimeException) e;
            } else {
                throw new MotanFrameworkException("encode error: isResponse=" + (message instanceof Response), e,
                        MotanErrorMsgConstant.FRAMEWORK_ENCODE_ERROR);
            }
        }
    }


    private void putString(ByteBuffer buf, String content) throws UnsupportedEncodingException {
        buf.put(content.getBytes("UTF-8"));
        buf.put("\n".getBytes("UTF-8"));
    }

    private void putMap(ByteBuffer buf, Map<String, String> map) throws UnsupportedEncodingException {
        if (!map.isEmpty()) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                putString(buf, entry.getKey());
                putString(buf, entry.getValue());
            }
        }
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
        Map<String, String> metaMap = null;
        ByteBuffer buf = ByteBuffer.wrap(data);
        int metaSize = buf.getInt(HEADER_SIZE);
        int index = HEADER_SIZE + 4;
        if (metaSize > 0) {
            byte[] meta = new byte[metaSize];
            buf.position(index);
            buf.get(meta);
            metaMap = deocdeMeta(meta);
            index += metaSize;
        }
        int bodySize = buf.getInt(index);
        index += 4;
        Object obj = null;
        if (bodySize > 0) {
            byte[] body = new byte[bodySize];
            buf.position(index);
            buf.get(body);
            //TODO 优化序列化优先级
            //默认自适应序列化
            Serialization serialization = getSerializaiontByNum(header.getSerialize());
            obj = new DeserializableObject(serialization, body);
        }
        if (header.isRequest()) {
            DefaultRequest request = new DefaultRequest();
            request.setRequestId(header.getRequestId());
            request.setInterfaceName(metaMap.remove("M_p"));
            request.setMethodName(metaMap.remove("M_m"));
            request.setParamtersDesc(metaMap.remove("M_md"));
            request.setAttachments(metaMap);
            request.setArguments(new Object[]{obj});
            if(metaMap.get("M_g") != null){
                request.setAttachment(URLParamType.group.getName(), metaMap.get("M_g"));
            }

            if(StringUtils.isNotBlank(metaMap.get("M_v"))){
                request.setAttachment(URLParamType.version.getName(), metaMap.get("M_v"));
            }

            return request;
        } else {
            DefaultResponse response = new DefaultResponse();
            response.setRequestId(header.getRequestId());
            response.setProcessTime(MathUtil.parseLong(metaMap.remove("M_pt"), 0));
            response.setAttachments(metaMap);
            if (header.getStatus() == 0) {//只解析正常消息
                response.setValue(obj);
            } else {
                String errmsg = metaMap.remove("M_e");
                Exception e = ExceptionUtil.fromMessage(errmsg);
                if (e == null) {
                    e = (Exception) new MotanServiceException("default remote exception. remote errmsg:" + errmsg);
                }
                response.setException(e);
            }
            return response;
        }

    }

    private Map<String, String> deocdeMeta(byte[] meta) {
        Map<String, String> map = new HashMap<String, String>();
        if(meta != null && meta.length > 0){
            String[] s = new String(meta).split("\n");
            for (int i = 0; i < s.length - 1; i++) {
                map.put(s[i++], s[i]);
            }
        }
        return map;
    }
}
