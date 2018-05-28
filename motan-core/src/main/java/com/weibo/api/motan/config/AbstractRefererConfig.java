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

/**
 *
 * Abstract referer config.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-6-13
 */

public abstract class AbstractRefererConfig extends AbstractInterfaceConfig {

    private static final long serialVersionUID = -8953815191278008453L;

    // 服务接口的mock类SLA
    protected String mean;
    protected String p90;
    protected String p99;
    protected String p999;
    protected String errorRate;
    protected Boolean asyncInitConnection;

    public String getMean() {
        return mean;
    }

    public void setMean(String mean) {
        this.mean = mean;
    }

    public String getP90() {
        return p90;
    }

    public void setP90(String p90) {
        this.p90 = p90;
    }

    public String getP99() {
        return p99;
    }

    public void setP99(String p99) {
        this.p99 = p99;
    }

    public String getP999() {
        return p999;
    }

    public void setP999(String p999) {
        this.p999 = p999;
    }

    public String getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(String errorRate) {
        this.errorRate = errorRate;
    }

    public Boolean getAsyncInitConnection() {
        return asyncInitConnection;
    }

    public void setAsyncInitConnection(Boolean asyncInitConnection) {
        this.asyncInitConnection = asyncInitConnection;
    }

}
