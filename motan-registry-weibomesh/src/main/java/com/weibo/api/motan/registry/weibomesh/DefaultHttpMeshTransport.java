/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.registry.weibomesh;

import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.util.LoggerUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author zhanglei28
 * @date 2021/6/28.
 */
public class DefaultHttpMeshTransport implements MeshTransport {
    private HttpClient httpClient;
    private static final int DEFAULT_TIMEOUT = 5000;

    public DefaultHttpMeshTransport() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(1000);
        connectionManager.setDefaultMaxPerRoute(100);

        RequestConfig requestConfig = RequestConfig.custom().
                setConnectTimeout(DEFAULT_TIMEOUT).
                setConnectionRequestTimeout(DEFAULT_TIMEOUT).
                setSocketTimeout(DEFAULT_TIMEOUT).
                build();

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().
                setConnectionManager(connectionManager).
                setDefaultRequestConfig(requestConfig).
                useSystemProperties();

        this.httpClient = httpClientBuilder.build();
    }

    @Override
    public ManageResponse getManageRequest(String url) throws MotanFrameworkException {
        HttpGet httpGet = new HttpGet(url);
        return executeRequest(httpGet);
    }

    @Override
    public ManageResponse postManageRequest(String url, Map<String, String> params) throws MotanFrameworkException {
        HttpPost httpPost = new HttpPost(url);
        if (params != null && !params.isEmpty()) {
            List<NameValuePair> paramList = new ArrayList();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                paramList.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(paramList, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new MotanServiceException("DefaultHttpMeshTransport convert post parmas fail. request url:" + url, e);
            }
        }
        return executeRequest(httpPost);
    }

    public ManageResponse postManageRequest(String url, String content) throws MotanFrameworkException {
        HttpPost httpPost = new HttpPost(url);
        if (content != null) {
            try {
                httpPost.setEntity(new StringEntity(content, "UTF-8"));
            } catch (UnsupportedCharsetException e) {
                throw new MotanServiceException("DefaultHttpMeshTransport convert post parmas fail. request url:" + url, e);
            }
        }
        return executeRequest(httpPost);
    }

    private ManageResponse executeRequest(HttpUriRequest httpRequest) {
        try {
            return httpClient.execute(httpRequest, response -> {
                int statusCode = response.getStatusLine().getStatusCode();
                String statusMessage = response.getStatusLine().getReasonPhrase();
                String content = EntityUtils.toString(response.getEntity(), "UTF-8");
                LoggerUtil.info("http request uri:" + httpRequest.getURI() + ", code: " + statusCode
                        + ", message: " + statusMessage + ", content: " + content);
                return new ManageResponse(statusCode, content);
            });
        } catch (IOException e) {
            throw new MotanServiceException("execute mesh manage requst fail. reqeust uri：" + httpRequest.getURI(), e);
        }
    }

}
