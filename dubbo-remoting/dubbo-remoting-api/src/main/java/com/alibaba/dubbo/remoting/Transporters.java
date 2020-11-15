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
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.remoting.transport.ChannelHandlerAdapter;
import com.alibaba.dubbo.remoting.transport.ChannelHandlerDispatcher;

/**
 * Transporter facade. (API, Static, ThreadSafe)
 *
 * @author william.liangf
 */
public class Transporters {

    static {
        // check duplicate jar package
        Version.checkDuplicate(Transporters.class);
        Version.checkDuplicate(RemotingException.class);
    }

    private Transporters() {
    }

    public static Server bind(String url, ChannelHandler... handler) throws RemotingException {
        return bind(URL.valueOf(url), handler);
    }

    /**
     * 将URL与多个通道处理类ChannelHandler绑定
     * 1）判断参数url、handlers是否正确
     * 2）判断是否有多个ChannelHandler
     *   2.1）只有一个：取第一个通道处理类ChannelHandler
     *   2.2）有多个：
     *       构造ChannelHandler的子类ChannelHandlerDispatcher，
     *       并且将handlers设置到ChannelHandlerDispatcher 的属性中 Collection<ChannelHandler> channelHandlers
     * 3）获取传输对象Transporter的实例(默认是NettyTransporter)，并调用bind方法
     */
    public static Server bind(URL url, ChannelHandler... handlers) throws RemotingException { //service export 步骤13
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handlers == null || handlers.length == 0) {
            throw new IllegalArgumentException("handlers == null");
        }
        ChannelHandler handler;
        if (handlers.length == 1) {
            handler = handlers[0];
        } else {
            handler = new ChannelHandlerDispatcher(handlers);
        }
        return getTransporter().bind(url, handler);
    }

    public static Client connect(String url, ChannelHandler... handler) throws RemotingException {
        return connect(URL.valueOf(url), handler);
    }

    /**
     * 连接服务：
     * 1）判断url是否为空
     * 2）判断传入ChannelHandler处理类参数
     *  2.1）若为空或者为0个，则创建ChannelHandlerAdapter适配类（该类只实现接口，实现内容是空的）
     *  2.2）若处理类是1个，直接取第一个ChannelHandler
     *  2.3）若处理类有多个，构建子类ChannelHandlerDispatcher的实例，并且设置该实例的channelHandlers集合属性
     * 3）通过SPI获取到Transporter的实例，并且将构建的handler传入connect方法进行服务连接
     *   Transporter的默认扩展名是"netty4"，即调用com.alibaba.dubbo.remoting.transport.netty4.NettyTransporter中的connect方法
     */
    public static Client connect(URL url, ChannelHandler... handlers) throws RemotingException {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        ChannelHandler handler;
        if (handlers == null || handlers.length == 0) {
            handler = new ChannelHandlerAdapter();
        } else if (handlers.length == 1) {
            handler = handlers[0];
        } else {
            handler = new ChannelHandlerDispatcher(handlers);
        }
        return getTransporter().connect(url, handler);
    }

    public static Transporter getTransporter() { //获取Transporter自适应扩展
        return ExtensionLoader.getExtensionLoader(Transporter.class).getAdaptiveExtension();
    }

}