/*
 *
 *   Copyright 2009-2023 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.rpc;

import com.weibo.api.motan.exception.MotanBizException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.example.IWorld;
import com.weibo.api.motan.protocol.example.IWorldAsync;
import com.weibo.api.motan.protocol.example.MockWorld;
import com.weibo.api.motan.runtime.RuntimeInfoKeys;
import com.weibo.api.motan.util.AsyncUtil;
import junit.framework.TestCase;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zhanglei28
 * @date 2023/10/18.
 */
@SuppressWarnings("ALL")
public class DefaultProviderTest extends TestCase {
    URL url = new URL("motan2", "localhost", 8002, IWorld.class.getName());
    MockWorld proxyImpl = new MockWorld();
    IWorldAsyncImpl asyncProxyImpl = new IWorldAsyncImpl();
    Class<?> aClass = IWorld.class;


    public void testInvoke() {
        DefaultProvider provider = new DefaultProvider(proxyImpl, url, aClass);

        // === test sync method invoke ===
        Response response = provider.invoke(buildDefaultRequest("world", "void", new Object[]{}));
        assertTrue(response instanceof DefaultResponse);
        assertEquals(proxyImpl.world(), response.getValue()); // proxyImpl.world() trigger count++ again
        assertEquals(2, proxyImpl.count.get());

        // parameterDesc compatible
        response = provider.invoke(buildDefaultRequest("worldSleep", null, new Object[]{"xxx", 5}));
        assertTrue(response instanceof DefaultResponse);
        assertEquals("xxx1", response.getValue());
        assertEquals(1, proxyImpl.sleepCount.get());

        // invoke exception
        response = provider.invoke(buildDefaultRequest("worldSleep", null, new Object[]{}));
        assertTrue(response.getException() instanceof MotanBizException);

        // === test async method invoke ===
        provider = new DefaultProvider(asyncProxyImpl, url, aClass); // use async implement class
        response = provider.invoke(buildDefaultRequest("world", "void", new Object[]{}));
        assertTrue(response instanceof ResponseFuture);
        final AtomicBoolean finish = new AtomicBoolean(false);
        ((ResponseFuture) response).addListener((future) -> finish.set(true));
        assertEquals("success", response.getValue()); // wait value from future
        assertEquals(1, asyncProxyImpl.count.get());
        assertTrue(finish.get());

        // sync exception
        response = provider.invoke(buildDefaultRequest("worldSleep", null, new Object[]{"xxx", 5}));
        assertTrue(response instanceof DefaultResponse);
        assertTrue(response.getException() instanceof MotanBizException);
        assertTrue(response.getException().getCause() instanceof MotanServiceException);

        // async biz exception
        response = provider.invoke(buildDefaultRequest("world", "java.lang.String", new Object[]{"fail"}));
        assertTrue(response instanceof ResponseFuture);
        try {
            response.getValue(); // wait result
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof RuntimeException);
        }
        assertEquals(1, asyncProxyImpl.stringCount.get());

        // ===== get runtime info =====
        // async implement
        provider = new DefaultProvider(asyncProxyImpl, url, aClass); // use async implement class
        Map<String, Object> info = provider.getRuntimeInfo();
        assertEquals(asyncProxyImpl.getClass().getName(), info.get(RuntimeInfoKeys.IMPL_CLASS_KEY));
        assertTrue((Boolean) info.get(RuntimeInfoKeys.IS_ASYNC_KEY));
        assertEquals(aClass.getName(), info.get(RuntimeInfoKeys.SERVICE_KEY));

        // sync implment
        provider = new DefaultProvider(proxyImpl, url, aClass); // use async implement class
        info = provider.getRuntimeInfo();
        assertEquals(proxyImpl.getClass().getName(), info.get(RuntimeInfoKeys.IMPL_CLASS_KEY));
        assertFalse((Boolean) info.get(RuntimeInfoKeys.IS_ASYNC_KEY));
        assertEquals(aClass.getName(), info.get(RuntimeInfoKeys.SERVICE_KEY));
    }

    private DefaultRequest buildDefaultRequest(String method, String parametersDesc, Object[] arguments) {
        DefaultRequest request = new DefaultRequest();
        request.setRequestId(123456);
        request.setMethodName(method);
        request.setParamtersDesc(parametersDesc);
        request.setInterfaceName(IWorld.class.getName());
        request.setArguments(arguments);
        RpcContext.init(request);
        return request;
    }

    static class IWorldAsyncImpl implements IWorldAsync {
        public AtomicLong count = new AtomicLong();
        public AtomicLong stringCount = new AtomicLong();

        @Override
        public String world() {
            throw new RuntimeException("synchronous methods should not be executed");
        }

        @Override
        public String world(String world) {
            throw new RuntimeException("synchronous methods should not be executed");
        }

        @Override
        public String worldSleep(String world, int sleep) {
            throw new RuntimeException("synchronous methods should not be executed");
        }

        @Override
        public ResponseFuture worldAsync() {
            ResponseFuture future = AsyncUtil.createResponseFutureForServerEnd();
            AsyncUtil.getDefaultCallbackExecutor().execute(() -> {
                count.incrementAndGet();
                future.onSuccess("success");
            });
            return future;
        }

        @Override
        public ResponseFuture worldAsync(String world) {
            ResponseFuture future = AsyncUtil.createResponseFutureForServerEnd();
            AsyncUtil.getDefaultCallbackExecutor().execute(() -> {
                stringCount.incrementAndGet();
                if ("success".equals(world)) {
                    future.onSuccess("success");
                } else {
                    future.onFailure(new RuntimeException("fail")); // 异步异常
                }
            });
            return future;
        }

        @Override
        public ResponseFuture worldSleepAsync(String world, int sleep) {
            throw new MotanServiceException("not implement"); // 同步异常
        }
    }
}