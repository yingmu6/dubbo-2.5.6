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
package com.alibaba.dubbo.remoting.transport.netty4;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ExecutorUtil;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Server;
import com.alibaba.dubbo.remoting.transport.AbstractServer;
import com.alibaba.dubbo.remoting.transport.dispatcher.ChannelHandlers;
import com.alibaba.dubbo.remoting.transport.netty4.logging.NettyHelper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * NettyServer
 *
 * @author qian.lei
 * @author chao.liuc
 * @author qinliujie
 */
public class NettyServer extends AbstractServer implements Server {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private Map<String, Channel> channels; // <ip:port, channel>

    private ServerBootstrap bootstrap;

    private io.netty.channel.Channel channel;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    /**
     * 构建NettyServer
     * 1）为Url中添加线程名
     * 2）封装处理类，ChannelHandler wrap(ChannelHandler handler, URL url)
     * 3）调用父类AbstractServer构造函数，进行相关属性设置
     */
    public NettyServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, ChannelHandlers.wrap(handler, ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME)));
    }

    /**
     * Netty打开服务端 -- 代码流程
     * 1）设置日志工厂处理日志
     * 2）构建ServerBootstrap 服务器引导类
     * 3）构建两个EventLoopGroup（事件循环组），bossGroup、workerGroup
     *    3.1）NioEventLoopGroup(int nThreads, ThreadFactory threadFactory) 入参为，线程数和线程工厂
     *        bossGroup的默认线程工厂名为NettyServerBoss， workerGroup的默认线程工厂名为NettyServerWorker
     *    3.2）主线程Boss就一个，而工作线程有多个，具体数目从url中的参数iothreads获取，默认的线程数
     *         Math.min(Runtime.getRuntime().availableProcessors() + 1, 32) 将进程数+1与32比较，谁小取谁，也就是默认情况下，最多是32个线程数
     * 4）创建NettyServerHandler，继承了ChannelDuplexHandler双工通道，重写通道中的方法，将实现转换为dubbo的channel处理
     *    即对外接口相同，但内部实现不同（对外接口是ChannelDuplexHandler，但内部实现是ChannelHandler）
     *    4.1）构建NettyServerHandler，初始化属性URL、ChannelHandler（传入getUrl()， this）
     *         因为NettyServer是AbstractPeer子类，而AbstractPeer是ChannelHandler实现类，所以可以传入this
     *    4.2）重写ChannelDuplexHandler中channelActive、channelInactive等方法，内部实现用ChannelHandler处理
     * 5）获取通道映射map， Map<String, Channel> channels
     * 6）bootstrap组装相关参数
     *    6.1）设置EventLoopGroup事件循环组，bossGroup为父循环组，workerGroup为子循环组
     *    6.2）指定通道类型为NioServerSocketChannel
     *    6.3）设置childOption参数，TCP_NODELAY：tcp是否延迟，SO_REUSEADDR：是否重用地址，
     *      ALLOCATOR：PooledByteBufAllocator
     *    6.4）设置childHandler处理类，入参为Netty的 ChannelHandler（Dubbo的名称也是ChannelHandler）
     *      6.4.1）重写ChannelInitializer的initChannel初始化方法
     *        6.4.1.1）创建NettyCodecAdapter编解码适配器（入参为Codec2、URL、ChannelHandler）
     *        6.4.1.2）NioSocketChannel获取DefaultChannelPipeline
     *                 然后为DefaultChannelPipeline添加解码器adapter.getDecoder()，
     *                 编码器adapter.getEncoder()，以及处理类NettyServerHandler
     * 7）bootstrap绑定到指定的地址getBindAddress()；设置同步不间断syncUninterruptibly()；
     *    channelFuture.channel()获取ChannelFuture并设置到netty中的Channel
     */
    @Override
    protected void doOpen() throws Throwable { //打开Netty服务端
        NettyHelper.setNettyLoggerFactory();

        bootstrap = new ServerBootstrap();

        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("NettyServerBoss", true));
        workerGroup = new NioEventLoopGroup(getUrl().getPositiveParameter(Constants.IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS),
                new DefaultThreadFactory("NettyServerWorker", true));

        final NettyServerHandler nettyServerHandler = new NettyServerHandler(getUrl(), this);
        channels = nettyServerHandler.getChannels();

        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<NioSocketChannel>() { //ChildOption和ChildHandler了解，ChildOption允许配置通道的配置内容ChannelConfig，ChildHandler通道事件处理器
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyServer.this);
                        ch.pipeline()//.addLast("logging",new LoggingHandler(LogLevel.INFO))//for debug
                                .addLast("decoder", adapter.getDecoder())
                                .addLast("encoder", adapter.getEncoder())
                                .addLast("handler", nettyServerHandler);
                    }
                });
        // bind
        ChannelFuture channelFuture = bootstrap.bind(getBindAddress());
        channelFuture.syncUninterruptibly();
        channel = channelFuture.channel();
        /**
         * https://www.jianshu.com/p/4835eb4e91ab
         * https://my.oschina.net/xinxingegeya/blog/278877  包含用例
         *
         * The result of an asynchronous Channel I/O operation（异步I/O操作返回的结果）
         * ChannelFuture的作用是用来保存Channel异步操作的结果。
         *
         * 在Netty中所有的I/O操作都是异步的。这意味着任何的I/O调用都将立即返回，
         * 而不保证这些被请求的I/O操作在调用结束的时候已经完成。取而代之地，
         * 你会得到一个返回的ChannelFuture实例，这个实例将给你一些关于I/O操作结果或者状态的信息。
         *
         * Future提供了一种在操作完成时通知应用程序的方式，这个对象可以看作是一个异步结果的占位符，它将在未来某个时刻完成，并提供对其结果的访问，访问时是阻塞地拿取结果
         * 而ChannelFuture能够注册一个或多个ChannelFutureListener实例，其回调方法operationComplete()会在对应的操作完成时被调用，这种监听通知机制消除了我们进行手动检查操作是否完成的必要。
         * 每个Netty的出站I/O操作都会返回一个ChannelFuture，他们都不会阻塞。也体现出了Netty完全是异步和事件驱动的。
         */

    }

    @Override
    protected void doClose() throws Throwable {
        try {
            if (channel != null) {
                // unbind.
                channel.close();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            Collection<com.alibaba.dubbo.remoting.Channel> channels = getChannels();
            if (channels != null && channels.size() > 0) {
                for (com.alibaba.dubbo.remoting.Channel channel : channels) {
                    try {
                        channel.close();
                    } catch (Throwable e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (bootstrap != null) {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (channels != null) {
                channels.clear();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public Collection<Channel> getChannels() {
        Collection<Channel> chs = new HashSet<Channel>();
        for (Channel channel : this.channels.values()) {
            if (channel.isConnected()) {
                chs.add(channel);
            } else {
                channels.remove(NetUtils.toAddressString(channel.getRemoteAddress()));
            }
        }
        return chs;
    }

    public Channel getChannel(InetSocketAddress remoteAddress) {
        return channels.get(NetUtils.toAddressString(remoteAddress));
    }

    public boolean isBound() {
        return channel.isActive();
    }

}