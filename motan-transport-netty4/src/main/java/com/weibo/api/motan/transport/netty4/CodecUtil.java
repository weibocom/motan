package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.protocol.rpc.DefaultRpcCodec;
import com.weibo.api.motan.protocol.v2motan.MotanV2Header;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.util.ByteUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;

import java.io.IOException;

/**
 * @author sunnights
 */
@SuppressWarnings("all")
public class CodecUtil {
    public static byte[] encodeObjectToBytes(Channel channel, Codec codec, Object msg) {
        try {
            byte[] data = encodeMessage(channel, codec, msg);
            short type = ByteUtil.bytes2short(data, 0);
            if (type == DefaultRpcCodec.MAGIC) {
                return encodeV1(msg, data);
            } else if (type == MotanV2Header.MAGIC) {
                return data;
            } else {
                throw new MotanFrameworkException("can not encode message, unknown magic:" + type);
            }
        } catch (IOException e) {
            throw new MotanFrameworkException("encode error: isResponse=" + (msg instanceof Response), e, MotanErrorMsgConstant.FRAMEWORK_ENCODE_ERROR);
        }
    }

    private static byte[] encodeV1(Object msg, byte[] data) throws IOException {
        long requestId = getRequestId(msg);
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
                Response oriResponse = (Response) msg;
                Response response = MotanFrameworkUtil.buildErrorResponse(oriResponse.getRequestId(), oriResponse.getRpcProtocolVersion(), e);
                data = codec.encode(channel, response);
            }
        } else {
            data = codec.encode(channel, msg);
        }
        if (msg instanceof Request) {
            MotanFrameworkUtil.logEvent((Request) msg, MotanConstants.TRACE_CENCODE);
        } else if (msg instanceof Response) {
            MotanFrameworkUtil.logEvent((Response) msg, MotanConstants.TRACE_SENCODE);
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
