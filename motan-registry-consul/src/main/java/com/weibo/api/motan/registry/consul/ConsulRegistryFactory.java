package com.weibo.api.motan.registry.consul;

import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.registry.Registry;
import com.weibo.api.motan.registry.consul.client.ConsulEcwidClient;
import com.weibo.api.motan.registry.consul.client.MotanConsulClient;
import com.weibo.api.motan.registry.support.AbstractRegistryFactory;
import com.weibo.api.motan.rpc.URL;
import org.apache.commons.lang3.StringUtils;


@SpiMeta(name = "consul")
public class ConsulRegistryFactory extends AbstractRegistryFactory {

    @Override
    protected Registry createRegistry(URL url) {
        String host = ConsulConstants.DEFAULT_HOST;
        int port = ConsulConstants.DEFAULT_PORT;
        if (StringUtils.isNotBlank(url.getHost())) {
            host = url.getHost();
        }
        if (url.getPort() > 0) {
            port = url.getPort();
        }
        //可以使用不同的client实现
        MotanConsulClient client = new ConsulEcwidClient(host, port);
        return new ConsulRegistry(url, client);
    }

}
