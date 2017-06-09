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

package com.weibo.api.motan.protocol.rpc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.StringUtils;

import com.weibo.api.motan.codec.AbstractCodec;
import com.weibo.api.motan.codec.Serialization;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.support.DefaultRpcHeartbeatFactory;
import com.weibo.api.motan.util.ByteUtil;
import com.weibo.api.motan.util.ConcurrentHashSet;
import com.weibo.api.motan.util.ExceptionUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanDigestUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import com.weibo.api.motan.util.ReflectUtil;

/**
 * 压缩协议codec，支持开启gzip压缩。
 * 
 * @author zhanglei
 *
 */
@SpiMeta(name = "compressMotan")
public class CompressRpcCodec extends AbstractCodec {
    private static final short MAGIC = (short) 0xF0F0;

    private static final byte MASK = 0x07;

    // 保存方法签名与具体方法信息的对应关系，decode request时server端使用
    private static ConcurrentHashMap<String, MethodInfo> SIGN_METHOD_MAP = new ConcurrentHashMap<String, MethodInfo>();
    // 保存方法信息串与签名之间的对应关系。
    private static ConcurrentHashMap<String, String> METHOD_SIGN_MAP = new ConcurrentHashMap<String, String>();

    // 保存方法签名与调用方attachment中application等固定信息的对应关系，decode request时server端使用
    private static ConcurrentHashMap<String, AttachmentInfo> SIGN_ATTACHMENT_MAP = new ConcurrentHashMap<String, AttachmentInfo>();

    private static ConcurrentHashSet<String> ACCEPT_ATTACHMENT_SIGN = new ConcurrentHashSet<String>();// 保存已被server端缓存的attachment信息签名。
                                                                                                      // client使用，如果server端已缓存则不用重复发送
    private static final String SIGN_FLAG = "1";// 使用方法签名标识位。用来实现新旧版本兼容

    // attachment中使用的简化key，都以_开头
    private static final String ATTACHMENT_SIGN = "_A";// 压缩attachment固定参数后签名的key。同时也是server确认已保存的签名key
    private static final String UN_ATTACHMENT_SIGN = "_UA";// server确认尚未保存的签名key
    private static final String CLIENT_REQUESTID = "_RID";// client requestid的简化key

    public static final String CODEC_VERSION_SWITCHER = "feature.motanrpc.codecversion.degrade";// codec降级开关，默认为false，为true时会使用v1非压缩版本。
    public static final String GROUP_CODEC_VERSION_SWITCHER = "feature.motanrpc.codecversion.groupdegrade.";// 按group分组降级codec开关前缀，默认为false，为true时会使用v1非压缩版本。
    private DefaultRpcCodec v1Codec = new DefaultRpcCodec();

    static {
        LoggerUtil.info("init compress codec");
        MotanSwitcherUtil.initSwitcher(CODEC_VERSION_SWITCHER, false);
    }

    @Override
    public byte[] encode(Channel channel, Object message) throws IOException {
        if (needEncodeV1(message)) {
            return v1Codec.encode(channel, message);
        } else {
            // 使用v2压缩版本
            return encodeV2(channel, message);
        }

    }

    // v1降级开关打开、心跳请求、client端使用v1版本时，需要使用v1编码
    private boolean needEncodeV1(Object message) {

        if (MotanSwitcherUtil.isOpen(CODEC_VERSION_SWITCHER)) {
            return true;
        }
        if (message instanceof Request) {
            //  心跳包不压缩
            if (DefaultRpcHeartbeatFactory.isHeartbeatRequest(message)) {
                return true;
            }
            // 检查分组降级开关是否开启
            String group = MotanFrameworkUtil.getGroupFromRequest((Request) message);
            if (MotanSwitcherUtil.switcherIsOpenWithDefault(GROUP_CODEC_VERSION_SWITCHER + group, false)) {
                return true;
            }
        }
        return message instanceof Response && ((Response) message).getRpcProtocolVersion() == RpcProtocolVersion.VERSION_1.getVersion();

    }



    /**
     * decode data
     * 
     * <pre>
	 * 		对于client端：主要是来自server端的response or exception
	 * 		对于server端: 主要是来自client端的request
	 * </pre>
     * 
     * @param data
     * @return
     * @throws IOException
     */
    @Override
    public Object decode(Channel channel, String remoteIp, byte[] data) throws IOException {
        if (MotanSwitcherUtil.isOpen(CODEC_VERSION_SWITCHER)) {
            // 降级开关打开时，使用v1版本codec
            return v1Codec.decode(channel, remoteIp, data);
        } else {
            if (data.length <= 3) {
                throw new MotanFrameworkException("decode error: format problem", MotanErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
            }
            // 只支持v1和v2版本
            if (data[2] == RpcProtocolVersion.VERSION_1.getVersion()) {
                return v1Codec.decode(channel, remoteIp, data);
            } else if (data[2] == RpcProtocolVersion.VERSION_2.getVersion()) {
                // 使用v2压缩版本
                return decodeV2(channel, remoteIp, data);
            } else {
                throw new MotanFrameworkException("decode error: version error. version=" + data[2],
                        MotanErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
            }

        }
    }



    public byte[] encodeV2(Channel channel, Object message) throws IOException {
        try {
            if (message instanceof Request) {
                return encodeRequest(channel, (Request) message);

            } else if (message instanceof Response) {
                return encodeResponse(channel, (Response) message);

            }
        } catch (Exception e) {
            if (ExceptionUtil.isMotanException(e)) {
                throw (RuntimeException) e;
            } else {
                throw new MotanFrameworkException("encode error: isResponse=" + (message instanceof Response), e,
                        MotanErrorMsgConstant.FRAMEWORK_ENCODE_ERROR);
            }
        }

        throw new MotanFrameworkException("encode error: message type not support, " + message.getClass(),
                MotanErrorMsgConstant.FRAMEWORK_ENCODE_ERROR);
    }

    /**
     * decode data
     * 
     * <pre>
     *      对于client端：主要是来自server端的response or exception
     *      对于server端: 主要是来自client端的request
     * </pre>
     * 
     * @param data
     * @return
     * @throws IOException
     */

    public Object decodeV2(Channel channel, String remoteIp, byte[] data) throws IOException {
        if (data.length <= RpcProtocolVersion.VERSION_2.getHeaderLength()) {
            throw new MotanFrameworkException("decode error: format problem", MotanErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
        }

        short type = ByteUtil.bytes2short(data, 0);

        if (type != MAGIC) {
            throw new MotanFrameworkException("decode error: magic error", MotanErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
        }

        int bodyLength = ByteUtil.bytes2int(data, 12);

        if (RpcProtocolVersion.VERSION_2.getHeaderLength() + bodyLength != data.length) {
            throw new MotanFrameworkException("decode error: content length error", MotanErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
        }

        byte flag = data[3];
        byte dataType = (byte) (flag & MASK);
        boolean isResponse = (dataType != MotanConstants.FLAG_REQUEST);

        byte[] body = new byte[bodyLength];

        System.arraycopy(data, RpcProtocolVersion.VERSION_1.getHeaderLength(), body, 0, bodyLength);

        long requestId = ByteUtil.bytes2long(data, 4);
        Serialization serialization =
                ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(
                        channel.getUrl().getParameter(URLParamType.serialize.getName(), URLParamType.serialize.getValue()));

        try {
            if (isResponse) {
                return decodeResponse(body, dataType, requestId, data[2], serialization);
            } else {
                return decodeRequest(body, requestId, remoteIp, serialization);
            }
        } catch (ClassNotFoundException e) {
            throw new MotanFrameworkException("decode " + (isResponse ? "response" : "request") + " error: class not found", e,
                    MotanErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
        } catch (Exception e) {
            if (ExceptionUtil.isMotanException(e)) {
                throw (RuntimeException) e;
            } else {
                throw new MotanFrameworkException("decode error: isResponse=" + isResponse, e, MotanErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
            }
        }
    }


    /**
     * request body 数据：
     * 
     * <pre>
	 * 
	 * 	 body:
	 * 
	 * 	 byte[] data :  
	 * 
	 * 			serialize(interface_name, method_name, method_param_desc, method_param_value, attachments_size, attachments_value) 
	 * 
	 *   method_param_desc:  for_each (string.append(method_param_interface_name))
	 * 
	 *   method_param_value: for_each (method_param_name, method_param_value)
	 * 
	 * 	 attachments_value:  for_each (attachment_name, attachment_value)
	 * 
	 * </pre>
     * 
     * @param request
     * @return
     * @throws IOException
     */
    private byte[] encodeRequest(Channel channel, Request request) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput output = createOutput(outputStream);
        addMethodInfo(output, request);


        Serialization serialization =
                ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(
                        channel.getUrl().getParameter(URLParamType.serialize.getName(), URLParamType.serialize.getValue()));

        if (request.getArguments() != null && request.getArguments().length > 0) {
            for (Object obj : request.getArguments()) {
                serialize(output, obj, serialization);
            }
        }

        if (request.getAttachments() == null || request.getAttachments().isEmpty()) {
            // empty attachments
            output.writeShort(0);
        } else {
            // 需要copy一份attachment进行签名替换，这样在失败重试时原始的request信息不会变更
            Map<String, String> attachments = copyMap(request.getAttachments());
            replaceAttachmentParamsBySign(channel, attachments);

            addAttachment(output, attachments);
        }

        output.flush();
        byte[] body = outputStream.toByteArray();

        byte flag = MotanConstants.FLAG_REQUEST;

        output.close();
        Boolean usegz = channel.getUrl().getBooleanParameter(URLParamType.usegz.getName(), URLParamType.usegz.getBooleanValue());
        int minGzSize = channel.getUrl().getIntParameter(URLParamType.mingzSize.getName(), URLParamType.mingzSize.getIntValue());
        return encode(compress(body, usegz, minGzSize), flag, request.getRequestId());
    }

    private Map<String, String> copyMap(Map<String, String> attachments) {
        Map<String, String> resultMap = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : attachments.entrySet()) {
            resultMap.put(entry.getKey(), entry.getValue());
        }
        return resultMap;
    }

    /**
     * 添加方法完整信息或方法签名。
     * 
     * @param output
     * @param request
     * @throws IOException
     */
    private void addMethodInfo(ObjectOutput output, Request request) throws IOException {
        String methodInfoStr = MotanFrameworkUtil.getServiceKey(request) + request.getMethodName() + request.getParamtersDesc();
        String methodSign = METHOD_SIGN_MAP.get(methodInfoStr);
        if (methodSign == null) {
            MethodInfo temp =
                    new MethodInfo(MotanFrameworkUtil.getGroupFromRequest(request), request.getInterfaceName(), request.getMethodName(),
                            request.getParamtersDesc(), MotanFrameworkUtil.getVersionFromRequest(request));
            try {
                methodSign = temp.getSign();
                METHOD_SIGN_MAP.putIfAbsent(methodInfoStr, methodSign);
                LoggerUtil.info("add method sign:" + methodSign + ", methodinfo:" + temp.toString());
            } catch (Exception e) {
                LoggerUtil.warn("gen method sign fail!" + e.getMessage());
            }

        }
        if (methodSign != null) {
            output.writeUTF(SIGN_FLAG);// 使用方法签名
            output.writeUTF(methodSign);
        } else {// 如果获取签名失败就使用非压缩方式。
            output.writeUTF(request.getInterfaceName());
            output.writeUTF(request.getMethodName());
            output.writeUTF(request.getParamtersDesc());
        }

    }

    /**
     * 用签名替换Attachment中每次必传的固定参数。
     * 
     * @param attachments
     */
    private void replaceAttachmentParamsBySign(Channel channel, Map<String, String> attachments) {
        // attachment中的固定参数使用签名方式传递。首次跟server建立链接时传全部信息，之后只传签名。
        AttachmentInfo info = getAttachmentInfoMap(attachments);
        if (info != null) {
            String sign = info.getAttachmetnSign();
            if (sign != null) {
                attachments.put(ATTACHMENT_SIGN, sign);

                if (ACCEPT_ATTACHMENT_SIGN.contains(sign)) {// server端已经缓存签名时，不需要传递application等固定信息。
                    removeAttachmentInfoMap(attachments);
                }
            }
        }
        // 如果没有client的requestid，则不传递此参数，否则使用简化key传递
        // 为保证不同版本兼容性，只在codec中进行处理。
        String clientRequestid = attachments.get(URLParamType.requestIdFromClient.getName());
        if (clientRequestid != null && !URLParamType.requestIdFromClient.getValue().equals(clientRequestid)) {
            attachments.put(CLIENT_REQUESTID, clientRequestid);
        }
        attachments.remove(URLParamType.requestIdFromClient.getName());
    }

    private void addAttachment(ObjectOutput output, Map<String, String> attachments) throws IOException {
        output.writeShort(attachments.size());
        for (Map.Entry<String, String> entry : attachments.entrySet()) {
            output.writeUTF(entry.getKey());
            output.writeUTF(entry.getValue());
        }
    }

    /**
     * response body 数据：
     * 
     * <pre>
	 * 
	 * body:
	 * 
	 * 	 byte[] :  serialize (result) or serialize (exception)
	 * 
	 * </pre>
     *
     * @param channel
     * @param value
     * @return
     * @throws IOException
     */
    private byte[] encodeResponse(Channel channel, Response value) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput output = createOutput(outputStream);
        Serialization serialization =
                ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(
                        channel.getUrl().getParameter(URLParamType.serialize.getName(), URLParamType.serialize.getValue()));

        byte flag = 0;

        output.writeLong(value.getProcessTime());

        if (value.getException() != null) {
            output.writeUTF(value.getException().getClass().getName());
            serialize(output, value.getException(), serialization);
            flag = MotanConstants.FLAG_RESPONSE_EXCEPTION;
        } else if (value.getValue() == null) {
            flag = MotanConstants.FLAG_RESPONSE_VOID;
        } else {
            output.writeUTF(value.getValue().getClass().getName());
            serialize(output, value.getValue(), serialization);
            // v2版本可以在response中添加attachment
            Map<String, String> attachments = value.getAttachments();
            if (attachments != null) {
                String signed = attachments.get(ATTACHMENT_SIGN);
                String unSigned = attachments.get(UN_ATTACHMENT_SIGN);
                attachments.clear(); // 除了attachment签名外不返回其他信息。

                if (StringUtils.isNotBlank(signed)) {
                    attachments.put(ATTACHMENT_SIGN, signed);
                }
                if (StringUtils.isNotBlank(unSigned)) {
                    attachments.put(UN_ATTACHMENT_SIGN, unSigned);
                }
            }
            if (attachments != null && !attachments.isEmpty()) {// 需要回传附加数据
                addAttachment(output, attachments);
            } else {
                // empty attachments
                output.writeShort(0);
            }
            flag = MotanConstants.FLAG_RESPONSE_ATTACHMENT; // v2版本flag
        }

        output.flush();

        byte[] body = outputStream.toByteArray();

        output.close();
        Boolean usegz = channel.getUrl().getBooleanParameter(URLParamType.usegz.getName(), URLParamType.usegz.getBooleanValue());
        int minGzSize = channel.getUrl().getIntParameter(URLParamType.mingzSize.getName(), URLParamType.mingzSize.getIntValue());
        return encode(compress(body, usegz, minGzSize), flag, value.getRequestId());
    }

    /**
     * 数据协议：
     * 
     * <pre>
	 * 
	 * header:  16个字节 
	 * 
	 * 0-15 bit 	:  magic
	 * 16-23 bit	:  version
	 * 24-31 bit	:  extend flag , 其中： 29-30 bit: event 可支持4种event，比如normal, exception等,  31 bit : 0 is request , 1 is response 
	 * 32-95 bit 	:  request id
	 * 96-127 bit 	:  body content length
	 * 
	 * </pre>
     *
     * @param body
     * @param flag
     * @param requestId
     * @return
     * @throws IOException
     */
    private byte[] encode(byte[] body, byte flag, long requestId) throws IOException {
        byte[] header = new byte[RpcProtocolVersion.VERSION_2.getHeaderLength()];
        int offset = 0;

        // 0 - 15 bit : magic
        ByteUtil.short2bytes(MAGIC, header, offset);
        offset += 2;

        // 16 - 23 bit : version
        header[offset++] = RpcProtocolVersion.VERSION_2.getVersion();

        // 24 - 31 bit : extend flag
        header[offset++] = flag;

        // 32 - 95 bit : requestId
        ByteUtil.long2bytes(requestId, header, offset);
        offset += 8;

        // 96 - 127 bit : body content length
        ByteUtil.int2bytes(body.length, header, offset);

        byte[] data = new byte[header.length + body.length];

        System.arraycopy(header, 0, data, 0, header.length);
        System.arraycopy(body, 0, data, header.length, body.length);

        return data;
    }

    private Object decodeRequest(byte[] body, long requestId, String remoteIp, Serialization serialization) throws IOException,
            ClassNotFoundException {

        ObjectInput input = createInput(getInputStream(body));
        String interfaceName = null;
        String methodName = null;
        String paramtersDesc = null;
        String group = null;
        String version = null;

        String flag = input.readUTF();

        if (SIGN_FLAG.equals(flag)) {// 方法签名方式
            String sign = input.readUTF();
            MethodInfo mInfo = SIGN_METHOD_MAP.get(sign);
            if (mInfo == null) {
                throw new MotanFrameworkException("decode error: invalid method sign: " + sign,
                        MotanErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
            }
            interfaceName = mInfo.getInterfaceName();
            methodName = mInfo.getMethodName();
            paramtersDesc = mInfo.getParamtersDesc();
            group = mInfo.getGroup();
            version = mInfo.getVersion();
        } else {
            interfaceName = flag;
            methodName = input.readUTF();
            paramtersDesc = input.readUTF();
        }


        DefaultRequest rpcRequest = new DefaultRequest();
        rpcRequest.setRequestId(requestId);
        rpcRequest.setInterfaceName(interfaceName);
        rpcRequest.setMethodName(methodName);
        rpcRequest.setParamtersDesc(paramtersDesc);
        rpcRequest.setArguments(decodeRequestParameter(input, paramtersDesc, serialization));
        rpcRequest.setAttachments(decodeRequestAttachments(input));
        rpcRequest.setRpcProtocolVersion(RpcProtocolVersion.VERSION_2.getVersion());

        input.close();
        Map<String, String> attachments = rpcRequest.getAttachments();
        putSignedAttachment(attachments, remoteIp);// 根据签名添加client固定参数。
        if (attachments.get(URLParamType.group.name()) == null) {
            // 如果attachment sign失效时，需要使用methodsign中的group信息。
            attachments.put(URLParamType.group.name(), group);
            attachments.put(URLParamType.version.name(), version);
        }

        return rpcRequest;
    }



    private void putSignedAttachment(Map<String, String> attachments, String remoteIp) {
        if (attachments != null && !attachments.isEmpty()) {
            AttachmentInfo info = getAttachmentInfoMap(attachments);
            if (info != null) {// 如果client端传递了application等固定参数，则更新对应缓存。并标记已接收缓存。
                String sign = attachments.get(ATTACHMENT_SIGN);
                if (StringUtils.isNotBlank(sign)) {
                    SIGN_ATTACHMENT_MAP.put(remoteIp + sign, info);
                    LoggerUtil.info("update attachment sign:" + remoteIp + sign + ", info-group:" + info.getGroup());
                }

            } else {// 使用签名
                String sign = attachments.get(ATTACHMENT_SIGN);
                if (StringUtils.isNotBlank(sign)) {
                    info = SIGN_ATTACHMENT_MAP.get(remoteIp + sign);
                    if (info != null) {
                        // 把缓存中的信息添加到request的attachment中。
                        putAttachmentInfoMap(info, attachments);
                    } else {// server端没有缓存sign对应的info，需要通知client重新发送info
                        attachments.put(UN_ATTACHMENT_SIGN, sign);
                        LoggerUtil.info("miss attachment sign:" + remoteIp + sign);
                    }
                    // 返回repsponse时ATTACHMENT_SIGN表示server端已缓存成功，
                    // 如果client没有传递attachmentinfo，则没有必要回应。
                    attachments.remove(ATTACHMENT_SIGN);
                } else {
                    LoggerUtil.warn("attachment sign is blank，application info miss!");
                }
            }

            // 还原client requestid
            String clientRequestid = URLParamType.requestIdFromClient.getValue();// 默认值
            if (attachments.containsKey(CLIENT_REQUESTID)) {
                clientRequestid = attachments.get(CLIENT_REQUESTID);
            }
            attachments.put(URLParamType.requestIdFromClient.getName(), clientRequestid);
        }
    }

    private Object[] decodeRequestParameter(ObjectInput input, String parameterDesc, Serialization serialization) throws IOException,
            ClassNotFoundException {
        if (parameterDesc == null || parameterDesc.equals("")) {
            return null;
        }

        Class<?>[] classTypes = ReflectUtil.forNames(parameterDesc);

        Object[] paramObjs = new Object[classTypes.length];

        for (int i = 0; i < classTypes.length; i++) {
            paramObjs[i] = deserialize((byte[]) input.readObject(), classTypes[i], serialization);
        }

        return paramObjs;
    }

    private Map<String, String> decodeRequestAttachments(ObjectInput input) throws IOException, ClassNotFoundException {
        int size = input.readShort();

        if (size <= 0) {
            return null;
        }

        Map<String, String> attachments = new HashMap<String, String>();

        for (int i = 0; i < size; i++) {
            attachments.put(input.readUTF(), input.readUTF());
        }

        return attachments;
    }

    /**
     * 
     * @param body
     * @param dataType
     * @param requestId
     * @param rpcProtocolVersion rpc协议的版本号，不同版本可能有不同的序列化方式
     * @param serialization
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private Object decodeResponse(byte[] body, byte dataType, long requestId, byte rpcProtocolVersion, Serialization serialization)
            throws IOException, ClassNotFoundException {


        ObjectInput input = createInput(getInputStream(body));

        long processTime = input.readLong();

        DefaultResponse response = new DefaultResponse();
        response.setRequestId(requestId);
        response.setProcessTime(processTime);

        if (dataType == MotanConstants.FLAG_RESPONSE_VOID) {
            return response;
        }

        String className = input.readUTF();
        Class<?> clz = ReflectUtil.forName(className);

        Object result = deserialize((byte[]) input.readObject(), clz, serialization);

        if (dataType == MotanConstants.FLAG_RESPONSE) {
            response.setValue(result);
        } else if (dataType == MotanConstants.FLAG_RESPONSE_ATTACHMENT) {
            response.setValue(result);
            Map<String, String> attachment = decodeRequestAttachments(input);
            checkAttachment(attachment);
        } else if (dataType == MotanConstants.FLAG_RESPONSE_EXCEPTION) {
            response.setException((Exception) result);
        } else {
            throw new MotanFrameworkException("decode error: response dataType not support " + dataType,
                    MotanErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
        }

        response.setRequestId(requestId);

        input.close();

        return response;
    }

    // 检查repsonse中是否有server端已缓存的sign或server尚未缓存的sign。
    private void checkAttachment(Map<String, String> attachment) {
        if (attachment != null && !attachment.isEmpty()) {
            String acceptSign = attachment.get(ATTACHMENT_SIGN);
            if (StringUtils.isNotBlank(acceptSign)) {// attachment
                                                     // sign已被server端缓存，则后续请求不用在传递固定的attachment
                ACCEPT_ATTACHMENT_SIGN.add(acceptSign);
            }
            String notAcceptSign = attachment.get(UN_ATTACHMENT_SIGN);
            if (StringUtils.isNotBlank(notAcceptSign)) {// 如果server端没有缓存sign对应的attachment信息，则下次请求时需要传递
                ACCEPT_ATTACHMENT_SIGN.remove(notAcceptSign);
            }
        }
    }

    // 从request的attachments中获取AttachmentInfo，没有时返回null
    private AttachmentInfo getAttachmentInfoMap(Map<String, String> attachments) {
        AttachmentInfo result = null;
        if (attachments != null && attachments.containsKey(URLParamType.application.name())) {
            String group = attachments.get(URLParamType.group.name());
            String application = attachments.get(URLParamType.application.name());
            String module = attachments.get(URLParamType.module.name());
            String version = attachments.get(URLParamType.version.name());
            result = new AttachmentInfo(group, application, module, version);
        }
        return result;
    }

    private void putAttachmentInfoMap(AttachmentInfo attachmentInfo, Map<String, String> attachments) {
        if (attachments != null) {
            attachments.put(URLParamType.group.name(), attachmentInfo.getGroup());
            attachments.put(URLParamType.application.name(), attachmentInfo.getApplication());
            attachments.put(URLParamType.module.name(), attachmentInfo.getModule());
            attachments.put(URLParamType.version.name(), attachmentInfo.getVersion());
        }
    }

    private void removeAttachmentInfoMap(Map<String, String> attachments) {
        if (attachments != null) {
            attachments.remove(URLParamType.group.name());
            attachments.remove(URLParamType.application.name());
            attachments.remove(URLParamType.module.name());
            attachments.remove(URLParamType.version.name());
        }
    }

    /**
     * 获取输入流。兼容gzip
     *
     * @param data
     * @return
     */
    public static InputStream getInputStream(byte[] data) {
        if (isGzipData(data)) {// 判断 gzip魔数
            try {
                return new GZIPInputStream(new ByteArrayInputStream(data));
            } catch (Exception ignore) {}
        }
        return  new ByteArrayInputStream(data);
    }

    // 简单判断是否为gzip压缩数据。
    // 判断方法为检验gzip魔数
    private static boolean isGzipData(byte[] data) {
        if (data.length > 2) {
            int header = (int) (((data[0] & 0xff)) | (data[1] & 0xff) << 8);
            if (GZIPInputStream.GZIP_MAGIC == header) {
                return true;
            }
        }
        return false;
    }

    // 对rpc body进行压缩。
    public byte[] compress(byte[] org, boolean useGzip, int minGzSize) throws IOException {
        if (useGzip && org.length > minGzSize) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            GZIPOutputStream gos = new GZIPOutputStream(outputStream);
            gos.write(org);
            gos.finish();
            gos.flush();
            gos.close();
            byte[] ret = outputStream.toByteArray();
            return ret;
        } else {
            return org;
        }

    }

    public static void putMethodSign(Provider<?> provider, List<Method> methods) {
        String group = provider.getUrl().getGroup();
        String interfaceName = provider.getInterface().getName();
        String version = provider.getUrl().getVersion();
        for (Method method : methods) {
            MethodInfo temp = new MethodInfo(group, interfaceName, method.getName(), ReflectUtil.getMethodParamDesc(method), version);
            String sign = temp.getSign();
            MethodInfo priInfo = SIGN_METHOD_MAP.putIfAbsent(sign, temp);
            if (priInfo != null && !temp.equals(priInfo)) {// 方法签名冲突
                throw new MotanFrameworkException("add method sign conflict! " + temp.toString() + " with " + priInfo.toString(),
                        MotanErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
            } else {
                LoggerUtil.info("add method sign:" + sign + ", methodinfo:" + temp.toString());
            }

        }
    }

    public static void putMethodSign(String methodSign, MethodInfo methodInfo) {
        SIGN_METHOD_MAP.putIfAbsent(methodSign, methodInfo);
    }

    static class MethodInfo {
        String group;
        String interfaceName;
        String methodName;
        String paramtersDesc;
        String version;



        public MethodInfo(String group, String interfaceName, String methodName, String paramtersDesc, String version) {
            super();
            this.group = group;
            this.interfaceName = interfaceName;
            this.methodName = methodName;
            this.paramtersDesc = paramtersDesc;
            this.version = version;
        }

        /**
         * 根据方法信息生成对应的签名。 此方法会抛出异常，调用时根据使用情况决定是否处理异常。
         * 
         * @return
         * @throws Exception
         */
        public String getSign() {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(group).append(interfaceName).append(methodName).append(paramtersDesc).append(version);
                String surfix = MotanDigestUtil.md5LowerCase(sb.toString()).substring(8, 20); // 取32位md5的8-20位。
                int endIndex = methodName.length() > 4 ? 4 : methodName.length();
                String prefix = methodName.substring(0, endIndex);
                return prefix + surfix;
            } catch (Exception e) {
                throw new MotanFrameworkException("gen method sign error! " + this.toString(), MotanErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
            }

        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getInterfaceName() {
            return interfaceName;
        }

        public void setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public String getParamtersDesc() {
            return paramtersDesc;
        }

        public void setParamtersDesc(String paramtersDesc) {
            this.paramtersDesc = paramtersDesc;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((group == null) ? 0 : group.hashCode());
            result = prime * result + ((interfaceName == null) ? 0 : interfaceName.hashCode());
            result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
            result = prime * result + ((paramtersDesc == null) ? 0 : paramtersDesc.hashCode());
            result = prime * result + ((version == null) ? 0 : version.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            MethodInfo other = (MethodInfo) obj;
            if (group == null) {
                if (other.group != null) return false;
            } else if (!group.equals(other.group)) return false;
            if (interfaceName == null) {
                if (other.interfaceName != null) return false;
            } else if (!interfaceName.equals(other.interfaceName)) return false;
            if (methodName == null) {
                if (other.methodName != null) return false;
            } else if (!methodName.equals(other.methodName)) return false;
            if (paramtersDesc == null) {
                if (other.paramtersDesc != null) return false;
            } else if (!paramtersDesc.equals(other.paramtersDesc)) return false;
            if (version == null) {
                if (other.version != null) return false;
            } else if (!version.equals(other.version)) return false;
            return true;
        }

        @Override
        public String toString() {
            return "MethodInfo [group=" + group + ", interfaceName=" + interfaceName + ", methodName=" + methodName + ", paramtersDesc="
                    + paramtersDesc + ", version=" + version + "]";
        }

    }

    static class AttachmentInfo {
        String group;
        String application;
        String module;
        String version;

        public AttachmentInfo(String group, String application, String module, String version) {
            super();
            this.group = group;
            this.application = application;
            this.module = module;
            this.version = version;
        }

        public String getAttachmetnSign() {
            String signstr = group + application + module + version;
            String hashcodeStr = null;
            try {
                hashcodeStr = MotanDigestUtil.md5LowerCase(signstr).substring(8, 12); // 取md5中的四个字符。
            } catch (Exception e) {
                LoggerUtil.warn("getAttachmetnSign fail!" + e.getMessage());
            }
            return hashcodeStr;
        }

        public String getGroup() {
            return group;
        }

        public String getApplication() {
            return application;
        }

        public String getModule() {
            return module;
        }

        public String getVersion() {
            return version;
        }


    }

}
