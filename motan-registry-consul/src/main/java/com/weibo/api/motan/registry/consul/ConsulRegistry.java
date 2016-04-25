package com.weibo.api.motan.registry.consul;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.registry.consul.client.MotanConsulClient;
import com.weibo.api.motan.registry.support.FailbackRegistry;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.LoggerUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConsulRegistry extends FailbackRegistry {
	private MotanConsulClient client;
	private ConsulHeartbeatManager heartbeatManager;
	private int lookupInterval;

	/**
	 * consul service的本地缓存。 key为group（consul中的service），value为接口名与对应的url list
	 */
	private ConcurrentHashMap<String, ConcurrentHashMap<String, List<URL>>> servicesCache = new ConcurrentHashMap<String, ConcurrentHashMap<String, List<URL>>>();

	// 记录所有已经启动查询线程的group，保证每个group只只启动一个线程。map的值用来记录上次查询consul的consulIndex
	private ConcurrentHashMap<String, Long> lookupGroups = new ConcurrentHashMap<String, Long>();

	/**
	 * 保持服务订阅方的所有回调listener,当订阅的服务发生变更时会通过listener进行回调
	 */
	private ConcurrentHashMap<String, HashMap<URL, NotifyListener>> subscribeListeners = new ConcurrentHashMap<String, HashMap<URL, NotifyListener>>();

	private ThreadPoolExecutor notifyExecutor;
	
	public ConsulRegistry(URL url, MotanConsulClient client) {
		super(url);
		this.client = client;
        heartbeatManager = new ConsulHeartbeatManager(client);
        heartbeatManager.start();
        lookupInterval = getUrl().getIntParameter(
                URLParamType.registrySessionTimeout.getName(),
                ConsulConstants.DEFAULT_LOOKUP_INTERVAL);

        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(
                20000);
        notifyExecutor = new ThreadPoolExecutor(10, 30, 30 * 1000,
                TimeUnit.MILLISECONDS, workQueue);
        LoggerUtil.info("ConsulRegistry init finish.");
	}
	
	

	@Override
	protected void doRegister(URL url) {
		ConsulService service = buildService(url);
		client.registerService(service);
		heartbeatManager.addHeartbeatServcieId(service.getId());
		getRegisteredServiceUrls().add(url);
	}

	@Override
	protected void doUnregister(URL url) {
		ConsulService service = buildService(url);
		client.deregisterService(service.getId());
		heartbeatManager.removeHeartbeatServiceId(service.getId());
		getRegisteredServiceUrls().remove(url);
	}

	@Override
	protected void doSubscribe(URL url, NotifyListener listener) {
		addSubscribeListeners(url, listener);
		startListenerThreadIfNewService(url);
		List<URL> urls = discover(url); // 订阅后先查询一次对应的服务，供refer初始化使用。
		if (urls != null && !urls.isEmpty()) {
			notify(url, listener, urls);
		}

	}

	/**
	 * 每个group使用一个查询线程进行服务发现，如果是新注册的group就开启一个新的查询线程。
	 * 
	 * @param url
	 */
	private void startListenerThreadIfNewService(URL url) {
		String group = url.getGroup();
		if (!lookupGroups.containsKey(group)) {
			Long value = lookupGroups.putIfAbsent(group, 0l);
			if (value == null) {
				GroupLookupThread lookupThread = new GroupLookupThread(
						group);
				lookupThread.setDaemon(true);
				lookupThread.start();
			}
		}
	}

	/**
	 * 保存url对应回调listener 按group－>url－>listener分类保存
	 * 
	 * @param url
	 * @param listener
	 */
	private void addSubscribeListeners(URL url, NotifyListener listener) {

		String cluster = getUrlClusterInfo(url);
		HashMap<URL, NotifyListener> map = subscribeListeners.get(cluster);
		if (map == null) {
			subscribeListeners.putIfAbsent(cluster,
					new HashMap<URL, NotifyListener>());
			map = subscribeListeners.get(cluster);
		}
		synchronized (map) {
			map.put(url, listener);
		}
	}

	@Override
	protected void doUnsubscribe(URL url, NotifyListener listener) {

		HashMap<URL, NotifyListener> clusterListeners = subscribeListeners
				.get(getUrlClusterInfo(url));
		if (clusterListeners != null) {
			synchronized (clusterListeners) {
				clusterListeners.remove(url);
			}
		}
	}

	@Override
	protected List<URL> doDiscover(URL url) {
		String cluster = getUrlClusterInfo(url);
		String group = url.getGroup();
		List<URL> clusterUrl = new ArrayList<URL>();
		ConcurrentHashMap<String, List<URL>> clusterMap = servicesCache
				.get(group);
		if (clusterMap == null) {
			// 更新整个group
			synchronized (group.intern()) {
				clusterMap = servicesCache.get(group);
				if (clusterMap == null) {
					ConcurrentHashMap<String, List<URL>> groupUrls = lookupForUpdate(group);
					updateServicesCache(group, groupUrls, false);
					clusterMap = servicesCache.get(group);
				}
			}
		}
		if (clusterMap != null) {
			clusterUrl = clusterMap.get(cluster);
		}
		return clusterUrl;
	}

    @Override
    protected void doAvailable(URL url) {
        if (url == null) {
            heartbeatManager.setHeartbeatOpen(true);
        } else {
            throw new UnsupportedOperationException("consul registry not support available by urls yet");
        }
    }

    @Override
    protected void doUnavailable(URL url) {
        if (url == null) {
            heartbeatManager.setHeartbeatOpen(false);
        } else {
            throw new UnsupportedOperationException("consul registry not support unavailable by urls yet");
        }
    }

    @Override
	public List<URL> discover(URL url) {
		return doDiscover(url);
	}

	@Override
	protected void notify(URL refUrl, NotifyListener listener, List<URL> urls) {
		listener.notify(refUrl, urls);
	}

	/**
	 * 查询group信息、更新缓存，并通知所有对应的订阅方 定时查询服务线程使用
	 * 
	 * @param group
	 * @return
	 */
	private ConcurrentHashMap<String, List<URL>> lookupForUpdate(String group) {
		Long lastConsulIndexId = lookupGroups.get(group) == null ? 0
				: lookupGroups.get(group);
		ConsulResponse<List<ConsulService>> res = lookupConsulService(group,
				lastConsulIndexId);
		if (res != null) {
			List<ConsulService> services = res.getValue();
			if (services != null && !services.isEmpty()
					&& res.getConsulIndex() > lastConsulIndexId) {

				// 将group内所有的url按cluster进行分类
				ConcurrentHashMap<String, List<URL>> groupUrls = new ConcurrentHashMap<String, List<URL>>();
				for (ConsulService hservice : services) {
					try {
						URL url = buildUrl(hservice);
						String cluster = getUrlClusterInfo(url);
						List<URL> urlList = groupUrls.get(cluster);
						if (urlList == null) {
							urlList = new ArrayList<URL>();
							groupUrls.put(cluster, urlList);
						}
						urlList.add(url);
					} catch (Exception e) {
						LoggerUtil.error(
								"convert consul service to url fail! service:"
										+ hservice, e);
					}
				}
				lookupGroups.put(group, res.getConsulIndex());
				return groupUrls;
			} else {
				LoggerUtil.info(group + " no need update, lastIndex:"
						+ lastConsulIndexId);
			}
		}
		return null;
	}

	/**
	 * 直接获取consul service数据。 返回值为consul response，可以从其中获取对应indexid
	 * 
	 * @param serviceName
	 * @return 可能会返回null
	 */
	private ConsulResponse<List<ConsulService>> lookupConsulService(
			String serviceName, long lastConsulIndexId) {

		// 获取可用的service
		ConsulResponse<List<ConsulService>> res = client.lookupHealthService(
				ConsulUtils.convertGroupToServiceName(serviceName),
				lastConsulIndexId);
		return res;

	}

	/**
	 * 更新一个group的缓存, 如果cluster有更新，则更新本地缓存，并通知对应cluster
	 * 
	 * @param group
	 * @param groupUrls
	 */
	private void updateServicesCache(String group,
			ConcurrentHashMap<String, List<URL>> groupUrls, boolean needNotify) {
		if (groupUrls != null && !groupUrls.isEmpty()) {
			ConcurrentHashMap<String, List<URL>> groupMap = servicesCache
					.get(group);
			if (groupMap == null) {
				servicesCache.put(group, groupUrls);
			}
			for (Entry<String, List<URL>> entry : groupUrls.entrySet()) {
				boolean change = true;
				if (groupMap != null) {
					List<URL> oldUrls = groupMap.get(entry.getKey());
					List<URL> newUrls = entry.getValue();
					if (newUrls == null || newUrls.isEmpty()
							|| isSame(entry.getValue(), oldUrls)) {
						change = false; // 不需要更新
					} else {
						groupMap.put(entry.getKey(), newUrls);
					}
				}
				if (change && needNotify) {
					notifyExecutor.execute(new NotifyJob(entry.getKey(), entry
							.getValue()));
					LoggerUtil.info("motan service notify-cluster: "
							+ entry.getKey());
					StringBuilder sb = new StringBuilder();
					for(URL url : entry.getValue()){
						sb.append(url.getUri()).append(";");
					}
					LoggerUtil.info("consul notify urls:" + sb.toString());
				}
			}
		}

	}

	/**
	 * 判断两个list中到url是否一致。 如果任意一个list为空，则返回false； 此方法并未做严格互相判等
	 * 
	 * @param urls1
	 * @param urls2
	 * @return
	 */
	private boolean isSame(List<URL> urls1, List<URL> urls2) {
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
	private static ConsulService buildService(URL url) {
		ConsulService service = new ConsulService();
		service.setAddress(url.getHost());
		service.setId(ConsulUtils.convertConsulSerivceId(url));
		service.setName(ConsulUtils.convertGroupToServiceName(url.getGroup()));
		service.setPort(url.getPort());
		service.setTtl(ConsulConstants.TTL);
		List<String> tags = new ArrayList<String>();
		tags.add(ConsulConstants.CONSUL_TAG_MOTAN_PROTOCOL + url.getProtocol());
		service.setTags(tags);
		return service;
	}

	/**
	 * 根据service生成motan使用的
	 * 
	 * @param service
	 * @return
	 */
	private URL buildUrl(ConsulService service) {
		String group = service.getName().substring(
				ConsulConstants.CONSUL_SERVICE_MOTAN_PRE.length());
		Map<String, String> params = new HashMap<String, String>();
		params.put(URLParamType.group.getName(), group);
		params.put(URLParamType.nodeType.getName(),
				MotanConstants.NODE_TYPE_SERVICE);

		String protocol = ConsulUtils.getProtocolFromTag(service.getTags().get(
				0));
		URL url = new URL(protocol, service.getAddress(), service.getPort(),
				ConsulUtils.getPathFromServiceId(service.getId()), params);
		return url;
	}

	/**
	 * 根据url获取cluster信息，cluster 信息包括协议和path（rpc服务中的接口类）。
	 * 
	 * @param url
	 * @return
	 */
	private String getUrlClusterInfo(URL url) {
		return url.getProtocol() + "-" + url.getPath();
	}

	class GroupLookupThread extends Thread {
		private String group;

		public GroupLookupThread(String group) {
			super();
			this.group = group;
		}

		@Override
		public void run() {
			LoggerUtil.info("start group lookup thread. lookup interval: " + lookupInterval + "ms, group: " + group);
			while (true) {
				try {
					sleep(lookupInterval);
					ConcurrentHashMap<String, List<URL>> groupUrls = lookupForUpdate(group);
					updateServicesCache(group, groupUrls, true);

				} catch (Throwable e) {
					LoggerUtil.error("group lookup thread fail!", e);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException ignored) {
					}
				}
			}

		}

	}

	class NotifyJob implements Runnable {
		private String Cluster;
		private List<URL> urls;

		public NotifyJob(String cluster, List<URL> urls) {
			super();
			Cluster = cluster;
			this.urls = urls;
		}

		@Override
		public void run() {
			HashMap<URL, NotifyListener> listeners = subscribeListeners
					.get(Cluster);
			synchronized (listeners) {
				for (Entry<URL, NotifyListener> entry : listeners.entrySet()) {
					ConsulRegistry.this.notify(entry.getKey(),
							entry.getValue(), urls);
				}
			}
		}

	}

	public void setConsulClient(MotanConsulClient client) {
		this.client = client;
		this.heartbeatManager.setClient(client);
	}
	
	//重新对jvm中注册过的consul service进行注册。因本地agent异常导致已注册server丢失的情况使用。
	public void reRegister(){
		for(URL url:getRegisteredServiceUrls()) {
			ConsulService service = buildService(url);
			client.registerService(service);
		}
	}

}
