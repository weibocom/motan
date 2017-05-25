package com.weibo.api.motan.registry.consul;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.weibo.api.motan.registry.consul.client.MotanConsulClient;
import com.weibo.api.motan.util.ConcurrentHashSet;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanSwitcherUtil;

/**
 * consul 心跳管理类。 rpc服务把需要设置passing状态的serviceid注册到此类，
 * 此类会定时对注册的serviceid设置passing状态（实际是对servcieid对应对checkid设置passing状态），
 * 从而完成servivce的心跳。
 * 开关开启后会进行心跳，开关关闭则停止心跳。
 * 
 * @author zhanglei
 *
 */
public class ConsulHeartbeatManager {
	private MotanConsulClient client;
	// 所有需要进行心跳的serviceid.
	private ConcurrentHashSet<String> serviceIds = new ConcurrentHashSet<String>();

	private ThreadPoolExecutor jobExecutor;
	private ScheduledExecutorService heartbeatExecutor;
	// 上一次心跳开关的状态
	private boolean lastHeartBeatSwitcherStatus = false;
	private volatile boolean currentHeartBeatSwitcherStatus = false;
	// 开关检查次数。
	private int switcherCheckTimes = 0;

	public ConsulHeartbeatManager(MotanConsulClient client) {
		this.client = client;
		heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
		ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(
				10000);
		jobExecutor = new ThreadPoolExecutor(5, 30, 30 * 1000,
				TimeUnit.MILLISECONDS, workQueue);
	}

	public void start() {
		heartbeatExecutor.scheduleAtFixedRate(
				new Runnable() {
					@Override
					public void run() {
						// 由于consul的check set pass会导致consul
						// server的写磁盘操作，过于频繁的心跳会导致consul
						// 性能问题，只能将心跳方式改为较长的周期进行一次探测。又因为想在关闭心跳开关后尽快感知
						// 就将心跳改为以较小周期检测心跳开关是否变动，连续检测多次后给consul server发送一次心跳。
						// TODO 改为开关listener方式。
						try {
							boolean switcherStatus = isHeartbeatOpen();
							if (isSwitcherChange(switcherStatus)) { // 心跳开关状态变更
								processHeartbeat(switcherStatus);
							} else {// 心跳开关状态未变更
								if (switcherStatus) {// 开关为开启状态，则连续检测超过MAX_SWITCHER_CHECK_TIMES次发送一次心跳
									switcherCheckTimes++;
									if (switcherCheckTimes >= ConsulConstants.MAX_SWITCHER_CHECK_TIMES) {
										processHeartbeat(true);
										switcherCheckTimes = 0;
									}
								}
							}

						} catch (Exception e) {
							LoggerUtil.error("consul heartbeat executor err:",
									e);
						}
					}
				}, ConsulConstants.SWITCHER_CHECK_CIRCLE,
				ConsulConstants.SWITCHER_CHECK_CIRCLE, TimeUnit.MILLISECONDS);
	}

	/**
	 * 判断心跳开关状态是否改变，如果心跳开关改变则更新lastHeartBeatSwitcherStatus为最新状态
	 * 
	 * @param switcherStatus
	 * @return
	 */
	private boolean isSwitcherChange(boolean switcherStatus) {
		boolean ret = false;
		if (switcherStatus != lastHeartBeatSwitcherStatus) {
			ret = true;
			lastHeartBeatSwitcherStatus = switcherStatus;
			LoggerUtil.info("heartbeat switcher change to " + switcherStatus);
		}
		return ret;
	}

	protected void processHeartbeat(boolean isPass) {
		for (String serviceid : serviceIds) {
			try {
				jobExecutor.execute(new HeartbeatJob(serviceid, isPass));
			} catch (RejectedExecutionException ree) {
				LoggerUtil.error("execute heartbeat job fail! serviceid:"
						+ serviceid + " is rejected");
			}
		}
	}

	public void close() {
		heartbeatExecutor.shutdown();
		jobExecutor.shutdown();
		LoggerUtil.info("Consul heartbeatManager closed.");
	}

	/**
	 * 添加consul serviceid，添加后的serviceid会通过定时设置passing状态保持心跳。
	 * 
	 * @param serviceid
	 */
	public void addHeartbeatServcieId(String serviceid) {
		serviceIds.add(serviceid);
	}

	/**
	 * 移除serviceid，对应的serviceid不会在进行心跳。
	 * 
	 * @param serviceid
	 */
	public void removeHeartbeatServiceId(String serviceid) {
		serviceIds.remove(serviceid);
	}

	// 检查心跳开关是否打开
	private boolean isHeartbeatOpen() {
		return currentHeartBeatSwitcherStatus;
	}

	public void setHeartbeatOpen(boolean open) {
		currentHeartBeatSwitcherStatus = open;
	}

	class HeartbeatJob implements Runnable {
		private String serviceid;
		private boolean isPass;

		public HeartbeatJob(String serviceid, boolean isPass) {
			super();
			this.serviceid = serviceid;
			this.isPass = isPass;
		}

		@Override
		public void run() {
			try {
				if (isPass) {
					client.checkPass(serviceid);
				} else {
					client.checkFail(serviceid);
				}
			} catch (Exception e) {
				LoggerUtil.error(
						"consul heartbeat-set check pass error!serviceid:"
								+ serviceid, e);
			}

		}

	}

	public void setClient(MotanConsulClient client) {
		this.client = client;
	}


}
