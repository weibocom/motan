package com.weibo.api.motan.registry.consul.client;

import com.ecwid.consul.transport.TLSConfig;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.AgentClient;
import com.ecwid.consul.v1.agent.model.*;

import java.util.List;
import java.util.Map;

/**
 * @author Michael Yang 杨福海 （fuhai999@gmail.com）
 * @version V1.0
 * @Package com.weibo.api.motan.registry.consul.client
 */
public class MotanEcwidConsulClient extends ConsulClient {


    private final AgentClient agentClient;

    public MotanEcwidConsulClient(ConsulRawClient rawClient) {
        super(rawClient);
        agentClient = new MotanAgentConsulClient(rawClient);
    }

    /**
     * Consul client will connect to local consul agent on
     * 'http://localhost:8500'
     */
    public MotanEcwidConsulClient() {
        this(new ConsulRawClient());
    }

    /**
     * Consul client will connect to local consul agent on
     * 'http://localhost:8500'
     *
     * @param tlsConfig TLS configuration
     */
    public MotanEcwidConsulClient(TLSConfig tlsConfig) {
        this(new ConsulRawClient(tlsConfig));
    }

    /**
     * Connect to consul agent on specific address and default port (8500)
     *
     * @param agentHost Hostname or IP address of consul agent. You can specify scheme
     *                  (HTTP/HTTPS) in address. If there is no scheme in address -
     *                  client will use HTTP.
     */
    public MotanEcwidConsulClient(String agentHost) {
        this(new ConsulRawClient(agentHost));
    }

    /**
     * Connect to consul agent on specific address and default port (8500)
     *
     * @param agentHost Hostname or IP address of consul agent. You can specify scheme
     *                  (HTTP/HTTPS) in address. If there is no scheme in address -
     *                  client will use HTTP.
     * @param tlsConfig TLS configuration
     */
    public MotanEcwidConsulClient(String agentHost, TLSConfig tlsConfig) {
        this(new ConsulRawClient(agentHost, tlsConfig));
    }

    /**
     * Connect to consul agent on specific address and port
     *
     * @param agentHost Hostname or IP address of consul agent. You can specify scheme
     *                  (HTTP/HTTPS) in address. If there is no scheme in address -
     *                  client will use HTTP.
     * @param agentPort Consul agent port
     */
    public MotanEcwidConsulClient(String agentHost, int agentPort) {
        this(new ConsulRawClient(agentHost, agentPort));
    }

    /**
     * Connect to consul agent on specific address and port
     *
     * @param agentHost Hostname or IP address of consul agent. You can specify scheme
     *                  (HTTP/HTTPS) in address. If there is no scheme in address -
     *                  client will use HTTP.
     * @param agentPort Consul agent port
     * @param tlsConfig TLS configuration
     */
    public MotanEcwidConsulClient(String agentHost, int agentPort, TLSConfig tlsConfig) {
        this(new ConsulRawClient(agentHost, agentPort, tlsConfig));
    }


    // -------------------------------------------------------------------------------------------
    // Agent

    @Override
    public Response<Map<String, Check>> getAgentChecks() {
        return agentClient.getAgentChecks();
    }

    @Override
    public Response<Map<String, Service>> getAgentServices() {
        return agentClient.getAgentServices();
    }

    @Override
    public Response<List<Member>> getAgentMembers() {
        return agentClient.getAgentMembers();
    }

    @Override
    public Response<Self> getAgentSelf() {
        return agentClient.getAgentSelf();
    }

    @Override
    public Response<Void> agentSetMaintenance(boolean maintenanceEnabled) {
        return agentClient.agentSetMaintenance(maintenanceEnabled);
    }

    @Override
    public Response<Void> agentSetMaintenance(boolean maintenanceEnabled, String reason) {
        return agentClient.agentSetMaintenance(maintenanceEnabled, reason);
    }

    @Override
    public Response<Void> agentJoin(String address, boolean wan) {
        return agentClient.agentJoin(address, wan);
    }

    @Override
    public Response<Void> agentForceLeave(String node) {
        return agentClient.agentForceLeave(node);
    }

    @Override
    public Response<Void> agentCheckRegister(NewCheck newCheck) {
        return agentClient.agentCheckRegister(newCheck);
    }

    @Override
    public Response<Void> agentCheckRegister(NewCheck newCheck, String token) {
        return agentClient.agentCheckRegister(newCheck, token);
    }

    @Override
    public Response<Void> agentCheckDeregister(String checkId) {
        return agentClient.agentCheckDeregister(checkId);
    }

    @Override
    public Response<Void> agentCheckPass(String checkId) {
        return agentClient.agentCheckPass(checkId);
    }

    @Override
    public Response<Void> agentCheckPass(String checkId, String note) {
        return agentClient.agentCheckPass(checkId, note);
    }

    @Override
    public Response<Void> agentCheckWarn(String checkId) {
        return agentClient.agentCheckWarn(checkId);
    }

    @Override
    public Response<Void> agentCheckWarn(String checkId, String note) {
        return agentClient.agentCheckWarn(checkId, note);
    }

    @Override
    public Response<Void> agentCheckFail(String checkId) {
        return agentClient.agentCheckFail(checkId);
    }

    @Override
    public Response<Void> agentCheckFail(String checkId, String note) {
        return agentClient.agentCheckFail(checkId, note);
    }

    @Override
    public Response<Void> agentServiceRegister(NewService newService) {
        return agentClient.agentServiceRegister(newService);
    }

    @Override
    public Response<Void> agentServiceRegister(NewService newService, String token) {
        return agentClient.agentServiceRegister(newService, token);
    }

    @Override
    public Response<Void> agentServiceDeregister(String serviceId) {
        return agentClient.agentServiceDeregister(serviceId);
    }

    @Override
    public Response<Void> agentServiceSetMaintenance(String serviceId, boolean maintenanceEnabled) {
        return agentClient.agentServiceSetMaintenance(serviceId, maintenanceEnabled);
    }

    @Override
    public Response<Void> agentServiceSetMaintenance(String serviceId, boolean maintenanceEnabled, String reason) {
        return agentClient.agentServiceSetMaintenance(serviceId, maintenanceEnabled, reason);
    }

}
