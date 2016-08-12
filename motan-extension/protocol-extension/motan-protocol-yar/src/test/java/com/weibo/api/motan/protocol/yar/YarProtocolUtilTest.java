/*
 * Copyright 2009-2016 Weibo, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.weibo.api.motan.protocol.yar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.exception.MotanBizException;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.ReflectUtil;
import com.weibo.yar.YarRequest;
import com.weibo.yar.YarResponse;

/**
 * 
 * @Description YarProtocolUtilTest
 * @author zhanglei
 * @date 2016年7月27日
 *
 */
public class YarProtocolUtilTest {

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testGetYarPath() {
        String path = YarProtocolUtil.getYarPath(YarMessageRouterTest.AnnoService.class, null);
        assertEquals("/test/anno_path", path);
        URL url = new URL("motan", "localhost", 8002, "testpath");
        path = YarProtocolUtil.getYarPath(null, url);
        assertEquals("/" + url.getGroup() + "/" + url.getPath(), path);
    }

    @Test
    public void testConvertYarRequest() throws NoSuchMethodException, SecurityException {
        DefaultRequest request = new DefaultRequest();
        request.setRequestId(123);
        request.setMethodName("hello");
        request.setArguments(new Object[] {"param1"});
        request.setInterfaceName(YarMessageRouterTest.AnnoService.class.getName());
        request.setParamtersDesc(ReflectUtil.getMethodParamDesc(YarMessageRouterTest.AnnoService.class.getMethod("hello", String.class)));
        YarRequest yarRequest = YarProtocolUtil.convert(request, "JSON");
        assertNotNull(yarRequest);

        Request newRequest = YarProtocolUtil.convert(yarRequest, YarMessageRouterTest.AnnoService.class);
        assertNotNull(newRequest);
        assertEquals(request.toString(), newRequest.toString());
    }

    @Test
    // test string cast primitive value
    public void testConvertRequest() throws Exception {
        String methodName = "testParam";
        Class[] paramClazz = new Class[] {int.class, long.class, boolean.class, float.class, double.class};
        Method method = MethodTestService.class.getDeclaredMethod(methodName, paramClazz);
        final String result = "succ";
        MethodTestService service = new MethodTestService() {
            @Override
            public String testParam(int intParam, long longParam, boolean booleanParam, float floatParam, double doubleParam) {
                return result;
            }
        };

        // string
        Object[] params = new Object[] {"234", "567", "true", "789.12", "678.12"};
        verifyMethodParam(MethodTestService.class, service, method, params, result);

        // number
        params = new Object[] {234l, 567, false, 789.12d, 678.12f};
        verifyMethodParam(MethodTestService.class, service, method, params, result);
    }


    private <T> void verifyMethodParam(Class<T> interfaceClazz, T service, Method method, Object[] params, Object expectResult)
            throws Exception {
        YarRequest yarRequest = new YarRequest();
        yarRequest.setId(123);
        yarRequest.setMethodName(method.getName());
        yarRequest.setPackagerName("JSON");
        yarRequest.setParameters(params);

        Request request = YarProtocolUtil.convert(yarRequest, interfaceClazz);
        assertNotNull(request);
        assertEquals(method.getName(), request.getMethodName());
        Object[] requestParams = request.getArguments();
        assertEquals(params.length, requestParams.length);
        Object result = method.invoke(service, requestParams);
        assertEquals(expectResult, result);
    }

    @Test
    public void testConvertYarResponse() {
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(456);
        response.setValue("stringValue");

        YarResponse yarResponse = YarProtocolUtil.convert(response, "JSON");
        assertNotNull(yarResponse);
        Response newResponse = YarProtocolUtil.convert(yarResponse);
        assertEquals(response.getRequestId(), newResponse.getRequestId());
        assertEquals(response.getValue(), newResponse.getValue());


        response.setException(new RuntimeException("test exception"));

        yarResponse = YarProtocolUtil.convert(response, "JSON");
        assertNotNull(yarResponse);
        newResponse = YarProtocolUtil.convert(yarResponse);
        assertEquals(response.getRequestId(), newResponse.getRequestId());
        // yarresponse的异常会转为motan业务异常
        assertEquals(new MotanBizException(response.getException().getMessage()).getMessage(), newResponse.getException().getMessage());

    }

    @Test
    public void testBuildDefaultErrorResponse() {
        String errMsg = "test err";
        String packagerName = "MSGPACK";
        YarResponse response = YarProtocolUtil.buildDefaultErrorResponse(errMsg, packagerName);
        assertNotNull(response);
        assertEquals(errMsg, response.getError());
        assertEquals(packagerName, response.getPackagerName());
    }

    interface MethodTestService {
        String testParam(int intParam, long longParam, boolean booleanParam, float floatParam, double doubleParam);
    }

}
