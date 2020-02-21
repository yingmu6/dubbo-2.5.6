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

import com.alibaba.dubbo.common.extension.SPI;


/**
 * ChannelHandler. (API, Prototype, ThreadSafe)
 *
 * @author qian.lei
 * @author william.liangf
 * @see com.alibaba.dubbo.remoting.Transporter#bind(com.alibaba.dubbo.common.URL, ChannelHandler)
 * @see com.alibaba.dubbo.remoting.Transporter#connect(com.alibaba.dubbo.common.URL, ChannelHandler)
 */
//ChannelHandler是通道的事件处理器
//Netty中描述 Handles an I/O event or intercepts（监听、拦截） an I/O operation, and forwards（向前） it to its next handler in its ChannelPipeline.
@SPI
public interface ChannelHandler { //发生事件时，回调对应的函数
    // 连接通道，是ChannelHandler与channel单向连接吗？那通道与通道怎么连接？
    // 解：不管是否是单向，通道与通道可以用Channel中transport(通道传输)
    /**
     * on channel connected.（连接事件）
     * 与Channel建立连接
     * @param channel channel.
     */
    void connected(Channel channel) throws RemotingException; //todo @chenSy Handler处理模式学习

    /**
     * on channel disconnected.（断开连接事件）
     *  断开与Channel的连接
     * @param channel channel.
     */
    void disconnected(Channel channel) throws RemotingException;

    /**
     * on message sent.
     *
     * @param channel channel.
     * @param message message.
     */
    void sent(Channel channel, Object message) throws RemotingException;

    /**
     * on message received.
     *
     * @param channel channel.
     * @param message message.
     */
    void received(Channel channel, Object message) throws RemotingException;

    /**
     * on exception caught.
     *
     * @param channel   channel.
     * @param exception exception.
     */
    void caught(Channel channel, Throwable exception) throws RemotingException;

}