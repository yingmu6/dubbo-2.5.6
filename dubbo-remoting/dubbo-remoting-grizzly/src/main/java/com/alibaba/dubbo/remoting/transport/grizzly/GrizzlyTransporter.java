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
package com.alibaba.dubbo.remoting.transport.grizzly;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Server;
import com.alibaba.dubbo.remoting.Transporter;

/**
 * GrizzlyTransporter
 *
 * @author william.liangf
 */
public class GrizzlyTransporter implements Transporter { // Grizzly了解以及基本使用

    public static final String NAME = "grizzly";

    public Server bind(URL url, ChannelHandler listener) throws RemotingException {
        return new GrizzlyServer(url, listener);
    }

    public Client connect(URL url, ChannelHandler listener) throws RemotingException {
        return new GrizzlyClient(url, listener);
    }

    /**
     * https://javaee.github.io/grizzly/  官方文档
     *
     * Grizzly：Java NIO框架 https://blog.csdn.net/wzyzzu/article/details/51023329
     * Grizzly NIO框架的设计初衷便是帮助开发者更好地利用Java NIO API，构建强大的可扩展的服务器应用，
     * 并提供扩展框架的组件：Web框架（HTTP/S）、WebSocket、Comet等
     *
     * Grizzly是一种应用程序框架，使用JAVA NIO作为基础，并隐藏其编程的复杂性。
     * 容易使用的高性能的API。带来非阻塞socketd到协议处理层。利用高性能的缓冲
     * 和缓冲管理使用高性能的线程池
     */
}