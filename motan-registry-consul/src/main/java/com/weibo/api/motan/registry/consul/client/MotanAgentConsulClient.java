package com.weibo.api.motan.registry.consul.client;

import com.ecwid.consul.SingleUrlParameters;
import com.ecwid.consul.UrlParameters;
import com.ecwid.consul.json.GsonFactory;
import com.ecwid.consul.transport.RawResponse;
import com.ecwid.consul.transport.TLSConfig;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.OperationException;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.AgentClient;
import com.ecwid.consul.v1.agent.model.*;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Map;

/**
 * @author Michael Yang 杨福海 （fuhai999@gmail.com）
 * @version V1.0
 */
public class MotanAgentConsulClient implements AgentClient {

    private final ConsulRawClient rawClient;

    public MotanAgentConsulClient(ConsulRawClient rawClient) {
        this.rawClient = rawClient;
    }

    public MotanAgentConsulClient() {
        this(new ConsulRawClient());
    }

    public MotanAgentConsulClient(TLSConfig tlsConfig) {
        this(new ConsulRawClient(tlsConfig));
    }

    public MotanAgentConsulClient(String agentHost) {
        this(new ConsulRawClient(agentHost));
    }

    public MotanAgentConsulClient(String agentHost, TLSConfig tlsConfig) {
        this(new ConsulRawClient(agentHost, tlsConfig));
    }

    public MotanAgentConsulClient(String agentHost, int agentPort) {
        this(new ConsulRawClient(agentHost, agentPort));
    }

    public MotanAgentConsulClient(String agentHost, int agentPort, TLSConfig tlsConfig) {
        this(new ConsulRawClient(agentHost, agentPort, tlsConfig));
    }

    @Override
    public Response<Map<String, Check>> getAgentChecks() {
        RawResponse rawResponse = rawClient.makeGetRequest("/v1/agent/checks");

        if (rawResponse.getStatusCode() == 200) {
            Map<String, Check> value = GsonFactory.getGson().fromJson(rawResponse.getContent(), new TypeToken<Map<String, Check>>() {
            }.getType());
            return new Response<Map<String, Check>>(value, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<Map<String, Service>> getAgentServices() {
        RawResponse rawResponse = rawClient.makeGetRequest("/v1/agent/services");

        if (rawResponse.getStatusCode() == 200) {
            Map<String, Service> agentServices = GsonFactory.getGson().fromJson(rawResponse.getContent(),
                    new TypeToken<Map<String, Service>>() {
                    }.getType());
            return new Response<Map<String, Service>>(agentServices, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<List<Member>> getAgentMembers() {
        RawResponse rawResponse = rawClient.makeGetRequest("/v1/agent/members");

        if (rawResponse.getStatusCode() == 200) {
            List<Member> members = GsonFactory.getGson().fromJson(rawResponse.getContent(), new TypeToken<List<Member>>() {
            }.getType());
            return new Response<List<Member>>(members, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<Self> getAgentSelf() {
        return getAgentSelf(null);
    }

    @Override
    public Response<Self> getAgentSelf(String token) {
        UrlParameters tokenParam = token != null ? new SingleUrlParameters("token", token) : null;

        RawResponse rawResponse = rawClient.makeGetRequest("/v1/agent/self", tokenParam);

        if (rawResponse.getStatusCode() == 200) {
            Self self = GsonFactory.getGson().fromJson(rawResponse.getContent(), Self.class);
            return new Response<Self>(self, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<Void> agentSetMaintenance(boolean maintenanceEnabled) {
        return agentSetMaintenance(maintenanceEnabled, null);
    }

    @Override
    public Response<Void> agentSetMaintenance(boolean maintenanceEnabled, String reason) {
        UrlParameters maintenanceParameter = new SingleUrlParameters("enable", Boolean.toString(maintenanceEnabled));
        UrlParameters reasonParamenter = reason != null ? new SingleUrlParameters("reason", reason) : null;

        RawResponse rawResponse = rawClient.makePutRequest("/v1/agent/maintenance", "", maintenanceParameter, reasonParamenter);

        if (rawResponse.getStatusCode() == 200) {
            return new Response<Void>(null, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }

    }

    @Override
    public Response<Void> agentJoin(String address, boolean wan) {
        UrlParameters wanParams = wan ? new SingleUrlParameters("wan", "1") : null;
        RawResponse rawResponse = rawClient.makePutRequest("/v1/agent/join/" + address, "", wanParams);

        if (rawResponse.getStatusCode() == 200) {
            return new Response<Void>(null, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<Void> agentForceLeave(String node) {
        RawResponse rawResponse = rawClient.makePutRequest("/v1/agent/force-leave/" + node, "");

        if (rawResponse.getStatusCode() == 200) {
            return new Response<Void>(null, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<Void> agentCheckRegister(NewCheck newCheck) {
        return agentCheckRegister(newCheck, null);
    }

    @Override
    public Response<Void> agentCheckRegister(NewCheck newCheck, String token) {
        UrlParameters tokenParam = token != null ? new SingleUrlParameters("token", token) : null;

        String json = GsonFactory.getGson().toJson(newCheck);
        RawResponse rawResponse = rawClient.makePutRequest("/v1/agent/check/register", json, tokenParam);

        if (rawResponse.getStatusCode() == 200) {
            return new Response<Void>(null, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<Void> agentCheckDeregister(String checkId) {
        RawResponse rawResponse = rawClient.makePutRequest("/v1/agent/check/deregister/" + checkId, "");

        if (rawResponse.getStatusCode() == 200) {
            return new Response<Void>(null, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<Void> agentCheckPass(String checkId) {
        return agentCheckPass(checkId, null);
    }

    @Override
    public Response<Void> agentCheckPass(String checkId, String note) {
        UrlParameters noteParams = note != null ? new SingleUrlParameters("note", note) : null;
        RawResponse rawResponse = rawClient.makePutRequest("/v1/agent/check/pass/" + checkId, "", noteParams);

        if (rawResponse.getStatusCode() == 200) {
            return new Response<Void>(null, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<Void> agentCheckWarn(String checkId) {
        return agentCheckWarn(checkId, null);
    }

    @Override
    public Response<Void> agentCheckWarn(String checkId, String note) {
        UrlParameters noteParams = note != null ? new SingleUrlParameters("note", note) : null;
        RawResponse rawResponse = rawClient.makePutRequest("/v1/agent/check/warn/" + checkId, "", noteParams);

        if (rawResponse.getStatusCode() == 200) {
            return new Response<Void>(null, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<Void> agentCheckFail(String checkId) {
        return agentCheckFail(checkId, null);
    }

    @Override
    public Response<Void> agentCheckFail(String checkId, String note) {
        UrlParameters noteParams = note != null ? new SingleUrlParameters("note", note) : null;
        RawResponse rawResponse = rawClient.makePutRequest("/v1/agent/check/fail/" + checkId, "", noteParams);

        if (rawResponse.getStatusCode() == 200) {
            return new Response<Void>(null, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<Void> agentServiceRegister(NewService newService) {
        return agentServiceRegister(newService, null);
    }

    @Override
    public Response<Void> agentServiceRegister(NewService newService, String token) {
        UrlParameters tokenParam = token != null ? new SingleUrlParameters("token", token) : null;

        String json = GsonFactory.getGson().toJson(newService);
        RawResponse rawResponse = rawClient.makePutRequest("/v1/agent/service/register", json, tokenParam);

        if (rawResponse.getStatusCode() == 200) {
            return new Response<Void>(null, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<Void> agentServiceDeregister(String serviceId) {
        return agentServiceDeregister(serviceId, null);
    }

    @Override
    public Response<Void> agentServiceDeregister(String serviceId, String token) {
        UrlParameters tokenParam = token != null ? new SingleUrlParameters("token", token) : null;

        RawResponse rawResponse = rawClient.makePutRequest("/v1/agent/service/deregister/" + serviceId, "", tokenParam);

        if (rawResponse.getStatusCode() == 200) {
            return new Response<Void>(null, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<Void> agentServiceSetMaintenance(String serviceId, boolean maintenanceEnabled) {
        return agentServiceSetMaintenance(serviceId, maintenanceEnabled, null);
    }

    @Override
    public Response<Void> agentServiceSetMaintenance(String serviceId, boolean maintenanceEnabled, String reason) {
        UrlParameters maintenanceParameter = new SingleUrlParameters("enable", Boolean.toString(maintenanceEnabled));
        UrlParameters reasonParameter = reason != null ? new SingleUrlParameters("reason", reason) : null;

        RawResponse rawResponse = rawClient.makePutRequest("/v1/agent/service/maintenance/" + serviceId, "", maintenanceParameter, reasonParameter);

        if (rawResponse.getStatusCode() == 200) {
            return new Response<Void>(null, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<Void> agentReload() {
        RawResponse rawResponse = rawClient.makePutRequest("/v1/agent/reload", "");

        if (rawResponse.getStatusCode() == 200) {
            return new Response<Void>(null, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }

    }
}
