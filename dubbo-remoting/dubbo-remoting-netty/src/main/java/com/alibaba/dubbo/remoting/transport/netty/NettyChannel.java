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
package com.alibaba.dubbo.remoting.transport.netty;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.AbstractChannel;

import org.jboss.netty.channel.ChannelFuture;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * NettyChannel.
 *
 * @author qian.lei
 * @author william.liangf
 */
final class NettyChannel extends AbstractChannel { //默认Netty传输，封装Netty3的Channel
    /**
     * 数据结构
     * 类继承关系：
     * 1）NettyChannel继承了AbstractChannel抽象类
     * 2）AbstractChannel抽象类继承了AbstractPeer抽象类并实现了Channel接口
     * 3）AbstractPeer抽象类实现了Endpoint、ChannelHandler接口
     *
     * 维护的数据：
     * 当前类的属性
     * ConcurrentMap<Channel, NettyChannel> channelMap（Netty中的通道与Dubbo封装的通道进行映射并缓存）
     * org.jboss.netty.channel.Channel channel（Netty中的通道信息）
     * Map<String, Object> attributes（通道中的属性集）
     *
     * 继承的属性：
     * 继承AbstractPeer的属性：
     * ChannelHandler handler（通道处理器）
     * URL url（维护的url数据信息）
     * 通道的状态：closing（关闭中）、closed（已经关闭）
     */
    private static final Logger logger = LoggerFactory.getLogger(NettyChannel.class);

    //channelMap 是netty中的通道和dubbo自定义的NettyChannel的映射
    private static final ConcurrentMap<org.jboss.netty.channel.Channel, NettyChannel> channelMap = new ConcurrentHashMap<org.jboss.netty.channel.Channel, NettyChannel>();

    private final org.jboss.netty.channel.Channel channel;

    private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

    private NettyChannel(org.jboss.netty.channel.Channel channel, URL url, ChannelHandler handler) {
        super(url, handler);
        if (channel == null) {
            throw new IllegalArgumentException("netty channel == null;");
        }
        this.channel = channel;
    }

    //若存在channel直接返回，不存在则先创建后返回
    static NettyChannel getOrAddChannel(org.jboss.netty.channel.Channel ch, URL url, ChannelHandler handler) { //默认情况：Channel实现类NioClientSocketChannel, ChannelHandler实现类为NettyClient
        if (ch == null) {
            return null;
        }
        NettyChannel ret = channelMap.get(ch);
        if (ret == null) {
            NettyChannel nc = new NettyChannel(ch, url, handler);
            if (ch.isConnected()) { //若通道处于被连接状态，则放入通道缓存
                ret = channelMap.putIfAbsent(ch, nc);
            }
            if (ret == null) {
                ret = nc;
            }
        }
        return ret;
    }

    //若通道没有被连接，则移除对应的缓存
    static void removeChannelIfDisconnected(org.jboss.netty.channel.Channel ch) {
        if (ch != null && !ch.isConnected()) {
            channelMap.remove(ch);
        }
    }

    //获取通道的本地地址
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) channel.getLocalAddress();
    }

    //获取通道的远程地址
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) channel.getRemoteAddress();
    }

    public boolean isConnected() {
        return channel.isConnected();
    }

    //消息格式如：Request [id=1, version=2.0.0, twoway=true, event=false, broken=false, data=RpcInvocation [methodName=sayHello, parameterTypes=[class java.lang.String], arguments=[world : ], attachments={path=com.alibaba.dubbo.demo.DemoService, interface=com.alibaba.dubbo.demo.DemoService, version=0.0.0}]]

    /**
     * 发送消息
     */
    public void send(Object message, boolean sent) throws RemotingException {
        /**
         * super可以调用父类构造方法、成员方法、成员变量
         * super若调用父类的构造方法，需写在子类构造方法的第一行，若父类构造方法有多个参数，则super对应设置，如super(A,B)
         * 调用子类构造方法时，若没有通过super调用父类的构造方法时，会默认调用父类的无参构造方法，若父类没有，则会报错
         *
         * this是自身的一个对象，代表对象本身，可以理解为：指向对象本身的一个指针
         * https://www.cnblogs.com/hasse/p/5023392.html
         *
         * super()和this()类似,区别是，super()从子类中调用父类的构造方法，this()在同一类内调用其它方法。
         * super()和this()均需放在构造方法内第一行。
         * this()和super()都指的是对象，所以，均不可以在static环境中使用。包括：static变量,static方法，static语句块。
         * 从本质上讲，this是一个指向本对象的指针, 然而super是一个Java关键字。
         */
        super.send(message, sent);

        boolean success = true;
        int timeout = 0;
        try {
            ChannelFuture future = channel.write(message);//Sends a message to this channel asynchronously（异步的往通道里发送消息）
            if (sent) {
                timeout = getUrl().getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
                success = future.await(timeout);
            }
            Throwable cause = future.getCause();
            if (cause != null) {
                throw cause;
            }
        } catch (Throwable e) {
            throw new RemotingException(this, "Failed to send message " + message + " to " + getRemoteAddress() + ", cause: " + e.getMessage(), e);
        }

        if (!success) {
            throw new RemotingException(this, "Failed to send message " + message + " to " + getRemoteAddress()
                    + "in timeout(" + timeout + "ms) limit");
        }
    }

    /**
     * 关闭通道
     */
    public void close() {
        try {
            super.close(); //设置关闭状态
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            removeChannelIfDisconnected(channel); //移除netty通道对应的缓存
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            attributes.clear(); //清除通道中维护的属性数据
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (logger.isInfoEnabled()) {
                logger.info("Close netty channel " + channel);
            }
            channel.close(); //关闭netty中的通道
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        if (value == null) { // The null value unallowed in the ConcurrentHashMap.
            attributes.remove(key);
        } else {
            attributes.put(key, value);
        }
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    @Override
    public int hashCode() { //todo 11/18 对象比较hashCode()、equals()了解
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channel == null) ? 0 : channel.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) { //对象比较，比较NettyChannel中的通道值channel
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        NettyChannel other = (NettyChannel) obj;
        if (channel == null) {
            if (other.channel != null) return false;
        } else if (!channel.equals(other.channel)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "NettyChannel [channel=" + channel + "]";
    }

}