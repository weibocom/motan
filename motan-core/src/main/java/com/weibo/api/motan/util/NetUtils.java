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

package com.weibo.api.motan.util;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.exception.MotanFrameworkException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 网络工具类
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-28
 */

public class NetUtils {

    private static final Logger logger = LoggerFactory.getLogger(NetUtils.class);

    public static final String LOCALHOST = "127.0.0.1";

    private static volatile String LOCAL_IP_STRING;
    private static volatile InetAddress LOCAL_ADDRESS = null;

    private static final Pattern LOCAL_IP_PATTERN = Pattern.compile("127(\\.\\d{1,3}){3}$");

    private static final Pattern IP_PATTERN = Pattern.compile("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    public static boolean isInvalidLocalHost(String host) {
        return host == null || host.isEmpty() || host.equalsIgnoreCase("localhost") || host.equals("0.0.0.0")
                || (LOCAL_IP_PATTERN.matcher(host).matches()) || !IP_PATTERN.matcher(host).matches();
    }

    public static boolean isValidLocalHost(String host) {
        return !isInvalidLocalHost(host);
    }

    public static String getLocalIpString() {
        return getLocalIpString(null);
    }

    /**
     * Search strategy: first check cache --> IP specified in the environment variable --> IP from the hostname --> local IP from the connected socket --> get from the NetworkInterface
     *
     * @return local ip string
     */
    public static String getLocalIpString(Map<String, Integer> destHostPorts) {
        if (LOCAL_IP_STRING != null) {
            return LOCAL_IP_STRING;
        }
        if (StringUtils.isNotBlank(System.getenv(MotanConstants.ENV_MOTAN_LOCAL_IP))) {
            // get local IP from env
            String envIp = System.getenv(MotanConstants.ENV_MOTAN_LOCAL_IP).trim();
            if (isValidLocalHost(envIp)) {
                LOCAL_IP_STRING = envIp;
                LoggerUtil.info("use env local IP:" + LOCAL_IP_STRING);
                return LOCAL_IP_STRING;
            }
        }
        LOCAL_IP_STRING = getLocalAddress(destHostPorts).getHostAddress();
        return LOCAL_IP_STRING;
    }

    /**
     * use getLocalIpString() instead
     */
    @Deprecated
    public static InetAddress getLocalAddress() {
        return getLocalAddress(null);
    }

    /**
     * This method will be converted to a private method in subsequent versions. Please use getLocalIpString instead.
     */
    @Deprecated
    public static InetAddress getLocalAddress(Map<String, Integer> destHostPorts) {
        if (LOCAL_ADDRESS != null) {
            return LOCAL_ADDRESS;
        }
        InetAddress localAddress = null;
        String ipPrefix = System.getenv(MotanConstants.ENV_MOTAN_IP_PREFIX);
        if (StringUtils.isNotBlank(ipPrefix)) { // 环境变量中如果指定了motan使用的ip前缀，则使用与该前缀匹配的网卡ip作为本机ip。
            localAddress = getLocalAddressByNetworkInterface(ipPrefix);
            LoggerUtil.info("get local address by ip prefix: " + ipPrefix + ", address:" + localAddress);
        }

        if (!isValidAddress(localAddress)) {
            localAddress = getLocalAddressByHostname();
            LoggerUtil.info("get local address by hostname, address:" + localAddress);
        }

        if (!isValidAddress(localAddress)) {
            localAddress = getLocalAddressBySocket(destHostPorts);
            LoggerUtil.info("get local address by remote host. address:" + localAddress);
        }

        if (!isValidAddress(localAddress)) {
            localAddress = getLocalAddressByNetworkInterface(null);
            LoggerUtil.info("get local address from network interface. address:" + localAddress);
        }

        if (isValidAddress(localAddress)) {
            LOCAL_ADDRESS = localAddress;
            LoggerUtil.info("use " + localAddress + " as local IP");
            return localAddress;
        }

        throw new MotanFrameworkException("getLocalAddress fail, no valid local IP found");
    }

    private static InetAddress getLocalAddressByHostname() {
        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            if (isValidAddress(localAddress)) {
                return localAddress;
            }
        } catch (Throwable e) {
            logger.warn("Failed to retrieving local address by hostname:" + e);
        }
        return null;
    }

    private static InetAddress getLocalAddressBySocket(Map<String, Integer> destHostPorts) {
        if (destHostPorts == null || destHostPorts.isEmpty()) {
            return null;
        }

        for (Map.Entry<String, Integer> entry : destHostPorts.entrySet()) {
            String host = entry.getKey();
            int port = entry.getValue();
            try {
                try (Socket socket = new Socket()) {
                    SocketAddress addr = new InetSocketAddress(host, port);
                    socket.connect(addr, 1000);
                    LoggerUtil.info("get local address from socket. remote host:" + host + ", port:" + port);
                    return socket.getLocalAddress();
                }
            } catch (Exception e) {
                LoggerUtil.warn(String.format("Failed to retrieving local address by connecting to dest host:port(%s:%s) false, e=%s", host,
                        port, e));
            }
        }
        return null;
    }

    private static InetAddress getLocalAddressByNetworkInterface(String prefix) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    try {
                        NetworkInterface network = interfaces.nextElement();
                        Enumeration<InetAddress> addresses = network.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            try {
                                InetAddress address = addresses.nextElement();
                                if (isValidAddress(address)) {
                                    if (StringUtils.isBlank(prefix)) {
                                        return address;
                                    }
                                    if (address.getHostAddress().startsWith(prefix)) {
                                        return address;
                                    }
                                }
                            } catch (Throwable e) {
                                logger.warn("Failed to retrieving ip address, " + e.getMessage(), e);
                            }
                        }
                    } catch (Throwable e) {
                        logger.warn("Failed to retrieving ip address, " + e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            logger.warn("Failed to retrieving ip address, " + e.getMessage(), e);
        }
        return null;
    }

    public static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }
        return isValidLocalHost(address.getHostAddress());
    }

    //return ip to avoid lookup dns
    public static String getHostName(SocketAddress socketAddress) {
        if (socketAddress == null) {
            return null;
        }

        if (socketAddress instanceof InetSocketAddress) {
            InetAddress addr = ((InetSocketAddress) socketAddress).getAddress();
            if (addr != null) {
                return addr.getHostAddress();
            }
        }

        return null;
    }

    // only for unit test
    public static void clearCache() {
        LOCAL_IP_STRING = null;
    }
}
