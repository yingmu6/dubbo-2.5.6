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
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.AbstractClient;
import com.alibaba.dubbo.remoting.transport.netty4.logging.NettyHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.TimeUnit;

/**
 * NettyClient.
 *
 * @author qian.lei
 * @author william.liangf
 * @author qinliujie
 */

/**
 * todo @csy-v1 netty 客户端、服务端基本使用
 */
public class NettyClient extends AbstractClient { // 使用的是netty 4.x版本以上的包

    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

    private static final NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup(Constants.DEFAULT_IO_THREADS, new DefaultThreadFactory("NettyClientWorker", true));

    private Bootstrap bootstrap;

    private volatile Channel channel; // volatile, please copy reference to use

    /**
     * 构建netty的客户端
     * 1）封装通道处理类 wrapChannelHandler
     * 2）调用父类AbstractClient构造函数，设置send_reconnect、shutdown_timeout等相关属性
     *    并且doOpen()开发客服端、connect()连接服务端
     */
    public NettyClient(final URL url, final ChannelHandler handler) throws RemotingException {
        super(url, wrapChannelHandler(url, handler));
    }

    /**
     * 打开Netty服务（对客户端的服务参数进行设值）
     * 1）设置Netty日志工厂，NettyHelper.setNettyLoggerFactory();
     * 2）通过URL，当前对象NettyClient，构建Netty处理类NettyClientHandler
     * 3）构建客户端启动类Bootstrap
     *  3.1）创建启动类Bootstrap对象
     *  3.2）设置事件处理组NioEventLoopGroup
     *  3.3）设置选项，SO_KEEPALIVE（保持连接）、TCP_NODELAY（tcp不延迟）、ALLOCATOR（直接内存）
     * 4）超时时间处理
     *   若小于3000毫秒，则将3000作为默认值
     *   若大于等于3000毫秒，则将AbstractEndpoint的timeout属性值作为默认值
     * 5）设置处理类
     *   5.1）添加decoder解码处理类
     *   5.2）添加encoder编码处理类
     *   5.3）添加NettyClientHandler，netty客户端处理类
     */
    @Override
    protected void doOpen() throws Throwable {
        NettyHelper.setNettyLoggerFactory();
        final NettyClientHandler nettyClientHandler = new NettyClientHandler(getUrl(), this);
        bootstrap = new Bootstrap();
        bootstrap.group(nioEventLoopGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                //.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getTimeout())
                .channel(NioSocketChannel.class);

        if (getTimeout() < 3000) {
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
        } else {
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getTimeout());
        }

        bootstrap.handler(new ChannelInitializer() {

            protected void initChannel(Channel ch) throws Exception {
                NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyClient.this);
                ch.pipeline()//.addLast("logging",new LoggingHandler(LogLevel.INFO))//for debug
                        .addLast("decoder", adapter.getDecoder())//
                        .addLast("encoder", adapter.getEncoder())//
                        .addLast("handler", nettyClientHandler);
            }
        });
    }

    /**
     * 连接服务器（将Netty客户端Bootstrap连接服务，并将新的通道Channel替换老的通道）
     * 1）获取有效的连接地址（"0.0.0.0"和"127.0.0.1"回路地址不是有效地址，
     *     但是遍历网卡尝试获取有效地址都拿不到有效地址时，兜底方案会以"127.0.0.1"代替，但是会抛出error日志）
     * 2）bootstrap进行连接操作，将通道连接到远程服务，返回ChannelFuture
     * 3）不间断等待3000毫秒，获取结果
     *   3.1）若ret接口成功，并且异步I/O操作已经完成
     *     3.1.1）返回与future关联的通道channel
     *     3.1.2）获取当前客户端旧的通道channel，关闭老的通道oldChannel.close()
     *       在finally中移除通道channel对应的值ConcurrentMap<Channel, NettyChannel> channelMap
     *     3.1.3）在finally处理判断
     *       3.1.3.1）若当前客户端是关闭的，NettyClient.this.isClosed()，
     *                则把新的channel也关闭了，并且将当前NettyClient.this.channel置为null，并且从map中移除通道
     *       3.1.3.2）若当前客户端是正常的，则将新的channel替换老的channel
     *   3.2）若I/O操作失败，future.cause() != null，则抛出远程异常RemotingException，日志带有client标识
     *   3.3）若异步future没有返回成功，并且没有异常future.cause()，那么就是超时异常了，平时看到的客户端连接超时都是这里显示的
     *        把设置的超时时间有实际执行的时间进行比较
     */
    protected void doConnect() throws Throwable {
        long start = System.currentTimeMillis();
        /**
         * ChannelFuture：The result of an asynchronous Channel I/O operation (异步处理I/O的通道)
         * All I/O operations in Netty are asynchronous. （Netty中的所有I/O都是异步的）
         */
        ChannelFuture future = bootstrap.connect(getConnectAddress());
        try {
            boolean ret = future.awaitUninterruptibly(3000, TimeUnit.MILLISECONDS);

            if (ret && future.isSuccess()) {
                Channel newChannel = future.channel();
                try {
                    // 关闭旧的连接
                    Channel oldChannel = NettyClient.this.channel; // copy reference
                    if (oldChannel != null) {
                        try {
                            if (logger.isInfoEnabled()) {
                                logger.info("Close old netty channel " + oldChannel + " on create new netty channel " + newChannel);
                            }
                            oldChannel.close();
                        } finally {
                            NettyChannel.removeChannelIfDisconnected(oldChannel);
                        }
                    }
                } finally {
                    if (NettyClient.this.isClosed()) {
                        try {
                            if (logger.isInfoEnabled()) {
                                logger.info("Close new netty channel " + newChannel + ", because the client closed.");
                            }
                            newChannel.close();
                        } finally {
                            NettyClient.this.channel = null;
                            NettyChannel.removeChannelIfDisconnected(newChannel);
                        }
                    } else {
                        NettyClient.this.channel = newChannel;
                    }
                }
            } else if (future.cause() != null) {
                throw new RemotingException(this, "client(url: " + getUrl() + ") failed to connect to server "
                        + getRemoteAddress() + ", error message is:" + future.cause().getMessage(), future.cause());
            } else {
                throw new RemotingException(this, "client(url: " + getUrl() + ") failed to connect to server "
                        + getRemoteAddress() + " client-side timeout "
                        + getConnectTimeout() + "ms (elapsed: " + (System.currentTimeMillis() - start) + "ms) from netty client "
                        + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion());
            }
        } finally {
            if (!isConnected()) {
                //future.cancel(true);
            }
        }
    }

    @Override
    protected void doDisConnect() throws Throwable {
        try {
            NettyChannel.removeChannelIfDisconnected(channel);
        } catch (Throwable t) {
            logger.warn(t.getMessage());
        }
    }

    @Override
    protected void doClose() throws Throwable {
        //can't shutdown nioEventLoopGroup
        //nioEventLoopGroup.shutdownGracefully();
    }

    @Override
    protected com.alibaba.dubbo.remoting.Channel getChannel() {
        Channel c = channel;
        if (c == null || !c.isActive())
            return null;
        return NettyChannel.getOrAddChannel(c, getUrl(), this);
    }

}