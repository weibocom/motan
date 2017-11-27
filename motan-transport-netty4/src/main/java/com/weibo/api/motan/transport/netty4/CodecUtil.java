package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.protocol.v2motan.MotanV2Codec;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.util.ByteUtil;
import com.weibo.api.motan.util.LoggerUtil;

import java.io.IOException;

/**
 * @author sunnights
 */
public class CodecUtil {

    public static byte[] encodeObjectToBytes(Channel channel, Codec codec, Object msg) {
        try {
            if (codec instanceof MotanV2Codec) {
                return encodeV2(channel, codec, msg);
            } else {
                return encodeV1(channel, codec, msg);
            }
        } catch (IOException e) {
            throw new MotanFrameworkException("encode error: isResponse=" + (msg instanceof Response), e, MotanErrorMsgConstant.FRAMEWORK_ENCODE_ERROR);
        }
    }

    private static byte[] encodeV2(Channel channel, Codec codec, Object msg) throws IOException {
        return encodeMessage(channel, codec, msg);
    }

    private static byte[] encodeV1(Channel channel, Codec codec, Object msg) throws IOException {
        long requestId = getRequestId(msg);
        byte[] data = encodeMessage(channel, codec, msg);
        byte[] result = new byte[MotanConstants.NETTY_HEADER + data.length];
        ByteUtil.short2bytes(MotanConstants.NETTY_MAGIC_TYPE, result, 0);
        result[3] = getType(msg);
        ByteUtil.long2bytes(requestId, result, 4);
        ByteUtil.int2bytes(data.length, result, 12);
        System.arraycopy(data, 0, result, MotanConstants.NETTY_HEADER, data.length);
        return result;
    }

    private static byte[] encodeMessage(Channel channel, Codec codec, Object msg) throws IOException {
        byte[] data;
        if (msg instanceof Response) {
            try {
                data = codec.encode(channel, msg);
            } catch (Exception e) {
                LoggerUtil.error("NettyEncoder encode error, identity=" + channel.getUrl().getIdentity(), e);
                long requestId = getRequestId(msg);
                Response response = buildExceptionResponse(requestId, e);
                data = codec.encode(channel, response);
            }
        } else {
            data = codec.encode(channel, msg);
        }
        return data;
    }

    private static long getRequestId(Object message) {
        if (message instanceof Request) {
            return ((Request) message).getRequestId();
        } else if (message instanceof Response) {
            return ((Response) message).getRequestId();
        } else {
            return 0;
        }
    }

    private static Response buildExceptionResponse(long requestId, Exception e) {
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(requestId);
        response.setException(e);
        return response;
    }

    private static byte getType(Object message) {
        if (message instanceof Request) {
            return MotanConstants.FLAG_REQUEST;
        } else if (message instanceof Response) {
            return MotanConstants.FLAG_RESPONSE;
        } else {
            return MotanConstants.FLAG_OTHER;
        }
    }
}
