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
package com.alibaba.dubbo.remoting.transport.mina;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ExecutorUtil;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.AbstractServer;
import com.alibaba.dubbo.remoting.transport.dispatcher.ChannelHandlers;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * MinaServer
 *
 * @author qian.lei
 * @author william.liangf
 * @author ding.lid
 */
public class MinaServer extends AbstractServer {//Mina了解以及基本使用

    private static final Logger logger = LoggerFactory.getLogger(MinaServer.class);

    private SocketAcceptor acceptor;

    public MinaServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, ChannelHandlers.wrap(handler, ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME)));
    }

    @Override
    protected void doOpen() throws Throwable {
        // set thread pool.
        acceptor = new SocketAcceptor(getUrl().getPositiveParameter(Constants.IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS),
                Executors.newCachedThreadPool(new NamedThreadFactory("MinaServerWorker",
                        true)));
        // config
        SocketAcceptorConfig cfg = (SocketAcceptorConfig) acceptor.getDefaultConfig();
        cfg.setThreadModel(ThreadModel.MANUAL);
        // set codec.
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MinaCodecAdapter(getCodec(), getUrl(), this)));

        acceptor.bind(getBindAddress(), new MinaHandler(getUrl(), this));
    }

    @Override
    protected void doClose() throws Throwable {
        try {
            if (acceptor != null) {
                acceptor.unbind(getBindAddress());
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public Collection<Channel> getChannels() {
        Set<IoSession> sessions = acceptor.getManagedSessions(getBindAddress());
        Collection<Channel> channels = new HashSet<Channel>();
        for (IoSession session : sessions) {
            if (session.isConnected()) {
                channels.add(MinaChannel.getOrAddChannel(session, getUrl(), this));
            }
        }
        return channels;
    }

    public Channel getChannel(InetSocketAddress remoteAddress) {
        Set<IoSession> sessions = acceptor.getManagedSessions(getBindAddress());
        for (IoSession session : sessions) {
            if (session.getRemoteAddress().equals(remoteAddress)) {
                return MinaChannel.getOrAddChannel(session, getUrl(), this);
            }
        }
        return null;
    }

    public boolean isBound() {
        return acceptor.isManaged(getBindAddress());
    }

    /**
     * Apache Mina Server 是一个网络通信应用框架，也就是说，它主要是对基于TCP/IP、
     * UDP/IP协议栈的通信框架（当然，也可以提供JAVA 对象的序列化服务、
     * 虚拟机管道通信服务等），Mina 可以帮助我们快速开发高性能、高扩展性的网络通信应用
     * ，Mina 提供了事件驱动、异步（Mina 的异步IO 默认使用的是JAVA NIO 作为底层支持）
     * 操作的编程模型
     *
     * ---
     * Mina 的底层依赖的主要是 Java NIO 库，上层提供的是基于事件的异步接口
     * NIO Overview — Apache MINA 官方文档  https://mina.apache.org/mina-project/userguide/ch1-getting-started/ch1.1-nio-overview.html
     * mina框架详解  https://blog.csdn.net/w13770269691/article/details/8614584
     * 认识Mina框架  http://www.linkedkeeper.com/1614.html
     * --
     */

}