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

package com.weibo.api.motan.config;

import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.NetUtils;
import com.weibo.api.motan.util.ReflectUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * <pre>
 * Interface config，
 *
 * 配置约定
 * 	  1 service 和 referer 端相同的参数的含义一定相同；
 *    2 service端参数的覆盖策略：protocol--basicConfig--service，前面的配置会被后面的config参数覆盖；
 *    3 registry 参数不进入service、referer端的参数列表；
 *    4 referer端从注册中心拿到参数后，先用referer端的参数覆盖，然后再使用该service
 * </pre>
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-27
 */

public class AbstractInterfaceConfig extends AbstractConfig {

    private static final long serialVersionUID = 4776516803466933310L;

    // 暴露、使用的协议，暴露可以使用多种协议，但client只能用一种协议进行访问，原因是便于client的管理
    protected List<ProtocolConfig> protocols;

    // 注册中心的配置列表
    protected List<RegistryConfig> registries;

    // 解析后的所有注册中心url
    protected List<URL> registryUrls = new ArrayList<>();

    // 扩展配置点
    protected ExtConfig extConfig;

    // 应用名称
    protected String application;

    // 模块名称
    protected String module;

    // 分组
    protected String group;

    // 服务版本
    protected String version;

    // 代理类型
    protected String proxy;

    // 过滤器
    protected String filter;

    // 最大并发调用
    protected Integer actives;

    // 是否异步
    protected Boolean async;

    // 服务接口的失败mock实现类名
    protected String mock;

    // 是否共享 channel
    protected Boolean shareChannel;

    // if throw exception when call failure，the default value is ture
    protected Boolean throwException;

    // 请求超时时间
    protected Integer requestTimeout;

    // 是否注册
    protected Boolean register;

    // 是否记录访问日志，true记录，false不记录
    protected String accessLog;

    // 是否进行check，如果为true，则在监测失败后抛异常
    protected String check;

    // 重试次数
    protected Integer retries;

    // 是否开启gzip压缩
    protected Boolean usegz;

    // 进行gzip压缩的最小阈值，usegz开启，且大于此值时才进行gzip压缩。单位Byte
    protected Integer mingzSize;

    protected String codec;

    protected String localServiceAddress;

    protected Integer backupRequestDelayTime;

    protected String backupRequestDelayRatio;

    protected String backupRequestSwitcherName;

    protected String backupRequestMaxRetryRatio;

    // 是否需要传输rpc server 端业务异常栈。默认true
    protected Boolean transExceptionStack;

    protected Integer slowThreshold;

    protected String mixGroups;

    protected Boolean asyncInitConnection;

    protected Integer fusingThreshold;

    protected Integer connectTimeout;

    protected String sandboxGroups; // the group name of the sandbox

    protected String backupGroups; // the group name of the backup

    public String getSandboxGroups() {
        return sandboxGroups;
    }

    public void setSandboxGroups(String sandboxGroups) {
        this.sandboxGroups = sandboxGroups;
    }

    public String getBackupGroups() {
        return backupGroups;
    }

    public void setBackupGroups(String backupGroups) {
        this.backupGroups = backupGroups;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getAccessLog() {
        return accessLog;
    }

    public void setAccessLog(String accessLog) {
        this.accessLog = accessLog;
    }

    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    public void setRegistries(List<RegistryConfig> registries) {
        this.registries = registries;
    }

    public ExtConfig getExtConfig() {
        return extConfig;
    }

    public void setExtConfig(ExtConfig extConfig) {
        this.extConfig = extConfig;
    }

    public void setRegistry(RegistryConfig registry) {
        this.registries = Collections.singletonList(registry);
    }

    public Integer getActives() {
        return actives;
    }

    public void setActives(Integer actives) {
        this.actives = actives;
    }

    public Boolean getAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public String getMock() {
        return mock;
    }

    public void setMock(String mock) {
        this.mock = mock;
    }

    public String getCheck() {
        return check;
    }

    @Deprecated
    public void setCheck(Boolean check) {
        this.check = String.valueOf(check);
    }

    public void setCheck(String check) {
        this.check = check;
    }

    public Boolean getShareChannel() {
        return shareChannel;
    }

    public void setShareChannel(Boolean shareChannel) {
        this.shareChannel = shareChannel;
    }

    public List<ProtocolConfig> getProtocols() {
        return protocols;
    }

    public void setProtocols(List<ProtocolConfig> protocols) {
        this.protocols = protocols;
    }

    public void setProtocol(ProtocolConfig protocol) {
        this.protocols = Collections.singletonList(protocol);
    }

    public Boolean getThrowException() {
        return throwException;
    }

    public void setThrowException(Boolean throwException) {
        this.throwException = throwException;
    }

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public boolean hasProtocol() {
        return this.protocols != null && !this.protocols.isEmpty();
    }

    public Boolean getRegister() {
        return register;
    }

    public void setRegister(Boolean register) {
        this.register = register;
    }

    public String getLocalServiceAddress() {
        return localServiceAddress;
    }

    public void setLocalServiceAddress(String localServiceAddress) {
        this.localServiceAddress = localServiceAddress;
    }

    public Boolean getUsegz() {
        return usegz;
    }

    public void setUsegz(Boolean usegz) {
        this.usegz = usegz;
    }

    public Integer getMingzSize() {
        return mingzSize;
    }

    public void setMingzSize(Integer mingzSize) {
        this.mingzSize = mingzSize;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public Integer getBackupRequestDelayTime() {
        return backupRequestDelayTime;
    }

    public void setBackupRequestDelayTime(Integer backupRequestDelayTime) {
        this.backupRequestDelayTime = backupRequestDelayTime;
    }

    public String getBackupRequestDelayRatio() {
        return backupRequestDelayRatio;
    }

    public void setBackupRequestDelayRatio(String backupRequestDelayRatio) {
        this.backupRequestDelayRatio = backupRequestDelayRatio;
    }

    public String getBackupRequestSwitcherName() {
        return backupRequestSwitcherName;
    }

    public void setBackupRequestSwitcherName(String backupRequestSwitcherName) {
        this.backupRequestSwitcherName = backupRequestSwitcherName;
    }

    public String getBackupRequestMaxRetryRatio() {
        return backupRequestMaxRetryRatio;
    }

    public void setBackupRequestMaxRetryRatio(String backupRequestMaxRetryRatio) {
        this.backupRequestMaxRetryRatio = backupRequestMaxRetryRatio;
    }

    public Boolean getTransExceptionStack() {
        return transExceptionStack;
    }

    public void setTransExceptionStack(Boolean transExceptionStack) {
        this.transExceptionStack = transExceptionStack;
    }

    public String getMixGroups() {
        return mixGroups;
    }

    public void setMixGroups(String mixGroups) {
        this.mixGroups = mixGroups;
    }

    public void setRegistryUrls(List<URL> registryUrls) {
        this.registryUrls = registryUrls;
    }

    public Boolean getAsyncInitConnection() {
        return asyncInitConnection;
    }

    public void setAsyncInitConnection(Boolean asyncInitConnection) {
        this.asyncInitConnection = asyncInitConnection;
    }

    public Integer getFusingThreshold() {
        return fusingThreshold;
    }

    public void setFusingThreshold(Integer fusingThreshold) {
        this.fusingThreshold = fusingThreshold;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    // 解析注册中心URL
    protected void loadRegistryUrls() {
        registryUrls.clear();
        if (registries != null && !registries.isEmpty()) {
            for (RegistryConfig config : registries) {
                List<URL> urls = config.toURLs();
                if (urls != null && !urls.isEmpty()) {
                    registryUrls.addAll(urls);
                }
            }
        }
    }

    protected void checkInterfaceAndMethods(Class<?> interfaceClass, List<MethodConfig> methods) {
        if (interfaceClass == null) {
            throw new IllegalStateException("interface not allow null!");
        }
        if (!interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        // 检查方法是否在接口中存在
        if (methods != null && !methods.isEmpty()) {
            for (MethodConfig methodBean : methods) {
                String methodName = methodBean.getName();
                if (methodName == null || methodName.length() == 0) {
                    throw new IllegalStateException("<motan:method> name attribute is required! Please check: <motan:service interface=\""
                            + interfaceClass.getName() + "\" ... ><motan:method name=\"\" ... /></<motan:referer>");
                }
                java.lang.reflect.Method hasMethod = null;
                for (java.lang.reflect.Method method : interfaceClass.getMethods()) {
                    if (method.getName().equals(methodName)) {
                        if (methodBean.getArgumentTypes() != null
                                && ReflectUtil.getMethodParamDesc(method).equals(methodBean.getArgumentTypes())) {
                            hasMethod = method;
                            break;
                        }
                        if (methodBean.getArgumentTypes() != null) {
                            continue;
                        }
                        if (hasMethod != null) {
                            throw new MotanFrameworkException("The interface " + interfaceClass.getName() + " has more than one method "
                                    + methodName + " , must set argumentTypes attribute.", MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
                        }
                        hasMethod = method;
                    }
                }
                if (hasMethod == null) {
                    throw new MotanFrameworkException("The interface " + interfaceClass.getName() + " not found method " + methodName,
                            MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
                }
                methodBean.setArgumentTypes(ReflectUtil.getMethodParamDesc(hasMethod));
            }
        }
    }

    protected String getLocalHostAddress() {
        Map<String, Integer> regHostPorts = new HashMap<String, Integer>();
        for (URL ru : registryUrls) {
            if (StringUtils.isNotBlank(ru.getHost()) && ru.getPort() > 0) {
                regHostPorts.put(ru.getHost(), ru.getPort());
            }
        }
        String localAddress = NetUtils.getLocalIpString(regHostPorts);
        if (NetUtils.isValidLocalHost(localAddress)) {
            return localAddress;
        }
        throw new MotanServiceException("Please config local server hostname with intranet IP first!",
                MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
    }

    public Integer getSlowThreshold() {
        return slowThreshold;
    }

    public void setSlowThreshold(int slowThreshold) {
        this.slowThreshold = slowThreshold;
    }

    public List<URL> getRegistryUrls() {
        return registryUrls;
    }
}
