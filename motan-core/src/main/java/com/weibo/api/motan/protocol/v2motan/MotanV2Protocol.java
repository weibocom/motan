package com.weibo.api.motan.protocol.v2motan;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.AbstractProtocol;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.transport.*;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhanglei28 on 2017/4/27.
 * 协议默认配置codec和serialize
 *  TODO 按v2版本实现对应逻辑
 */
@SpiMeta(name = "motan2")
public class MotanV2Protocol extends AbstractProtocol{


    // 多个service可能在相同端口进行服务暴露，因此来自同个端口的请求需要进行路由以找到相应的服务，同时不在该端口暴露的服务不应该被找到
    private Map<String, ProviderMessageRouter> ipPort2RequestRouter = new HashMap<String, ProviderMessageRouter>();

    @Override
    protected <T> Exporter<T> createExporter(Provider<T> provider, URL url) {
        setDefaultCodec(url);
        return new V2RpcExporter<T>(provider, url);
    }

    @Override
    protected <T> Referer<T> createReferer(Class<T> clz, URL url, URL serviceUrl) {
        setDefaultCodec(url);
        return new V2RpcReferer<T>(clz, url, serviceUrl);
    }

    private void setDefaultCodec(URL url){
        //TODO check 注册中心通知时是否有url判等之类的问题
        String codec = url.getParameter(URLParamType.codec.getName());
        if (StringUtils.isBlank(codec)){
            url.getParameters().put(URLParamType.codec.getName(), "motan2");
        }
    }

    /**
     * rpc provider
     *
     * @param <T>
     * @author maijunsheng
     */
    class V2RpcExporter<T> extends AbstractExporter<T> {
        private Server server;
        private EndpointFactory endpointFactory;

        public V2RpcExporter(Provider<T> provider, URL url) {
            super(provider, url);

            ProviderMessageRouter requestRouter = initRequestRouter(url);
            endpointFactory =
                    ExtensionLoader.getExtensionLoader(EndpointFactory.class).getExtension(
                            url.getParameter(URLParamType.endpointFactory.getName(), URLParamType.endpointFactory.getValue()));
            server = endpointFactory.createServer(url, requestRouter);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void unexport() {
            String protocolKey = MotanFrameworkUtil.getProtocolKey(url);
            String ipPort = url.getServerPortStr();

            Exporter<T> exporter = (Exporter<T>) exporterMap.remove(protocolKey);

            if (exporter != null) {
                exporter.destroy();
            }

            synchronized (ipPort2RequestRouter) {
                ProviderMessageRouter requestRouter = ipPort2RequestRouter.get(ipPort);

                if (requestRouter != null) {
                    requestRouter.removeProvider(provider);
                }
            }

            LoggerUtil.info("DefaultRpcExporter unexport Success: url={}", url);
        }

        @Override
        protected boolean doInit() {
            boolean result = server.open();

            return result;
        }

        @Override
        public boolean isAvailable() {
            return server.isAvailable();
        }

        @Override
        public void destroy() {
            endpointFactory.safeReleaseResource(server, url);
            LoggerUtil.info("DefaultRpcExporter destory Success: url={}", url);
        }

        private ProviderMessageRouter initRequestRouter(URL url) {
            ProviderMessageRouter requestRouter = null;
            String ipPort = url.getServerPortStr();

            synchronized (ipPort2RequestRouter) {
                requestRouter = ipPort2RequestRouter.get(ipPort);

                if (requestRouter == null) {
                    requestRouter = new ProviderProtectedMessageRouter(provider);
                    ipPort2RequestRouter.put(ipPort, requestRouter);
                } else {
                    requestRouter.addProvider(provider);
                }
            }

            return requestRouter;
        }
    }

    /**
     * rpc referer
     *
     * @param <T>
     * @author maijunsheng
     */
    class V2RpcReferer<T> extends AbstractReferer<T> {
        private Client client;
        private EndpointFactory endpointFactory;

        public V2RpcReferer(Class<T> clz, URL url, URL serviceUrl) {
            super(clz, url, serviceUrl);

            endpointFactory =
                    ExtensionLoader.getExtensionLoader(EndpointFactory.class).getExtension(
                            url.getParameter(URLParamType.endpointFactory.getName(), URLParamType.endpointFactory.getValue()));

            client = endpointFactory.createClient(url);
        }

        @Override
        protected Response doCall(Request request) {
            try {
                // 为了能够实现跨group请求，需要使用server端的group。
                request.setAttachment(URLParamType.group.getName(), serviceUrl.getGroup());
                return client.request(request);
            } catch (TransportException exception) {
                throw new MotanServiceException("DefaultRpcReferer call Error: url=" + url.getUri(), exception);
            }
        }

        @Override
        protected void decrActiveCount(Request request, Response response) {
            if (response == null || !(response instanceof Future)) {
                activeRefererCount.decrementAndGet();
                return;
            }

            Future future = (Future) response;

            future.addListener(new FutureListener() {
                @Override
                public void operationComplete(Future future) throws Exception {
                    activeRefererCount.decrementAndGet();
                }
            });
        }

        @Override
        protected boolean doInit() {
            boolean result = client.open();

            return result;
        }

        @Override
        public boolean isAvailable() {
            return client.isAvailable();
        }

        @Override
        public void destroy() {
            endpointFactory.safeReleaseResource(client, url);
            LoggerUtil.info("DefaultRpcReferer destory client: url={}" + url);
        }
    }
}
