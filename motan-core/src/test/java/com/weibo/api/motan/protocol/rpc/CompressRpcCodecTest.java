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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.mock.MockChannel;
import com.weibo.api.motan.protocol.rpc.CompressRpcCodec.MethodInfo;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.util.ByteUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import com.weibo.api.motan.util.MotanSwitcherUtil;

/**
 * 基础功能由父类进行测试，此类中测试开关、版本兼容性、gz压缩等功能
 * 
 * @author zhanglei
 *
 */
public class CompressRpcCodecTest extends DefaultRpcCodecTest {

    @Before
    public void setUp() throws Exception {
        rpcCodec = new CompressRpcCodec();
        MotanSwitcherUtil.setSwitcherValue(CompressRpcCodec.CODEC_VERSION_SWITCHER, false);
        boolean isopen =
                MotanSwitcherUtil.switcherIsOpenWithDefault(CompressRpcCodec.GROUP_CODEC_VERSION_SWITCHER + URLParamType.group.getValue(),
                        false);
        if (isopen) {
            MotanSwitcherUtil.setSwitcherValue(CompressRpcCodec.GROUP_CODEC_VERSION_SWITCHER + URLParamType.group.getValue(), false);
        }

    }

    @After
    public void tearDown() throws Exception {}

    // 测试开关有效性
    @Test
    public void testSwitcher() throws IOException {
        DefaultRequest request = getRequest("void", null);
        byte[] bytes = rpcCodec.encode(channel, request);
        assertTrue(isCompressVersion(bytes));
        // 整体开关测试
        MotanSwitcherUtil.setSwitcherValue(CompressRpcCodec.CODEC_VERSION_SWITCHER, true);
        bytes = rpcCodec.encode(channel, request);
        assertTrue(isV1Version(bytes));
        // 分组开关测试
        MotanSwitcherUtil.setSwitcherValue(CompressRpcCodec.CODEC_VERSION_SWITCHER, false);
        MotanSwitcherUtil.setSwitcherValue(CompressRpcCodec.GROUP_CODEC_VERSION_SWITCHER + URLParamType.group.getValue(), true);
        bytes = rpcCodec.encode(channel, request);
        assertTrue(isV1Version(bytes));

    }


    // 测试server端对旧版本的兼容性
    @Test
    public void testCompatibility() throws IOException {
        DefaultRequest request = getRequest("int[]", new Object[] {new int[] {1, 2}});
        Codec v1Codec = new DefaultRpcCodec();
        byte[] bytes = v1Codec.encode(channel, request);
        assertTrue(isV1Version(bytes));
        Request result = (Request) rpcCodec.decode(channel, "", bytes);

        Assert.assertTrue(equals(request, result));
    }

    // 测试gz压缩
    @Test
    public void testGzip() throws IOException {
        DefaultRequest request = getRequest("int[]", new Object[] {new int[] {1, 2}});
        byte[] bytes = rpcCodec.encode(channel, request);
        assertFalse(isGzip(bytes));

        // 小于阈值
        int bodyLength = ByteUtil.bytes2int(bytes, 12);
        URL url = new URL("motan", "localhost", 18080, "com.weibo.api.motan.procotol.example.IHello");
        Map<String, String> params = url.getParameters();
        params.put(URLParamType.usegz.name(), "true");
        params.put(URLParamType.mingzSize.name(), String.valueOf(bodyLength - 1));
        Channel tempChannel = new MockChannel(url);
        bytes = rpcCodec.encode(tempChannel, request);
        assertTrue(isGzip(bytes));

        // 等于、大于阈值.url内部对数字类型参数有缓存，且无法动态更新，需要重新生产url及channel
        url = new URL("motan", "localhost", 18080, "com.weibo.api.motan.procotol.example.IHello");
        params = url.getParameters();
        params.put(URLParamType.usegz.name(), "true");
        params.put(URLParamType.mingzSize.name(), String.valueOf(bodyLength));
        tempChannel = new MockChannel(url);
        bytes = rpcCodec.encode(tempChannel, request);
        assertFalse(isGzip(bytes));
    }



    public void testCodecRequest(Request request) throws Exception {
        byte[] bytes = rpcCodec.encode(channel, request);
        assertTrue(isCompressVersion(bytes));
        Request result = (Request) rpcCodec.decode(channel, "", bytes);

        Assert.assertTrue(equals(request, result));
    }

    public void testCodecResponse(Response respose) throws Exception {
        respose.setRpcProtocolVersion(RpcProtocolVersion.VERSION_2.getVersion());
        byte[] bytes = rpcCodec.encode(channel, respose);
        assertTrue(isCompressVersion(bytes));
        Response result = (Response) rpcCodec.decode(channel, "", bytes);

        Assert.assertTrue(result.getValue().toString().equals(respose.getValue().toString()));
    }

    protected DefaultRequest getRequest(String paramtersDesc, Object[] params) {
        DefaultRequest request = new DefaultRequest();
        request.setInterfaceName(basicInterface);
        request.setMethodName(basicMethod);
        request.setParamtersDesc(paramtersDesc);
        if (params != null) {
            request.setArguments(params);
        }
        Map<String, String> attachmentsMap = new HashMap<String, String>();
        attachmentsMap.put(URLParamType.group.name(), URLParamType.group.getValue());
        attachmentsMap.put(URLParamType.version.name(), URLParamType.version.getValue());
        attachmentsMap.put(URLParamType.module.name(), URLParamType.module.getValue());
        attachmentsMap.put(URLParamType.application.name(), URLParamType.application.getValue());
        request.setAttachments(attachmentsMap);
        addMethodSign(request, (CompressRpcCodec) rpcCodec);

        return request;
    }

    private void addMethodSign(Request request, CompressRpcCodec codec) {
        MethodInfo temp =
                new MethodInfo(MotanFrameworkUtil.getGroupFromRequest(request), request.getInterfaceName(), request.getMethodName(),
                        request.getParamtersDesc(), MotanFrameworkUtil.getVersionFromRequest(request));
        codec.putMethodSign(temp.getSign(), temp);
    }

    private boolean isV1Version(byte[] bytes) {
        return bytes[2] == RpcProtocolVersion.VERSION_1.getVersion();
    }

    private boolean isCompressVersion(byte[] bytes) {
        return bytes[2] == RpcProtocolVersion.VERSION_2.getVersion();
    }

    private boolean isGzip(byte[] bytes) {
        int bodyLength = ByteUtil.bytes2int(bytes, 12);
        byte[] body = new byte[bodyLength];
        System.arraycopy(bytes, RpcProtocolVersion.VERSION_1.getHeaderLength(), body, 0, bodyLength);
        InputStream inputStream = CompressRpcCodec.getInputStream(body);
        return inputStream instanceof GZIPInputStream;
    }

}
