/*
 * Copyright 2009-2016 Weibo, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.weibo.api.motan.rpc.init;

import java.util.List;

import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.util.LoggerUtil;

/**
 * 
 * @Description Initializable factory. Initializable.init() will be called before spring bean init.
 *              U can custom Initializable by SPI to do smthing before rpc start. all Initializable
 *              will be init in default. U can set sequence by annotation @see
 *              com.weibo.api.motan.core.extension.Activation
 * @author zhanglei
 * @date 2016-6-15
 *
 */
public class InitializationFactory {
    private static boolean inited = false;
    private static Initializable instance = new AllSpiInitialization();

    public static Initializable getInitialization() {
        return instance;
    }

    static class AllSpiInitialization implements Initializable {

        @Override
        // find all initilizable spi and init it.
        public synchronized void init() {
            if (!inited) {
                try {
                    LoggerUtil.info("AllSpiInitialization init.");
                    ExtensionLoader<Initializable> extensionLoader = ExtensionLoader.getExtensionLoader(Initializable.class);
                    List<Initializable> allInit = extensionLoader.getExtensions(null);
                    if (allInit != null && !allInit.isEmpty()) {
                        for (Initializable initializable : allInit) {
                            try {
                                initializable.init();
                                LoggerUtil.info(initializable.getClass().getName() + " is init.");
                            } catch (Exception initErr) {
                                LoggerUtil.error(initializable.getClass().getName() + " init fail!", initErr);
                            }
                        }
                    }
                    inited = true;
                    LoggerUtil.info("AllSpiInitialization init finish.");
                } catch (Exception e) {
                    LoggerUtil.error("Initializable spi init fail!", e);;
                }
            }
        }
    }
}
