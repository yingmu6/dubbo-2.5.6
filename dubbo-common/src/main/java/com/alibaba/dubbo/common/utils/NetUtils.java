/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.utils;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * IP and Port Helper for RPC,
 *
 * @author shawn.qianx
 */

public class NetUtils {

    public static final String LOCALHOST = "127.0.0.1";
    public static final String ANYHOST = "0.0.0.0";
    private static final Logger logger = LoggerFactory.getLogger(NetUtils.class);
    private static final int RND_PORT_START = 30000;

    private static final int RND_PORT_RANGE = 10000;

    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 65535;
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}\\:\\d{1,5}$");
    private static final Pattern LOCAL_IP_PATTERN = Pattern.compile("127(\\.\\d{1,3}){3}$"); //以127开头的本地地址
    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");
    private static final Map<String, String> hostNameCache = new LRUCache<String, String>(1000);
    private static volatile InetAddress LOCAL_ADDRESS = null;

    public static int getRandomPort() { //随机端口从3000开始，随机值的返回是10000，所以随机端口 30000 - 40000
        return RND_PORT_START + RANDOM.nextInt(RND_PORT_RANGE);
    }

    public static int getAvailablePort() {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket();
            ss.bind(null);
            return ss.getLocalPort();
        } catch (IOException e) {
            return getRandomPort();
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static int getAvailablePort(int port) {
        if (port <= 0) {
            return getAvailablePort();
        }
        for (int i = port; i < MAX_PORT; i++) {
            ServerSocket ss = null;
            try {
                ss = new ServerSocket(i); //将端口数值依次递增，不断带上端口尝试连接连接
                return i;
            } catch (IOException e) {
                // continue
            } finally {
                if (ss != null) {
                    try {
                        ss.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        return port;
    }

    public static boolean isInvalidPort(int port) {
        return port <= MIN_PORT || port > MAX_PORT;
    }

    public static boolean isValidAddress(String address) {
        return ADDRESS_PATTERN.matcher(address).matches();
    }

    public static boolean isLocalHost(String host) {
        return host != null
                && (LOCAL_IP_PATTERN.matcher(host).matches()
                || host.equalsIgnoreCase("localhost"));
    }

    public static boolean isAnyHost(String host) {
        return "0.0.0.0".equals(host);
    }

    public static boolean isInvalidLocalHost(String host) { //判断是否是无效的ip
        return host == null
                || host.length() == 0
                || host.equalsIgnoreCase("localhost")
                || host.equals("0.0.0.0")
                || (LOCAL_IP_PATTERN.matcher(host).matches()); //为空，localhost，0.0.0.0，127...都是无效的ip
    }

    public static boolean isValidLocalHost(String host) {
        return !isInvalidLocalHost(host);
    }

    public static InetSocketAddress getLocalSocketAddress(String host, int port) {
        return isInvalidLocalHost(host) ?
                new InetSocketAddress(port) : new InetSocketAddress(host, port);
    }

    private static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress())
            return false;
        String name = address.getHostAddress();
        return (name != null
                && !ANYHOST.equals(name)
                && !LOCALHOST.equals(name)
                && IP_PATTERN.matcher(name).matches());
    }

    public static String getLocalHost() {
        InetAddress address = getLocalAddress();
        return address == null ? LOCALHOST : address.getHostAddress();
    }

    /**
     * todo @csy-v1 待调试；过滤啥
     */
    public static String filterLocalHost(String host) {
        if (host == null || host.length() == 0) {
            return host;
        }
        if (host.contains("://")) {
            URL u = URL.valueOf(host);
            if (NetUtils.isInvalidLocalHost(u.getHost())) {
                return u.setHost(NetUtils.getLocalHost()).toFullString();
            }
        } else if (host.contains(":")) {
            int i = host.lastIndexOf(':');
            if (NetUtils.isInvalidLocalHost(host.substring(0, i))) {
                return NetUtils.getLocalHost() + host.substring(i);
            }
        } else {
            if (NetUtils.isInvalidLocalHost(host)) {
                return NetUtils.getLocalHost();
            }
        }
        return host;
    }

    /**
     * 遍历本地网卡，返回第一个合理的IP。
     * todo @csy-v1 待调试观察
     * @return 本地网卡IP
     */
    public static InetAddress getLocalAddress() {
        if (LOCAL_ADDRESS != null)
            return LOCAL_ADDRESS;
        InetAddress localAddress = getLocalAddress0();
        LOCAL_ADDRESS = localAddress;
        return localAddress;
    }

    public static String getLogHost() {
        InetAddress address = LOCAL_ADDRESS;
        return address == null ? LOCALHOST : address.getHostAddress();
    }

    private static InetAddress getLocalAddress0() {
        InetAddress localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost();
            if (isValidAddress(localAddress)) {
                return localAddress;
            }
        } catch (Throwable e) {
            logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) { //遍历网卡，找到有效的本地地址
                    try {
                        NetworkInterface network = interfaces.nextElement();
                        Enumeration<InetAddress> addresses = network.getInetAddresses();
                        if (addresses != null) {
                            while (addresses.hasMoreElements()) {
                                try {
                                    InetAddress address = addresses.nextElement();
                                    if (isValidAddress(address)) {
                                        return address;
                                    }
                                } catch (Throwable e) {
                                    logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
                                }
                            }
                        }
                    } catch (Throwable e) {
                        logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
        }
        /**@c 获取不到本地主机ip时，使用127.0.0.1代替 */
        logger.error("Could not get local host ip address, will use 127.0.0.1 instead.");
        return localAddress;
    }

    public static String getHostName(String address) {
        try {
            int i = address.indexOf(':');
            if (i > -1) {
                address = address.substring(0, i);
            }
            String hostname = hostNameCache.get(address);
            if (hostname != null && hostname.length() > 0) {
                return hostname;
            }
            InetAddress inetAddress = InetAddress.getByName(address);
            if (inetAddress != null) {
                hostname = inetAddress.getHostName();
                hostNameCache.put(address, hostname);
                return hostname;
            }
        } catch (Throwable e) {
            // ignore
        }
        return address;
    }

    /**
     * @csy-v2 ip、host相关概念了解
     * 获取主机名对应的ip地址
     * @param hostName
     * @return ip address or hostName if UnknownHostException
     *
     * https://www.cnblogs.com/fangzuchang/p/6702023.html  网址（url），域名，ip地址，dns，hosts之间的关系
     * ip：主机在网络中的地址
     * host：主机域名，相比ip便于记忆，相当于ip的别名
     * DNS: Domain Name System域名系统，用于IP与域名的转换
     * HOSTS文件相当于一个本地的小型DNS服务器，电脑会优先在本地的HOSTS文件中查找网址对应的IP，如果没有找到，才向DNS请求。
     * URL：网址，域名前加上传输协议信息及主机类型信息就构成了网址(URL）
     *
     * https://www.jianshu.com/p/6c6b9d629bae Hosts详解（含域名、DNS）
     */
    public static String getIpByHost(String hostName) {
        try {
            return InetAddress.getByName(hostName).getHostAddress();
        } catch (UnknownHostException e) {
            return hostName;
        }
    }

    /**
     * @csy-v2  Socket了解以及应用场景（套接字的概念）
     * http://c.biancheng.net/view/2123.html socket概念
     *
     * 网络编程就是编写程序使联网的计算机相互交换数据。
     * socket 的原意是“插座”，在计算机通信领域，socket 被翻译为“套接字”，它是计算机之间进行通信的一种约定或一种方式。
     * 通过 socket 这种约定，一台计算机可以接收其他计算机的数据，也可以向其他计算机发送数据
     * Socket跟TCP/IP并没有必然的联系。Socket的出现只是可以更方便的使用TCP/IP协议栈而已，其对TCP/IP进行了抽象，
     * 形成了几个最基本的函数接口。比如create，listen，accept，connect，read和write等等。
     *
     * https://segmentfault.com/a/1190000014044351 一篇搞懂TCP、HTTP、Socket、Socket连接池
     * 网络通信的分层模型：七层模型，亦称OSI(Open System Interconnection)模型 ，Interconnection：互联
     * 自下往上分为：物理层、数据链路层、网络层、传输层、会话层、表示层和应用层。所有有关通信的都离不开它，
     *
     * 所谓长连接，指在一个TCP连接上可以连续发送多个数据包，在TCP连接保持期间，如果没有数据包发送，
     * 需要双方发检测包以维持此连接(心跳包)，一般需要自己做在线维持。 短连接是指通信双方有数据交互时，就建立一个TCP连接，数据发送完成后，则断开此TCP连接。
     *
     * 通常的短连接操作步骤是：
     * 连接→数据传输→关闭连接；
     * 而长连接通常就是：
     * 连接→数据传输→保持连接(心跳)→数据传输→保持连接(心跳)→……→关闭连接；
     *
     * 心跳包就是在客户端和服务端间 定时通知 对方自己状态的一个自己定义的命令字，按照一定的时间间隔发送，类似于心跳，所以叫做心跳包。
     * 定时发送一个自定义的结构体（心跳包或心跳帧），让对方知道自己“在线”,以确保链接的有效性
     */
    public static String toAddressString(InetSocketAddress address) {
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }

    /**
     * 基于TCP协议上自定义自己的应用层的协议需要解决的几个问题：
     * 1) 心跳包格式的定义及处理
     * 2) 报文头的定义，就是你发送数据的时候需要先发送报文头，报文里面能解析出你将要发送的数据长度
     * 3) 你发送数据包的格式，是json的还是其他序列化的方式
     *
     * 什么是Socket连接池,池的概念可以联想到是一种资源的集合，所以Socket连接池，就是维护着一定数量Socket长连接的集合。
     * 它能自动检测Socket长连接的有效性，剔除无效的连接，补充连接池的长连接的数量。
     */
    public static InetSocketAddress toAddress(String address) {
        int i = address.indexOf(':');
        String host;
        int port;
        if (i > -1) {
            host = address.substring(0, i);
            port = Integer.parseInt(address.substring(i + 1));
        } else {
            host = address;
            port = 0;
        }
        return new InetSocketAddress(host, port);
    }

    public static String toURL(String protocol, String host, int port, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append(protocol).append("://");
        sb.append(host).append(':').append(port);
        if (path.charAt(0) != '/')
            sb.append('/');
        sb.append(path);
        return sb.toString();
    }

}