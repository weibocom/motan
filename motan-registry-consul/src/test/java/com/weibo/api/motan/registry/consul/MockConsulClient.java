package com.weibo.api.motan.registry.consul;

import com.weibo.api.motan.registry.consul.client.MotanConsulClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zhanglei28
 * @Description MockConsulClient
 * @date 2016年3月22日
 */
public class MockConsulClient extends MotanConsulClient {
    // 保存各个mock service的心跳次数。
    private ConcurrentHashMap<String, AtomicLong> checkPassTimesMap = new ConcurrentHashMap<String, AtomicLong>();

    // 保存注册过的service和service工作状态，true表示正常，false表示不对外提供服务
    private ConcurrentHashMap<String, Boolean> serviceStatus = new ConcurrentHashMap<String, Boolean>();
    // 保存注册的serviceid与service对应关系
    private ConcurrentHashMap<String, ConsulService> services = new ConcurrentHashMap<String, ConsulService>();
    // 保存KVValue
    private ConcurrentHashMap<String, String> KVValues = new ConcurrentHashMap<String, String>();

    private int mockServiceNum = 10;// 获取服务时，返回的mock service数量
    private long mockIndex = 10;

    public MockConsulClient(String host, int port) {
        super(host, port);
    }

    @Override
    public void checkPass(String serviceid) {
        AtomicLong times = checkPassTimesMap.get(serviceid);
        if (times == null) {
            checkPassTimesMap.putIfAbsent(serviceid, new AtomicLong());
            times = checkPassTimesMap.get(serviceid);
        }
        times.getAndIncrement();

        serviceStatus.put(serviceid, true);
    }

    @Override
    public void checkFail(String serviceid) {
        serviceStatus.put(serviceid, false);
    }

    @Override
    public void registerService(ConsulService service) {
        serviceStatus.put(service.getId(), false);
        services.put(service.getId(), service);
    }

    @Override
    public void unregisterService(String serviceid) {
        serviceStatus.remove(serviceid);
        services.remove(serviceid);
    }

    @Override
    public ConsulResponse<List<ConsulService>> lookupHealthService(String serviceName, long lastConsulIndex) {
        ConsulResponse<List<ConsulService>> res = new ConsulResponse<List<ConsulService>>();
        res.setConsulIndex(lastConsulIndex + 1);
        res.setConsulKnownLeader(true);
        res.setConsulLastContact(0L);

        List<ConsulService> list = new ArrayList<ConsulService>();
        for (Map.Entry<String, Boolean> entry : serviceStatus.entrySet()) {
            if (entry.getValue()) {
                list.add(services.get(entry.getKey()));
            }
        }
        res.setValue(list);
        return res;
    }

    @Override
    public String lookupCommand(String group) {
        String command = KVValues.get(group);
        if (command == null) {
            command = "";
        }
        return command;
    }

    public long getCheckPassTimes(String serviceid) {
        AtomicLong times = checkPassTimesMap.get(serviceid);
        if (times == null) {
            return 0;
        }
        return times.get();
    }

    public int getMockServiceNum() {
        return mockServiceNum;
    }

    public void setMockServiceNum(int mockServiceNum) {
        this.mockServiceNum = mockServiceNum;
    }

    /**
     * 查看给定的serviceid是否是已注册状态
     *
     * @param serviceid
     * @return
     */
    public boolean isRegistered(String serviceid) {
        return serviceStatus.containsKey(serviceid);
    }

    public boolean isWorking(String serviceid) {
        return serviceStatus.get(serviceid);
    }

    public void removeService(String serviceid) {
        serviceStatus.remove(serviceid);
        services.remove(serviceid);
    }


    public long getMockIndex() {
        return mockIndex;
    }

    public void setKVValue(String key, String value) {
        KVValues.put(key, value);
    }

    public void removeKVValue(String key) {
        KVValues.remove(key);
    }

}
