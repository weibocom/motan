package com.weibo.api.motan.registry.mesh;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanErrorMsg;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.registry.Registry;
import com.weibo.api.motan.registry.RegistryFactory;
import com.weibo.api.motan.registry.support.AbstractRegistryFactory;
import com.weibo.api.motan.rpc.URL;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * @author sunnights
 */
@SpiMeta(name = "mesh")
public class MeshRegistryFactory extends AbstractRegistryFactory {
    @Override
    protected Registry createRegistry(URL url) {
        String backupReg = url.getParameter("backupRegistry");
        if (StringUtils.isBlank(backupReg)) {
            throw new MotanFrameworkException("Missing backupRegistry in meshRegistry!" + url, MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }

        URL backupUrl = URL.valueOf(backupReg.split(";")[0]);
        RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getExtension(backupUrl.getProtocol());
        if (registryFactory == null) {
            throw new MotanFrameworkException(new MotanErrorMsg(500, MotanErrorMsgConstant.FRAMEWORK_REGISTER_ERROR_CODE,
                    "register error! Could not find extension for registry protocol:" + url.getProtocol()
                            + ", make sure registry module for " + url.getProtocol() + " is in classpath!"));
        }
        Registry backupRegistry = registryFactory.getRegistry(backupUrl);
        String backupRegistryUrl = backupUrl.getProtocol() + "://" + backupUrl.getServerPortStr();

        int requestTimeout = url.getIntParameter(URLParamType.requestTimeout.getName(), URLParamType.requestTimeout.getIntValue());
        int connectTimeout = url.getIntParameter(URLParamType.connectTimeout.getName(), URLParamType.connectTimeout.getIntValue());
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(requestTimeout)
                .setConnectTimeout(connectTimeout)
                .build();
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();
        MeshClient meshClient = new MeshClient(httpClient, url.getServerPortStr(), backupRegistryUrl);
        return new MeshRegistry(url, meshClient, backupRegistry);
    }
}
