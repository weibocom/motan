package com.weibo.api.motan.registry.mesh;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.LoggerUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * @author sunnights
 */
public class MeshClient {
    private final ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
        @Override
        public String handleResponse(HttpResponse response) throws IOException {
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String content = entity != null ? EntityUtils.toString(entity) : null;
            if (status >= 200 && status < 300) {
                return content;
            } else {
                throw new HttpResponseException(status, String.format("Unexpected response status: %s, content: %s", status, content));
            }
        }
    };
    private CloseableHttpClient httpClient;
    private String registryAdminUrl;
    private String backupRegistryUrl;

    public MeshClient(CloseableHttpClient httpClient, String registryAddress, String backupRegistryUrl) {
        this.httpClient = httpClient;
        this.registryAdminUrl = "http://" + registryAddress;
        this.backupRegistryUrl = backupRegistryUrl;
    }

    public String getBackupRegistryUrl() {
        return backupRegistryUrl;
    }

    private JSONObject buildUrlJsonObj(URL url) {
        JSONObject parameters = (JSONObject) JSON.toJSON(url.getParameters());
        parameters.put("proxyRegistry", backupRegistryUrl);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("protocol", url.getProtocol());
        jsonObject.put("host", url.getHost());
        jsonObject.put("port", url.getPort());
        jsonObject.put("path", url.getPath());
        jsonObject.put("group", url.getGroup());
        jsonObject.put("parameters", parameters);
        return jsonObject;
    }

    public void register(URL url) {
        JSONObject jsonObject = buildUrlJsonObj(url);
        HttpPost httpPost = new HttpPost(registryAdminUrl + "/registry/register");
        httpPost.addHeader("Content-Type", "application/json");
        StringEntity stringEntity = new StringEntity(jsonObject.toString(), "UTF-8");
        httpPost.setEntity(stringEntity);
        String result = "";
        try {
            result = httpClient.execute(httpPost, responseHandler);
            LoggerUtil.info("register service, url={}, result={}", url.toString(), result);
        } catch (Exception e) {
            LoggerUtil.error("register service error, url={}, result={}, e:", url.toString(), result, e);
            throw new MotanFrameworkException("fail to register service");
        }
    }

    public void unregister(URL url) {
        JSONObject jsonObject = buildUrlJsonObj(url);
        HttpPost httpPost = new HttpPost(registryAdminUrl + "/registry/unregister");
        httpPost.addHeader("Content-Type", "application/json");
        StringEntity stringEntity = new StringEntity(jsonObject.toString(), "UTF-8");
        httpPost.setEntity(stringEntity);
        String result = "";
        try {
            result = httpClient.execute(httpPost, responseHandler);
            LoggerUtil.info("unregister service, url={}, result={}", url.toString(), result);
        } catch (Exception e) {
            LoggerUtil.error("unregister service error, url={}, result={}, e:", url.toString(), result, e);
            throw new MotanFrameworkException("fail to unregister service");
        }
    }

    public void subscribe(URL url) {
        JSONObject jsonObject = buildUrlJsonObj(url);
        HttpPost httpPost = new HttpPost(registryAdminUrl + "/registry/subscribe");
        httpPost.addHeader("Content-Type", "application/json");
        StringEntity stringEntity = new StringEntity(jsonObject.toString(), "UTF-8");
        httpPost.setEntity(stringEntity);
        String result = "";
        try {
            result = httpClient.execute(httpPost, responseHandler);
            LoggerUtil.info("subscribe service, url={}, result={}", url.toString(), result);
        } catch (Exception e) {
            LoggerUtil.error("subscribe service error, url={}, result={}, e:", url.toString(), result, e);
            throw new MotanFrameworkException("fail to subscribe service");
        } finally {
            httpPost.releaseConnection();
        }
    }

    public void unsubscribe(URL url) {
        JSONObject jsonObject = buildUrlJsonObj(url);
        HttpPost httpPost = new HttpPost(registryAdminUrl + "/registry/unsubscribe");
        httpPost.addHeader("Content-Type", "application/json");
        StringEntity stringEntity = new StringEntity(jsonObject.toString(), "UTF-8");
        httpPost.setEntity(stringEntity);
        String result = "";
        try {
            result = httpClient.execute(httpPost, responseHandler);
            LoggerUtil.info("unsubscribe service, url={}, result={}", url.toString(), result);
        } catch (Exception e) {
            LoggerUtil.error("unsubscribe service error, url={}, result={}, e:", url.toString(), result, e);
            throw new MotanFrameworkException("fail to unsubscribe service");
        } finally {
            httpPost.releaseConnection();
        }
    }

    public int getMeshPort() {
        int port = 9981;
        HttpGet httpGet = new HttpGet(registryAdminUrl + "/registry/info");
        String result = "";
        try {
            result = httpClient.execute(httpGet, responseHandler);
            port = JSON.parseObject(result).getJSONObject("body").getIntValue("mesh_port");
        } catch (IOException e) {
            LoggerUtil.error("mesh registry check available, result={}, e:", result, e);
        } finally {
            httpGet.releaseConnection();
        }
        return port;
    }

    public boolean checkAvailable() {
        HttpGet httpGet = new HttpGet(registryAdminUrl + "/");
        try {
            HttpResponse response = httpClient.execute(httpGet);
            int status = response.getStatusLine().getStatusCode();
            return status >= 200 && status < 300;
        } catch (IOException e) {
            LoggerUtil.error("mesh registry check available error, e:", e);
        } finally {
            httpGet.releaseConnection();
        }
        return false;
    }
}
