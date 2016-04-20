package com.weibo.api.motan.registry.consul;

import com.weibo.api.motan.rpc.URL;

public class ConsulUtils {
	

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
		return group.substring(ConsulConstants.CONSUL_SERVICE_MOTAN_PRE
				.length());
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
	 * 使用consul的tag来保持protocol信息。 根据motan的protocol生成consul的tag。
	 * 
	 * @param protocol
	 * @return
	 */
	public static String convertProtocolToTag(String protocol) {
		return ConsulConstants.CONSUL_TAG_MOTAN_PROTOCOL + protocol;
	}

	/**
	 * 从consul的tag获取motan的protocol
	 * 
	 * @param tag
	 * @return
	 */
	public static String getProtocolFromTag(String tag) {
		return tag
				.substring(ConsulConstants.CONSUL_TAG_MOTAN_PROTOCOL.length());
	}
	
	
	public static String convertServiceId(String host, int port, String path){
		return host + ":" + port+ "-" + path;
	}

}
