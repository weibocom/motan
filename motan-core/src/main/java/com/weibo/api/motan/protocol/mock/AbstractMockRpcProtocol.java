package com.weibo.api.motan.protocol.mock;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.protocol.AbstractProtocol;
import com.weibo.api.motan.rpc.AbstractExporter;
import com.weibo.api.motan.rpc.AbstractReferer;
import com.weibo.api.motan.rpc.Exporter;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.EndpointFactory;
import com.weibo.api.motan.transport.ProviderMessageRouter;
import com.weibo.api.motan.transport.Server;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;

/**
 * 
 * @Description:abstract mock protocol, it can mock all rpc from server or client.
 *                    implementation class must implement 'processRequest()' method, and declare SpiMeta annotation. 
 * @author zhanglei28
 * @date 2016-3-14
 *
 */
public abstract class AbstractMockRpcProtocol extends AbstractProtocol {
    private static ProviderMessageRouter mockProviderMessageRouter;

    @Override
    protected <T> Exporter<T> createExporter(Provider<T> provider, URL url) {
        Exporter<T> exporter = new MockRpcExporter<T>(provider, url);
        LoggerUtil.info("create MockRpcExporter: url={}", url);
        return exporter;
    }

    @Override
    protected <T> Referer<T> createReferer(Class<T> clz, URL url, URL serviceUrl) {
        Referer<T> referer = new MockRpcReferer<T>(clz, url, serviceUrl);
        LoggerUtil.info("create MockRpcReferer: url={}", url);
        return referer;
    }

    class MockRpcExporter<T> extends AbstractExporter<T> {
        private Server server;
        private EndpointFactory endpointFactory;

        public MockRpcExporter(Provider<T> provider, URL url) {
            super(provider, url);

            ProviderMessageRouter requestRouter = getMockProviderMessageRouter(url);
            endpointFactory =
                    ExtensionLoader.getExtensionLoader(EndpointFactory.class).getExtension(
                            url.getParameter(URLParamType.endpointFactory.getName(), URLParamType.endpointFactory.getValue()));
            server = endpointFactory.createServer(url, requestRouter);
        }

        @Override
        public void unexport() {
            String protocolKey = MotanFrameworkUtil.getProtocolKey(url);
            Exporter<T> exporter = (Exporter<T>) exporterMap.remove(protocolKey);
            if (exporter != null) {
                exporter.destroy();
            }
        }

        @Override
        public void destroy() {
            endpointFactory.safeReleaseResource(server, url);
            LoggerUtil.info("MockRpcExporter destory Success: url={}", url);
        }

        @Override
        protected boolean doInit() {
            return server.open();
        }

        @Override
        public boolean isAvailable() {
            return server.isAvailable();
        }

    }


    class MockRpcReferer<T> extends AbstractReferer<T> {


        public MockRpcReferer(Class<T> clz, URL url, URL serviceUrl) {
            super(clz, url, serviceUrl);
        }


        @Override
        public void destroy() {
            LoggerUtil.info("MockRpcReferer destroy Success: url={}", url);
        }

        @Override
        protected Response doCall(Request request) {
            return processRequest(request);
        }

        @Override
        protected boolean doInit() {
            LoggerUtil.info("MockRpcReferer init Success: url={}", url);
            return true;
        }

    }

    // process all urls。
    public ProviderMessageRouter getMockProviderMessageRouter(URL url) {
        if (mockProviderMessageRouter == null) {
            //default
            mockProviderMessageRouter = new MockProviderMessageRouter();
        }

        return mockProviderMessageRouter;
    }

    class MockProviderMessageRouter extends ProviderMessageRouter {

        @Override
        public Object handle(Channel channel, Object message) {
            if (channel == null || message == null) {
                throw new MotanFrameworkException("RequestRouter handler(channel, message) params is null");
            }

            if (!(message instanceof Request)) {
                throw new MotanFrameworkException("RequestRouter message type not support: " + message.getClass());
            }
            return processRequest((Request) message);
        }

    }

    /**
     * process request. request is mock processed by client or server
     * 
     * @param request
     * @return
     */
    protected abstract Response processRequest(Request request);

}
