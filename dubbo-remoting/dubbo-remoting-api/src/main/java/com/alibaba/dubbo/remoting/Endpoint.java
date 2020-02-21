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
package com.alibaba.dubbo.remoting;

import com.alibaba.dubbo.common.URL;

import java.net.InetSocketAddress;

/**
 * Endpoint. (API/SPI, Prototype, ThreadSafe)
 *
 * @author william.liangf
 * @see com.alibaba.dubbo.remoting.Channel
 * @see com.alibaba.dubbo.remoting.Client
 * @see com.alibaba.dubbo.remoting.Server
 */
// 用来做啥的？含义是端点，节点；是指zookeeper中的节点，还是dubbo自身的节点？是树结构吗？
// 解：是一个网络节点，dubbo自身的节点。从代码看，没有父节点、子节点、根节点概念，所以不是树结构
// 可以把一个provider或者consumer认为是一个endpoint节点吗
// 解：不能说为provider、consumer，从子接口看，应该是Client、Server客户端和服务端属于一个节点
public interface Endpoint { // 集中的节点

    /**
     * get url. dubbo自定义的URL
     * Dubbo采用总线型方式，参数都放在url中了
     * @return url
     */
    URL getUrl();

    /**
     * get channel handler.
     *
     * @return channel handler
     */
    ChannelHandler getChannelHandler();

    /**
     * get local address.
     *
     * @return local address.
     */
    InetSocketAddress getLocalAddress();

    /**
     * send message.
     *
     * @param message
     * @throws RemotingException
     */
    void send(Object message) throws RemotingException;

    /**
     * send message.
     *
     * @param message
     * @param sent    是否已发送完成
     */
    void send(Object message, boolean sent) throws RemotingException;

    /**
     * close the channel.
     */
    void close();

    /**
     * Graceful（优雅的） close the channel.
     */
    void close(int timeout);

    void startClose();//closing = true 标记状态

    /**
     * is closed.
     *
     * @return closed
     */
    boolean isClosed();

}