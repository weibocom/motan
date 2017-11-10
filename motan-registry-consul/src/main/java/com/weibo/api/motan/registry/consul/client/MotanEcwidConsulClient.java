package com.weibo.api.motan.registry.consul.client;

import com.ecwid.consul.transport.TLSConfig;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.acl.AclClient;
import com.ecwid.consul.v1.acl.AclConsulClient;
import com.ecwid.consul.v1.acl.model.Acl;
import com.ecwid.consul.v1.acl.model.NewAcl;
import com.ecwid.consul.v1.acl.model.UpdateAcl;
import com.ecwid.consul.v1.agent.AgentClient;
import com.ecwid.consul.v1.agent.model.*;
import com.ecwid.consul.v1.catalog.CatalogClient;
import com.ecwid.consul.v1.catalog.CatalogConsulClient;
import com.ecwid.consul.v1.catalog.model.*;
import com.ecwid.consul.v1.coordinate.CoordinateClient;
import com.ecwid.consul.v1.coordinate.CoordinateConsulClient;
import com.ecwid.consul.v1.coordinate.model.Datacenter;
import com.ecwid.consul.v1.event.EventClient;
import com.ecwid.consul.v1.event.EventConsulClient;
import com.ecwid.consul.v1.event.model.Event;
import com.ecwid.consul.v1.event.model.EventParams;
import com.ecwid.consul.v1.health.HealthClient;
import com.ecwid.consul.v1.health.HealthConsulClient;
import com.ecwid.consul.v1.health.model.HealthService;
import com.ecwid.consul.v1.kv.KeyValueClient;
import com.ecwid.consul.v1.kv.KeyValueConsulClient;
import com.ecwid.consul.v1.kv.model.GetBinaryValue;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.ecwid.consul.v1.kv.model.PutParams;
import com.ecwid.consul.v1.session.SessionClient;
import com.ecwid.consul.v1.session.SessionConsulClient;
import com.ecwid.consul.v1.session.model.NewSession;
import com.ecwid.consul.v1.session.model.Session;
import com.ecwid.consul.v1.status.StatusClient;
import com.ecwid.consul.v1.status.StatusConsulClient;

import java.util.List;
import java.util.Map;

/**
 * @author Michael Yang 杨福海 （fuhai999@gmail.com）
 * @version V1.0
 * @Package com.weibo.api.motan.registry.consul.client
 */
public class MotanEcwidConsulClient extends ConsulClient {


    private final AclClient aclClient;
    private final AgentClient agentClient;
    private final CatalogClient catalogClient;
    private final CoordinateClient coordinateClient;
    private final EventClient eventClient;
    private final HealthClient healthClient;
    private final KeyValueClient keyValueClient;
    private final SessionClient sessionClient;
    private final StatusClient statusClient;

    public MotanEcwidConsulClient(ConsulRawClient rawClient) {
        aclClient = new AclConsulClient(rawClient);
        agentClient = new MotanAgentConsulClient(rawClient);
        catalogClient = new CatalogConsulClient(rawClient);
        coordinateClient = new CoordinateConsulClient(rawClient);
        eventClient = new EventConsulClient(rawClient);
        healthClient = new HealthConsulClient(rawClient);
        keyValueClient = new KeyValueConsulClient(rawClient);
        sessionClient = new SessionConsulClient(rawClient);
        statusClient = new StatusConsulClient(rawClient);
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
    // ACL

    @Override
    public Response<String> aclCreate(NewAcl newAcl, String token) {
        return aclClient.aclCreate(newAcl, token);
    }

    @Override
    public Response<Void> aclUpdate(UpdateAcl updateAcl, String token) {
        return aclClient.aclUpdate(updateAcl, token);
    }

    @Override
    public Response<Void> aclDestroy(String aclId, String token) {
        return aclClient.aclDestroy(aclId, token);
    }

    @Override
    public Response<Acl> getAcl(String id) {
        return aclClient.getAcl(id);
    }

    @Override
    public Response<String> aclClone(String aclId, String token) {
        return aclClient.aclClone(aclId, token);
    }

    @Override
    public Response<List<Acl>> getAclList(String token) {
        return aclClient.getAclList(token);
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

    // -------------------------------------------------------------------------------------------
    // Catalog

    @Override
    public Response<Void> catalogRegister(CatalogRegistration catalogRegistration) {
        return catalogClient.catalogRegister(catalogRegistration);
    }

    @Override
    public Response<Void> catalogDeregister(CatalogDeregistration catalogDeregistration) {
        return catalogClient.catalogDeregister(catalogDeregistration);
    }

    @Override
    public Response<List<String>> getCatalogDatacenters() {
        return catalogClient.getCatalogDatacenters();
    }

    @Override
    public Response<List<Node>> getCatalogNodes(QueryParams queryParams) {
        return catalogClient.getCatalogNodes(queryParams);
    }

    @Override
    public Response<Map<String, List<String>>> getCatalogServices(QueryParams queryParams) {
        return catalogClient.getCatalogServices(queryParams);
    }

    @Override
    public Response<Map<String, List<String>>> getCatalogServices(QueryParams queryParams, String token) {
        return catalogClient.getCatalogServices(queryParams, token);
    }

    @Override
    public Response<List<CatalogService>> getCatalogService(String serviceName, QueryParams queryParams) {
        return catalogClient.getCatalogService(serviceName, queryParams);
    }

    @Override
    public Response<List<CatalogService>> getCatalogService(String serviceName, String tag, QueryParams queryParams) {
        return catalogClient.getCatalogService(serviceName, tag, queryParams);
    }

    @Override
    public Response<List<CatalogService>> getCatalogService(String serviceName, QueryParams queryParams, String token) {
        return catalogClient.getCatalogService(serviceName, queryParams, token);
    }

    @Override
    public Response<List<CatalogService>> getCatalogService(String serviceName, String tag, QueryParams queryParams, String token) {
        return catalogClient.getCatalogService(serviceName, tag, queryParams, token);
    }

    @Override
    public Response<CatalogNode> getCatalogNode(String nodeName, QueryParams queryParams) {
        return catalogClient.getCatalogNode(nodeName, queryParams);
    }

    // -------------------------------------------------------------------------------------------
    // Coordinates

    @Override
    public Response<List<Datacenter>> getDatacenters() {
        return coordinateClient.getDatacenters();
    }

    @Override
    public Response<List<com.ecwid.consul.v1.coordinate.model.Node>> getNodes(QueryParams queryParams) {
        return coordinateClient.getNodes(queryParams);
    }

    // -------------------------------------------------------------------------------------------
    // Event

    @Override
    public Response<Event> eventFire(String event, String payload, EventParams eventParams, QueryParams queryParams) {
        return eventClient.eventFire(event, payload, eventParams, queryParams);
    }

    @Override
    public Response<List<Event>> eventList(QueryParams queryParams) {
        return eventClient.eventList(queryParams);
    }

    @Override
    public Response<List<Event>> eventList(String event, QueryParams queryParams) {
        return eventClient.eventList(event, queryParams);
    }

    // -------------------------------------------------------------------------------------------
    // Health

    @Override
    public Response<List<com.ecwid.consul.v1.health.model.Check>> getHealthChecksForNode(String nodeName, QueryParams queryParams) {
        return healthClient.getHealthChecksForNode(nodeName, queryParams);
    }

    @Override
    public Response<List<com.ecwid.consul.v1.health.model.Check>> getHealthChecksForService(String serviceName, QueryParams queryParams) {
        return healthClient.getHealthChecksForService(serviceName, queryParams);
    }

    @Override
    public Response<List<HealthService>> getHealthServices(String serviceName, boolean onlyPassing, QueryParams queryParams) {
        return healthClient.getHealthServices(serviceName, onlyPassing, queryParams);
    }

    @Override
    public Response<List<HealthService>> getHealthServices(String serviceName, String tag, boolean onlyPassing, QueryParams queryParams) {
        return healthClient.getHealthServices(serviceName, tag, onlyPassing, queryParams);
    }

    @Override
    public Response<List<HealthService>> getHealthServices(String serviceName, boolean onlyPassing, QueryParams queryParams, String token) {
        return healthClient.getHealthServices(serviceName, onlyPassing, queryParams, token);
    }

    @Override
    public Response<List<HealthService>> getHealthServices(String serviceName, String tag, boolean onlyPassing, QueryParams queryParams, String token) {
        return healthClient.getHealthServices(serviceName, tag, onlyPassing, queryParams, token);
    }

    @Override
    public Response<List<com.ecwid.consul.v1.health.model.Check>> getHealthChecksState(QueryParams queryParams) {
        return healthClient.getHealthChecksState(queryParams);
    }

    @Override
    public Response<List<com.ecwid.consul.v1.health.model.Check>> getHealthChecksState(com.ecwid.consul.v1.health.model.Check.CheckStatus checkStatus, QueryParams queryParams) {
        return healthClient.getHealthChecksState(checkStatus, queryParams);
    }

    // -------------------------------------------------------------------------------------------
    // KV

    @Override
    public Response<GetValue> getKVValue(String key) {
        return keyValueClient.getKVValue(key);
    }

    @Override
    public Response<GetValue> getKVValue(String key, String token) {
        return keyValueClient.getKVValue(key, token);
    }

    @Override
    public Response<GetValue> getKVValue(String key, QueryParams queryParams) {
        return keyValueClient.getKVValue(key, queryParams);
    }

    @Override
    public Response<GetValue> getKVValue(String key, String token, QueryParams queryParams) {
        return keyValueClient.getKVValue(key, token, queryParams);
    }

    @Override
    public Response<GetBinaryValue> getKVBinaryValue(String key) {
        return keyValueClient.getKVBinaryValue(key);
    }

    @Override
    public Response<GetBinaryValue> getKVBinaryValue(String key, String token) {
        return keyValueClient.getKVBinaryValue(key, token);
    }

    @Override
    public Response<GetBinaryValue> getKVBinaryValue(String key, QueryParams queryParams) {
        return keyValueClient.getKVBinaryValue(key, queryParams);
    }

    @Override
    public Response<GetBinaryValue> getKVBinaryValue(String key, String token, QueryParams queryParams) {
        return keyValueClient.getKVBinaryValue(key, token, queryParams);
    }

    @Override
    public Response<List<GetValue>> getKVValues(String keyPrefix) {
        return keyValueClient.getKVValues(keyPrefix);
    }

    @Override
    public Response<List<GetValue>> getKVValues(String keyPrefix, String token) {
        return keyValueClient.getKVValues(keyPrefix, token);
    }

    @Override
    public Response<List<GetValue>> getKVValues(String keyPrefix, QueryParams queryParams) {
        return keyValueClient.getKVValues(keyPrefix, queryParams);
    }

    @Override
    public Response<List<GetValue>> getKVValues(String keyPrefix, String token, QueryParams queryParams) {
        return keyValueClient.getKVValues(keyPrefix, token, queryParams);
    }

    @Override
    public Response<List<GetBinaryValue>> getKVBinaryValues(String keyPrefix) {
        return keyValueClient.getKVBinaryValues(keyPrefix);
    }

    @Override
    public Response<List<GetBinaryValue>> getKVBinaryValues(String keyPrefix, String token) {
        return keyValueClient.getKVBinaryValues(keyPrefix, token);
    }

    @Override
    public Response<List<GetBinaryValue>> getKVBinaryValues(String keyPrefix, QueryParams queryParams) {
        return keyValueClient.getKVBinaryValues(keyPrefix, queryParams);
    }

    @Override
    public Response<List<GetBinaryValue>> getKVBinaryValues(String keyPrefix, String token, QueryParams queryParams) {
        return keyValueClient.getKVBinaryValues(keyPrefix, token, queryParams);
    }

    @Override
    public Response<List<String>> getKVKeysOnly(String keyPrefix) {
        return keyValueClient.getKVKeysOnly(keyPrefix);
    }

    @Override
    public Response<List<String>> getKVKeysOnly(String keyPrefix, String separator, String token) {
        return keyValueClient.getKVKeysOnly(keyPrefix, separator, token);
    }

    @Override
    public Response<List<String>> getKVKeysOnly(String keyPrefix, QueryParams queryParams) {
        return keyValueClient.getKVKeysOnly(keyPrefix, queryParams);
    }

    @Override
    public Response<List<String>> getKVKeysOnly(String keyPrefix, String separator, String token, QueryParams queryParams) {
        return keyValueClient.getKVKeysOnly(keyPrefix, separator, token, queryParams);
    }

    @Override
    public Response<Boolean> setKVValue(String key, String value) {
        return keyValueClient.setKVValue(key, value);
    }

    @Override
    public Response<Boolean> setKVValue(String key, String value, PutParams putParams) {
        return keyValueClient.setKVValue(key, value, putParams);
    }

    @Override
    public Response<Boolean> setKVValue(String key, String value, String token, PutParams putParams) {
        return keyValueClient.setKVValue(key, value, token, putParams);
    }

    @Override
    public Response<Boolean> setKVValue(String key, String value, QueryParams queryParams) {
        return keyValueClient.setKVValue(key, value, queryParams);
    }

    @Override
    public Response<Boolean> setKVValue(String key, String value, PutParams putParams, QueryParams queryParams) {
        return keyValueClient.setKVValue(key, value, putParams, queryParams);
    }

    @Override
    public Response<Boolean> setKVValue(String key, String value, String token, PutParams putParams, QueryParams queryParams) {
        return keyValueClient.setKVValue(key, value, token, putParams, queryParams);
    }

    @Override
    public Response<Boolean> setKVBinaryValue(String key, byte[] value) {
        return keyValueClient.setKVBinaryValue(key, value);
    }

    @Override
    public Response<Boolean> setKVBinaryValue(String key, byte[] value, PutParams putParams) {
        return keyValueClient.setKVBinaryValue(key, value, putParams);
    }

    @Override
    public Response<Boolean> setKVBinaryValue(String key, byte[] value, String token, PutParams putParams) {
        return keyValueClient.setKVBinaryValue(key, value, token, putParams);
    }

    @Override
    public Response<Boolean> setKVBinaryValue(String key, byte[] value, QueryParams queryParams) {
        return keyValueClient.setKVBinaryValue(key, value, queryParams);
    }

    @Override
    public Response<Boolean> setKVBinaryValue(String key, byte[] value, PutParams putParams, QueryParams queryParams) {
        return keyValueClient.setKVBinaryValue(key, value, putParams, queryParams);
    }

    @Override
    public Response<Boolean> setKVBinaryValue(String key, byte[] value, String token, PutParams putParams, QueryParams queryParams) {
        return keyValueClient.setKVBinaryValue(key, value, token, putParams, queryParams);
    }

    @Override
    public Response<Void> deleteKVValue(String key) {
        return keyValueClient.deleteKVValue(key);
    }

    @Override
    public Response<Void> deleteKVValue(String key, String token) {
        return keyValueClient.deleteKVValue(key, token);
    }

    @Override
    public Response<Void> deleteKVValue(String key, QueryParams queryParams) {
        return keyValueClient.deleteKVValue(key, queryParams);
    }

    @Override
    public Response<Void> deleteKVValue(String key, String token, QueryParams queryParams) {
        return keyValueClient.deleteKVValue(key, token, queryParams);
    }

    @Override
    public Response<Void> deleteKVValues(String key) {
        return keyValueClient.deleteKVValues(key);
    }

    @Override
    public Response<Void> deleteKVValues(String key, String token) {
        return keyValueClient.deleteKVValues(key, token);
    }

    @Override
    public Response<Void> deleteKVValues(String key, QueryParams queryParams) {
        return keyValueClient.deleteKVValues(key, queryParams);
    }

    @Override
    public Response<Void> deleteKVValues(String key, String token, QueryParams queryParams) {
        return keyValueClient.deleteKVValues(key, token, queryParams);
    }

    // -------------------------------------------------------------------------------------------
    // Session

    @Override
    public Response<String> sessionCreate(NewSession newSession, QueryParams queryParams) {
        return sessionClient.sessionCreate(newSession, queryParams);
    }

    @Override
    public Response<Void> sessionDestroy(String session, QueryParams queryParams) {
        return sessionClient.sessionDestroy(session, queryParams);
    }

    @Override
    public Response<Session> getSessionInfo(String session, QueryParams queryParams) {
        return sessionClient.getSessionInfo(session, queryParams);
    }

    @Override
    public Response<List<Session>> getSessionNode(String node, QueryParams queryParams) {
        return sessionClient.getSessionNode(node, queryParams);
    }

    @Override
    public Response<List<Session>> getSessionList(QueryParams queryParams) {
        return sessionClient.getSessionList(queryParams);
    }

    @Override
    public Response<Session> renewSession(String session, QueryParams queryParams) {
        return sessionClient.renewSession(session, queryParams);
    }

    // -------------------------------------------------------------------------------------------
    // Status

    @Override
    public Response<String> getStatusLeader() {
        return statusClient.getStatusLeader();
    }

    @Override
    public Response<List<String>> getStatusPeers() {
        return statusClient.getStatusPeers();
    }
}
