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

package com.weibo.api.motan.registry.support.command;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RpcCommand {

    private List<ClientCommand> clientCommandList;

    public void sort() {
        Collections.sort(clientCommandList, new Comparator<ClientCommand>() {
            @Override
            public int compare(ClientCommand o1, ClientCommand o2) {
                Integer i1 = o1.getIndex();
                Integer i2 = o2.getIndex();
                if (i1 == null) {
                    return -1;
                }
                if (i2 == null) {
                    return 1;
                }
                int r = i1.compareTo(i2);
                return r;
            }
        });
    }

    public static class ClientCommand {
        private Integer index;
        private String version;
        private String dc;
        private String pattern;
        private List<String> mergeGroups;
        // 路由规则，当有多个匹配时，按顺序依次过滤结果
        private List<String> routeRules;
        private String remark;

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getDc() {
            return dc;
        }

        public void setDc(String dc) {
            this.dc = dc;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public List<String> getMergeGroups() {
            return mergeGroups;
        }

        public void setMergeGroups(List<String> mergeGroups) {
            this.mergeGroups = mergeGroups;
        }

        public List<String> getRouteRules() {
            return routeRules;
        }

        public void setRouteRules(List<String> routeRules) {
            this.routeRules = routeRules;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    public List<ClientCommand> getClientCommandList() {
        return clientCommandList;
    }

    public void setClientCommandList(List<ClientCommand> clientCommandList) {
        this.clientCommandList = clientCommandList;
    }

    public static class ServerCommand {

    }
}
