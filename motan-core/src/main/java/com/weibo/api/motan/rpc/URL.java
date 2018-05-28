/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.weibo.api.motan.rpc;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <pre>
 * Desc a reffer or a service.
 * 所有获取URL的parameter时（即带参数的getXXX方法），都必须返回对象,避免不经意的修改引发错误，因为
 * 有些地方需要根据是否含这个参数来进行操作。
 * 
 * 对于getXXX，当不带defaultValue时，如果不存在就返回null
 * </pre>
 * 
 * @author fishermen
 * @version V1.0 created at: 2013-5-16
 */

public class URL {

    private String protocol;

    private String host;

    private int port;

    // interfaceName
    private String path;

    private Map<String, String> parameters;

    private volatile transient Map<String, Number> numbers;

    public URL(String protocol, String host, int port, String path) {
        this(protocol, host, port, path, new HashMap<String, String>());
    }

    public URL(String protocol, String host, int port, String path, Map<String, String> parameters) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.path = removeAsyncPath(path);
        this.parameters = parameters;
    }

    public static URL valueOf(String url) {
        if (StringUtils.isBlank(url)) {
            throw new MotanServiceException("url is null");
        }
        String protocol = null;
        String host = null;
        int port = 0;
        String path = null;
        Map<String, String> parameters = new HashMap<String, String>();;
        int i = url.indexOf("?"); // seperator between body and parameters
        if (i >= 0) {
            String[] parts = url.substring(i + 1).split("\\&");

            for (String part : parts) {
                part = part.trim();
                if (part.length() > 0) {
                    int j = part.indexOf('=');
                    if (j >= 0) {
                        parameters.put(part.substring(0, j), part.substring(j + 1));
                    } else {
                        parameters.put(part, part);
                    }
                }
            }
            url = url.substring(0, i);
        }
        i = url.indexOf("://");
        if (i >= 0) {
            if (i == 0) throw new IllegalStateException("url missing protocol: \"" + url + "\"");
            protocol = url.substring(0, i);
            url = url.substring(i + 3);
        } else {
            i = url.indexOf(":/");
            if (i >= 0) {
                if (i == 0) throw new IllegalStateException("url missing protocol: \"" + url + "\"");
                protocol = url.substring(0, i);
                url = url.substring(i + 1);
            }
        }

        i = url.indexOf("/");
        if (i >= 0) {
            path = url.substring(i + 1);
            url = url.substring(0, i);
        }

        i = url.indexOf(":");
        if (i >= 0 && i < url.length() - 1) {
            port = Integer.parseInt(url.substring(i + 1));
            url = url.substring(0, i);
        }
        if (url.length() > 0) host = url;
        return new URL(protocol, host, port, path, parameters);
    }

    private static String buildHostPortStr(String host, int defaultPort) {
        if (defaultPort <= 0) {
            return host;
        }

        int idx = host.indexOf(":");
        if (idx < 0) {
            return host + ":" + defaultPort;
        }

        int port = Integer.parseInt(host.substring(idx + 1));
        if (port <= 0) {
            return host.substring(0, idx + 1) + defaultPort;
        }
        return host;
    }

    public URL createCopy() {
        Map<String, String> params = new HashMap<String, String>();
        if (this.parameters != null) {
            params.putAll(this.parameters);
        }

        return new URL(protocol, host, port, path, params);
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = removeAsyncPath(path);
    }

    public String getVersion() {
        return getParameter(URLParamType.version.getName(), URLParamType.version.getValue());
    }

    public String getGroup() {
        return getParameter(URLParamType.group.getName(), URLParamType.group.getValue());
    }

    public String getApplication() {
        return getParameter(URLParamType.application.getName(), URLParamType.application.getValue());
    }

    public String getModule() {
        return getParameter(URLParamType.module.getName(), URLParamType.module.getValue());
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public String getParameter(String name, String defaultValue) {
        String value = getParameter(name);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public String getMethodParameter(String methodName, String paramDesc, String name) {
        String value = getParameter(MotanConstants.METHOD_CONFIG_PREFIX + methodName + "(" + paramDesc + ")." + name);
        if (value == null || value.length() == 0) {
            return getParameter(name);
        }
        return value;
    }

    public String getMethodParameter(String methodName, String paramDesc, String name, String defaultValue) {
        String value = getMethodParameter(methodName, paramDesc, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value;
    }

    public void addParameter(String name, String value) {
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(value)) {
            return;
        }
        parameters.put(name, value);
    }

    public void removeParameter(String name) {
        if (name != null) {
            parameters.remove(name);
        }
    }

    public void addParameters(Map<String, String> params) {
        parameters.putAll(params);
    }

    public void addParameterIfAbsent(String name, String value) {
        if (hasParameter(name)) {
            return;
        }
        parameters.put(name, value);
    }

    public Boolean getBooleanParameter(String name, boolean defaultValue) {
        String value = getParameter(name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value);
    }

    public Boolean getMethodParameter(String methodName, String paramDesc, String name, boolean defaultValue) {
        String value = getMethodParameter(methodName, paramDesc, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public Integer getIntParameter(String name, int defaultValue) {
        Number n = getNumbers().get(name);
        if (n != null) {
            return n.intValue();
        }
        String value = parameters.get(name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        getNumbers().put(name, i);
        return i;
    }

    public Integer getMethodParameter(String methodName, String paramDesc, String name, int defaultValue) {
        String key = methodName + "(" + paramDesc + ")." + name;
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.intValue();
        }
        String value = getMethodParameter(methodName, paramDesc, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        getNumbers().put(key, i);
        return i;
    }

    public Long getLongParameter(String name, long defaultValue) {
        Number n = getNumbers().get(name);
        if (n != null) {
            return n.longValue();
        }
        String value = parameters.get(name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        long l = Long.parseLong(value);
        getNumbers().put(name, l);
        return l;
    }

    public Long getMethodParameter(String methodName, String paramDesc, String name, long defaultValue) {
        String key = methodName + "(" + paramDesc + ")." + name;
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.longValue();
        }
        String value = getMethodParameter(methodName, paramDesc, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        long l = Long.parseLong(value);
        getNumbers().put(key, l);
        return l;
    }

    public Float getFloatParameter(String name, float defaultValue) {
        Number n = getNumbers().get(name);
        if (n != null) {
            return n.floatValue();
        }
        String value = parameters.get(name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        float f = Float.parseFloat(value);
        getNumbers().put(name, f);
        return f;
    }

    public Float getMethodParameter(String methodName, String paramDesc, String name, float defaultValue) {
        String key = methodName + "(" + paramDesc + ")." + name;
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.floatValue();
        }
        String value = getMethodParameter(methodName, paramDesc, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        float f = Float.parseFloat(value);
        getNumbers().put(key, f);
        return f;
    }

    public Boolean getBooleanParameter(String name) {
        String value = parameters.get(name);
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(value);
    }

    public String getUri() {
        return protocol + MotanConstants.PROTOCOL_SEPARATOR + host + ":" + port +
                MotanConstants.PATH_SEPARATOR + path;
    }

    /**
     * 返回一个service or referer的identity,如果两个url的identity相同，则表示相同的一个service或者referer
     *
     * @return
     */
    public String getIdentity() {
        return protocol + MotanConstants.PROTOCOL_SEPARATOR + host + ":" + port +
                "/" + getParameter(URLParamType.group.getName(), URLParamType.group.getValue()) + "/" +
                getPath() + "/" + getParameter(URLParamType.version.getName(), URLParamType.version.getValue()) +
                "/" + getParameter(URLParamType.nodeType.getName(), URLParamType.nodeType.getValue());
    }

    /**
     * check if this url can serve the refUrl.
     *
     * @param refUrl
     * @return
     */
    public boolean canServe(URL refUrl) {
        if (refUrl == null || !this.getPath().equals(refUrl.getPath())) {
            return false;
        }

        if (!ObjectUtils.equals(protocol, refUrl.protocol)) {
            return false;
        }

        if (!StringUtils.equals(this.getParameter(URLParamType.nodeType.getName()), MotanConstants.NODE_TYPE_SERVICE)) {
            return false;
        }

        String version = getParameter(URLParamType.version.getName(), URLParamType.version.getValue());
        String refVersion = refUrl.getParameter(URLParamType.version.getName(), URLParamType.version.getValue());
        if (!version.equals(refVersion)) {
            return false;
        }
        // check serialize
        String serialize = getParameter(URLParamType.serialize.getName(), URLParamType.serialize.getValue());
        String refSerialize = refUrl.getParameter(URLParamType.serialize.getName(), URLParamType.serialize.getValue());
        if (!serialize.equals(refSerialize)) {
            return false;
        }
        // 由于需要提供跨group访问rpc的能力，所以不再验证group是否一致。
        return true;
    }

    public String toFullStr() {
        StringBuilder builder = new StringBuilder();
        builder.append(getUri()).append("?");

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            builder.append(name).append("=").append(value).append("&");
        }

        return builder.toString();
    }

    public String toString() {
        return toSimpleString();
    }

    // 包含协议、host、port、path、group
    public String toSimpleString() {
        return getUri() + "?group=" + getGroup();
    }

    public boolean hasParameter(String key) {
        return StringUtils.isNotBlank(getParameter(key));
    }

    /**
     * comma separated host:port pairs, e.g. "127.0.0.1:3000"
     *
     * @return
     */
    public String getServerPortStr() {
        return buildHostPortStr(host, port);

    }

    @Override
    public int hashCode() {
        int factor = 31;
        int rs = 1;
        rs = factor * rs + ObjectUtils.hashCode(protocol);
        rs = factor * rs + ObjectUtils.hashCode(host);
        rs = factor * rs + ObjectUtils.hashCode(port);
        rs = factor * rs + ObjectUtils.hashCode(path);
        rs = factor * rs + ObjectUtils.hashCode(parameters);
        return rs;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof URL)) {
            return false;
        }
        URL ou = (URL) obj;
        if (!ObjectUtils.equals(this.protocol, ou.protocol)) {
            return false;
        }
        if (!ObjectUtils.equals(this.host, ou.host)) {
            return false;
        }
        if (!ObjectUtils.equals(this.port, ou.port)) {
            return false;
        }
        if (!ObjectUtils.equals(this.path, ou.path)) {
            return false;
        }
        return ObjectUtils.equals(this.parameters, ou.parameters);
    }

    private Map<String, Number> getNumbers() {
        if (numbers == null) { // 允许并发重复创建
            numbers = new ConcurrentHashMap<String, Number>();
        }
        return numbers;
    }
    
    /**
     * because async call in client path with Async suffix,we need
     * remove Async suffix in path for subscribe.
     * @param path
     * @return
     */
    private String removeAsyncPath(String path){
        return MotanFrameworkUtil.removeAsyncSuffix(path);
    }

}
