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

import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.registry.support.FailbackRegistry;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.LoggerUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CommandFailbackRegistry extends FailbackRegistry {

    private ConcurrentHashMap<URL, CommandServiceManager> commandManagerMap;

    public CommandFailbackRegistry(URL url) {
        super(url);
        commandManagerMap = new ConcurrentHashMap<URL, CommandServiceManager>();
        LoggerUtil.info("CommandFailbackRegistry init. url: " + url.toSimpleString());
    }

    @Override
    protected void doSubscribe(URL url, final NotifyListener listener) {
        LoggerUtil.info("CommandFailbackRegistry subscribe. url: " + url.toSimpleString());
        URL urlCopy = url.createCopy();
        CommandServiceManager manager = getCommandServiceManager(urlCopy);
        manager.addNotifyListener(listener);

        subscribeService(urlCopy, manager);
        subscribeCommand(urlCopy, manager);

        List<URL> urls = doDiscover(urlCopy);
        if (urls != null && urls.size() > 0) {
            this.notify(urlCopy, listener, urls);
        }
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        LoggerUtil.info("CommandFailbackRegistry unsubscribe. url: " + url.toSimpleString());
        URL urlCopy = url.createCopy();
        CommandServiceManager manager = commandManagerMap.get(urlCopy);

        manager.removeNotifyListener(listener);
        unsubscribeService(urlCopy, manager);
        unsubscribeCommand(urlCopy, manager);

    }

    @Override
    protected List<URL> doDiscover(URL url) {
        LoggerUtil.info("CommandFailbackRegistry discover. url: " + url.toSimpleString());
        List<URL> finalResult;

        URL urlCopy = url.createCopy();
        String commandStr = discoverCommand(urlCopy);
        RpcCommand rpcCommand = null;
        if (StringUtils.isNotEmpty(commandStr)) {
            rpcCommand = RpcCommandUtil.stringToCommand(commandStr);

        }

        LoggerUtil.info("CommandFailbackRegistry discover command. commandStr: " + commandStr + ", rpccommand "
                + (rpcCommand == null ? "is null." : "is not null."));

        if (rpcCommand != null) {
            rpcCommand.sort();
            CommandServiceManager manager = getCommandServiceManager(urlCopy);
            finalResult = manager.discoverServiceWithCommand(urlCopy, new HashMap<String, Integer>(), rpcCommand);

            // 在subscribeCommon时，可能订阅完马上就notify，导致首次notify指令时，可能还有其他service没有完成订阅，
            // 此处先对manager更新指令，避免首次订阅无效的问题。
            manager.setCommandCache(commandStr);
        } else {
            finalResult = discoverService(urlCopy);
        }

        LoggerUtil.info("CommandFailbackRegistry discover size: " + (finalResult == null ? "0" : finalResult.size()));

        return finalResult;
    }

    public List<URL> commandPreview(URL url, RpcCommand rpcCommand, String previewIP) {
        List<URL> finalResult;
        URL urlCopy = url.createCopy();

        if (rpcCommand != null) {
            CommandServiceManager manager = getCommandServiceManager(urlCopy);
            finalResult = manager.discoverServiceWithCommand(urlCopy, new HashMap<String, Integer>(), rpcCommand, previewIP);
        } else {
            finalResult = discoverService(urlCopy);
        }

        return finalResult;
    }

    private CommandServiceManager getCommandServiceManager(URL urlCopy) {
        CommandServiceManager manager = commandManagerMap.get(urlCopy);
        if (manager == null) {
            manager = new CommandServiceManager(urlCopy);
            manager.setRegistry(this);
            CommandServiceManager manager1 = commandManagerMap.putIfAbsent(urlCopy, manager);
            if (manager1 != null) manager = manager1;
        }
        return manager;
    }

    // for UnitTest
    public ConcurrentHashMap<URL, CommandServiceManager> getCommandManagerMap() {
        return commandManagerMap;
    }

    protected abstract void subscribeService(URL url, ServiceListener listener);

    protected abstract void subscribeCommand(URL url, CommandListener listener);

    protected abstract void unsubscribeService(URL url, ServiceListener listener);

    protected abstract void unsubscribeCommand(URL url, CommandListener listener);

    protected abstract List<URL> discoverService(URL url);

    protected abstract String discoverCommand(URL url);

}
