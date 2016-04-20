package com.weibo.api.motan.registry.consul;

import com.weibo.api.motan.registry.consul.client.MotanConsulClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    // 保存注册过的mock service
    private Set<String> registerServices = new HashSet<String>();

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
    }

    @Override
    public void registerService(ConsulService service) {
        registerServices.add(service.getId());
        System.out.println("mock register service success! serviceid :" + service.getId());

    }

    @Override
    public void deregisterService(String serviceid) {
        registerServices.remove(serviceid);
        System.out.println("mock deregister service success! serviceid :" + serviceid);
    }

    @Override
    public ConsulResponse<List<ConsulService>> lookupHealthService(String serviceName, long lastConsulIndex) {
        ConsulResponse<List<ConsulService>> res = new ConsulResponse<List<ConsulService>>();
        res.setConsulIndex(++mockIndex);
        res.setConsulKnownLeader(true);
        res.setConsulLastContact(0l);

        List<ConsulService> list = new ArrayList<ConsulService>();
        for (int i = 0; i < mockServiceNum; i++) {
            list.add(MockUtils.getMockService(i));
        }
        res.setValue(list);
        return res;
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
    public boolean isServiceRegister(String serviceid) {
        return registerServices.contains(serviceid);
    }

    public void removeService(String serviceid) {
        registerServices.remove(serviceid);
    }


    public long getMockIndex() {
        return mockIndex;
    }

    @Override
    public void checkFail(String serviceid) {

    }

}
