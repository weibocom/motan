package com.weibo.api.motan.registry.consul;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.StringTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsulUtils {

    /**
     * 判断两个list中的url是否一致。 如果任意一个list为空，则返回false； 此方法并未做严格互相判等
     *
     * @param urls1
     * @param urls2
     * @return
     */
    public static boolean isSame(List<URL> urls1, List<URL> urls2) {
        if (urls1 == null || urls2 == null) {
            return false;
        }
        if (urls1.size() != urls2.size()) {
            return false;
        }
        return urls1.containsAll(urls2);
    }

    /**
     * 根据服务的url生成consul对应的service
     *
     * @param url
     * @return
     */
    public static ConsulService buildService(URL url) {
        ConsulService service = new ConsulService();
        service.setAddress(url.getHost());
        service.setId(ConsulUtils.convertConsulSerivceId(url));
        service.setName(ConsulUtils.convertGroupToServiceName(url.getGroup()));
        service.setPort(url.getPort());
        service.setTtl(ConsulConstants.TTL);

        List<String> tags = new ArrayList<String>();
        tags.add(ConsulConstants.CONSUL_TAG_MOTAN_PROTOCOL + url.getProtocol());
        tags.add(ConsulConstants.CONSUL_TAG_MOTAN_URL + StringTools.urlEncode(url.toFullStr()));
        service.setTags(tags);

        return service;
    }

    /**
     * 根据service生成motan使用的
     *
     * @param service
     * @return
     */
    public static URL buildUrl(ConsulService service) {
        URL url = null;
        for (String tag : service.getTags()) {
            if (tag.startsWith(ConsulConstants.CONSUL_TAG_MOTAN_URL)) {
                String encodeUrl = tag.substring(tag.indexOf("_") + 1);
                url = URL.valueOf(StringTools.urlDecode(encodeUrl));
            }
        }
        if (url == null) {
            Map<String, String> params = new HashMap<String, String>();
            String group = service.getName().substring(ConsulConstants.CONSUL_SERVICE_MOTAN_PRE.length());
            params.put(URLParamType.group.getName(), group);
            params.put(URLParamType.nodeType.getName(), MotanConstants.NODE_TYPE_SERVICE);
            String protocol = ConsulUtils.getProtocolFromTag(service.getTags().get(0));
            url = new URL(protocol, service.getAddress(), service.getPort(),
                    ConsulUtils.getPathFromServiceId(service.getId()), params);
        }
        return url;
    }

    /**
     * 根据url获取cluster信息，cluster 信息包括协议和path（rpc服务中的接口类）。
     *
     * @param url
     * @return
     */
    public static String getUrlClusterInfo(URL url) {
        return url.getProtocol() + "-" + url.getPath();
    }

    /**
     * 有motan的group生成consul的serivce name
     *
     * @param group
     * @return
     */
    public static String convertGroupToServiceName(String group) {
        return ConsulConstants.CONSUL_SERVICE_MOTAN_PRE + group;
    }

    /**
     * 从consul的service name中获取motan的group
     *
     * @param group
     * @return
     */
    public static String getGroupFromServiceName(String group) {
        return group.substring(ConsulConstants.CONSUL_SERVICE_MOTAN_PRE.length());
    }

    /**
     * 根据motan的url生成consul的serivce id。 serviceid 包括ip＋port＋rpc服务的接口类名
     *
     * @param url
     * @return
     */
    public static String convertConsulSerivceId(URL url) {
        if (url == null) {
            return null;
        }
        return convertServiceId(url.getHost(), url.getPort(), url.getPath());
    }

    /**
     * 从consul 的serviceid中获取rpc服务的接口类名（url的path）
     *
     * @param serviceId
     * @return
     */
    public static String getPathFromServiceId(String serviceId) {
        return serviceId.substring(serviceId.indexOf("-") + 1);
    }

    /**
     * 从consul的tag获取motan的protocol
     *
     * @param tag
     * @return
     */
    public static String getProtocolFromTag(String tag) {
        return tag.substring(ConsulConstants.CONSUL_TAG_MOTAN_PROTOCOL.length());
    }


    public static String convertServiceId(String host, int port, String path) {
        return host + ":" + port + "-" + path;
    }

}
