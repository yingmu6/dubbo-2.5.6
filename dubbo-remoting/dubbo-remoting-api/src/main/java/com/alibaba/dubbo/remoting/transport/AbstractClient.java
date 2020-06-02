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
package com.alibaba.dubbo.remoting.transport;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.store.DataStore;
import com.alibaba.dubbo.common.utils.ExecutorUtil;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.dispatcher.ChannelHandlers;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AbstractClient
 *
 * @author qian.lei
 * @author chao.liuc
 */

/**
 * 了解面对抽象编程思想
 * https://blog.csdn.net/chenssy/article/details/12858267 抽象类与接口
 * 1）抽象类是对类抽象，而接口是对行为的抽象。抽象类所代表的是“is-a”的关系，而接口所代表的是“like-a”的关系。都对多态提供了非常好的支持。
 * 2）如果一个类没有足够的信息来描述一个具体的对象，而需要其他具体的类来支撑它，那么这样的类我们称它为抽象类。如Animal与Dog，
 *    抽象类体现了数据抽象的思想，是实现多态的一种机制。抽象方法:只有方法声明，没有方法体
 * 3）接口是用来建立类与类之间的协议，它所提供的只是一种形式，而没有具体的实现。 接口是完全抽象的类，更纯碎的抽象类
 *   接口中的方法都是public abstract, 变量都是 public final
 */
public abstract class AbstractClient extends AbstractEndpoint implements Client { //将公共逻辑放在抽象类中，封装子类中的重复内容

    protected static final String CLIENT_THREAD_POOL_NAME = "DubboClientHandler";
    private static final Logger logger = LoggerFactory.getLogger(AbstractClient.class);
    private static final AtomicInteger CLIENT_THREAD_POOL_ID = new AtomicInteger();
    // ScheduledThreadPoolExecutor 可定时执行或延迟执行的线程池
    private static final ScheduledThreadPoolExecutor reconnectExecutorService = new ScheduledThreadPoolExecutor(2, new NamedThreadFactory("DubboClientReconnectTimer", true));
    private final Lock connectLock = new ReentrantLock();
    private final boolean send_reconnect;
    private final AtomicInteger reconnect_count = new AtomicInteger(0);
    //重连的error日志是否已经被调用过.
    private final AtomicBoolean reconnect_error_log_flag = new AtomicBoolean(false);
    //重连warning的间隔.(waring多少次之后，warning一次) //for test
    private final int reconnect_warning_period;
    private final long shutdown_timeout;
    protected volatile ExecutorService executor;
    private volatile ScheduledFuture<?> reconnectExecutorFuture = null;
    //the last successed connected time
    private long lastConnectedTime = System.currentTimeMillis();

    /**
     * AbstractClient构建方法
     * 1）设置相关参数
     *    1.1）构造AbstractPeer，设置其属性URL、ChannelHandler
     *    1.2）构建AbstractEndpoint，设置其属性codec、timeout、connectTimeout
     *    1.3）构建AbstractClient，设置属性send_reconnect、shutdown_timeout、reconnect_warning_period
     * 2）调用子类的方法，打开客户端，比如NettyClient中的doOpen方法，若异常则抛出相关日志，并做关闭close()
     * 3）调用当前抽象类中的connect方法，连接服务器，若异常则抛出相关日志，并做关闭close()
     * 4）构建ExecutorService线程池执行类
     *    4.1）获取DataStore的默认扩展SimpleDataStore实例
     *    4.2）调用SimpleDataStore的方式get，获取键consumer对应的实例
     * 5）缓存DataStore中的key处理好后，就从本地缓存中移除掉
     */
    public AbstractClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);

        send_reconnect = url.getParameter(Constants.SEND_RECONNECT_KEY, false);

        shutdown_timeout = url.getParameter(Constants.SHUTDOWN_TIMEOUT_KEY, Constants.DEFAULT_SHUTDOWN_TIMEOUT);

        //默认重连间隔2s，1800表示1小时warning一次.
        reconnect_warning_period = url.getParameter("reconnect.waring.period", 1800);

        try {
            doOpen(); //打开相关客户端
        } catch (Throwable t) {
            close();
            throw new RemotingException(url.toInetSocketAddress(), null,
                    "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                            + " connect to the server " + getRemoteAddress() + ", cause: " + t.getMessage(), t);
        }
        try {
            // connect.
            connect(); //连接服务端
            if (logger.isInfoEnabled()) { //正常连接时会打印出日志
                /**
                 * 观察控制台启动日志
                 * 打印内容 Start NettyClient（客户端类） MacBook-Pro-csy.local/192.168.0.102(本地地址)
                 *    connect to the server /192.168.0.102:20885（远程地址）, dubbo version: 2.0.0, current host: 192.168.0.102（FailsafeLogger 打印日志时加入的描述信息）
                 * 实现类为NettyClient，所以getClass().getSimpleName() 为NettyClient
                 */
                logger.info("Start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress() + " connect to the server " + getRemoteAddress());

            }
        } catch (RemotingException t) {
            if (url.getParameter(Constants.CHECK_KEY, true)) {
                close();
                throw t; //继续抛出异常
            } else {
                logger.warn("Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                        + " connect to the server " + getRemoteAddress() + " (check == false, ignore and retry later!), cause: " + t.getMessage(), t);
            }

            /**
             *  平时多留意kibana上相关dubbo异常日志（明确是什么异常）
             *  搜关键字 "cause"、看异常类型，比如TimeoutException（超时异常等）、dubbo日志打印时会附加标志以及常用信息 [DUBBO]
             */
        } catch (Throwable t) {
            close();
            throw new RemotingException(url.toInetSocketAddress(), null,
                    "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                            + " connect to the server " + getRemoteAddress() + ", cause: " + t.getMessage(), t);
        }

        executor = (ExecutorService) ExtensionLoader.getExtensionLoader(DataStore.class) //获取缓存对象，获取值以后移除缓存对象
                .getDefaultExtension().get(Constants.CONSUMER_SIDE, Integer.toString(url.getPort()));
        ExtensionLoader.getExtensionLoader(DataStore.class)
                .getDefaultExtension().remove(Constants.CONSUMER_SIDE, Integer.toString(url.getPort()));
    }

    /**
     * 封装通道处理类
     * 1）设置url的线程名，若url中的键threadname不存在值，则以DubboClientHandler为线程名
     * 2）为url的键threadpool，设置值为cached
     * 3）调用ChannelHandlers.wrap 对处理类进行封装处理
     */
    protected static ChannelHandler wrapChannelHandler(URL url, ChannelHandler handler) {
        url = ExecutorUtil.setThreadName(url, CLIENT_THREAD_POOL_NAME); //设置线程名到URL参数中
        url = url.addParameterIfAbsent(Constants.THREADPOOL_KEY, Constants.DEFAULT_CLIENT_THREADPOOL);
        return ChannelHandlers.wrap(handler, url);
    }

    /**
     * @param url
     * @return 0-false
     */
    private static int getReconnectParam(URL url) { // 获取重连的时间间隔
        int reconnect;
        String param = url.getParameter(Constants.RECONNECT_KEY);
        if (param == null || param.length() == 0 || "true".equalsIgnoreCase(param)) {
            reconnect = Constants.DEFAULT_RECONNECT_PERIOD;
        } else if ("false".equalsIgnoreCase(param)) {
            reconnect = 0;
        } else {
            try {
                reconnect = Integer.parseInt(param);
            } catch (Exception e) {
                throw new IllegalArgumentException("reconnect param must be nonnegative integer or false/true. input is:" + param);
            }
            if (reconnect < 0) {
                throw new IllegalArgumentException("reconnect param must be nonnegative integer or false/true. input is:" + param);
            }
        }
        return reconnect;
    }

    /**
     * init reconnect thread（创建重连线程）
     */
    private synchronized void initConnectStatusCheckCommand() {
        //reconnect=false to close reconnect
        int reconnect = getReconnectParam(getUrl()); // 重连的时间
        if (reconnect > 0 && (reconnectExecutorFuture == null || reconnectExecutorFuture.isCancelled())) {
            Runnable connectStatusCheckCommand = new Runnable() {
                public void run() {
                    try {
                        if (!isConnected()) {
                            connect();
                        } else {
                            lastConnectedTime = System.currentTimeMillis(); //记录最后一次连接时间
                        }
                    } catch (Throwable t) {
                        String errorMsg = "client reconnect to " + getUrl().getAddress() + " find error . url: " + getUrl();
                        // wait registry sync provider list
                        if (System.currentTimeMillis() - lastConnectedTime > shutdown_timeout) { //若当前距离最后一次连接的时间超过重连的时间间隔，则终止并打印异常结果
                            if (!reconnect_error_log_flag.get()) {
                                reconnect_error_log_flag.set(true);
                                logger.error(errorMsg, t);
                                return;
                            }
                        }
                        if (reconnect_count.getAndIncrement() % reconnect_warning_period == 0) { //若重连的次数等于需要重连的次数，则打印信息
                            logger.warn(errorMsg, t);
                        }
                    }
                }
            };

            /**
             * 延时任务中initialDelay, delay,两个参数的含义研究
             * 解：initialDelay： 第一次执行时，延后多少时间执行
             * delay：第一次执行以后，按相同的延迟时间执行，
             * 比如initialDelay=2000，delay=3000 时间单位TimeUnit.MILLISECONDS、第一次延迟2秒，后面每次延迟3秒 ，如
             * 调用前时间：20200229 21:36:08
             * 调用后时间：20200229 21:38:08
             * 调用后时间：20200229 21:41:08
             * 调用后时间：20200229 21:44:08
             */
            reconnectExecutorFuture = reconnectExecutorService.scheduleWithFixedDelay(connectStatusCheckCommand, reconnect, reconnect, TimeUnit.MILLISECONDS);
        }
    }

    private synchronized void destroyConnectStatusCheckCommand() {
        try {
            if (reconnectExecutorFuture != null && !reconnectExecutorFuture.isDone()) {
                reconnectExecutorFuture.cancel(true);
                reconnectExecutorService.purge(); // purge(移除)：尝试移除工作队列中已经取消的任务
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    /**
     * 线程池工具类Executors使用，以及原生ThreadPoolExecutor使用
     *  Executors 1）Executors.newFixedThreadPool(...) 创建固定数量的线程池， 2）Executors.newCachedThreadPool()创建不限数量的线程池
     *  ThreadPoolExecutor 参数说明：
     *
     *  https://blog.csdn.net/Jack_SivenChen/article/details/53394058  ThreadPoolExecutor 参数详解（图解）
     *  https://www.jianshu.com/p/6f82b738ac58 todo @csy-v1 参数还需再了解
     *  https://www.jianshu.com/p/67add4f5939c   http://www.matools.com/api/java8(Java API)
     *  corePoolSize ：核心线程数，即线程池中能运行的线程数
     *  maximumPoolSize：线程池的最大线程数
     *  keepAliveTime：非核心线程的闲置超时时间，超过这个时间就会被回收
     *  workQueue 创建工作队列，用于存放提交的等待执行任务
     *
     *  初步思路：使用execute()添加一个任务时
     *  1）若正在运行的线程小于corePoolSize，则马上创建线程运行任务
     *  2）若正在运行的线程数量大于或等于 corePoolSize，那么将这个任务放入队列
     *  3）若队列满了
     *     3.1）若正在运行的数量小于maximumPoolSize，则创建线程运行任务
     *     3.2）若正在运行的数量大于或等于maximumPoolSize，则抛出异常，不再接受任务
     *  4) 当一个线程完成任务时，它会从队列中取下一个任务来执行
     *  5）当一个线程无事可做，超过一定的时间keepAliveTime，会做判断是否停掉线程
     *
     */
    protected ExecutorService createExecutor() {
        return Executors.newCachedThreadPool(new NamedThreadFactory(CLIENT_THREAD_POOL_NAME + CLIENT_THREAD_POOL_ID.incrementAndGet() + "-" + getUrl().getAddress(), true));
    }

    public InetSocketAddress getConnectAddress() {
        return new InetSocketAddress(NetUtils.filterLocalHost(getUrl().getHost()), getUrl().getPort());
    }

    /**
     *  dubbo本地地址与远程的区分
     *  本地地址和远程地址是相对的：对于消费者来说，提供者的地址就是远程地址；对于提供者来说，消费者的地址就是远程地址
     */
    public InetSocketAddress getRemoteAddress() {
        Channel channel = getChannel();
        if (channel == null)
            return getUrl().toInetSocketAddress();
        return channel.getRemoteAddress();
    }

    public InetSocketAddress getLocalAddress() {
        Channel channel = getChannel();
        if (channel == null)
            return InetSocketAddress.createUnresolved(NetUtils.getLocalHost(), 0);
        return channel.getLocalAddress();
    }

    public boolean isConnected() {
        Channel channel = getChannel();
        if (channel == null)
            return false;
        return channel.isConnected();
    }

    public Object getAttribute(String key) {
        Channel channel = getChannel();
        if (channel == null)
            return null;
        return channel.getAttribute(key);
    }

    public void setAttribute(String key, Object value) {
        Channel channel = getChannel();
        if (channel == null)
            return;
        channel.setAttribute(key, value);
    }

    public void removeAttribute(String key) {
        Channel channel = getChannel();
        if (channel == null)
            return;
        channel.removeAttribute(key);
    }

    public boolean hasAttribute(String key) {
        Channel channel = getChannel();
        if (channel == null)
            return false;
        return channel.hasAttribute(key);
    }

    public void send(Object message, boolean sent) throws RemotingException {
        if (send_reconnect && !isConnected()) {
            connect();
        }
        Channel channel = getChannel();
        //todo @system getChannel返回的状态是否包含null需要改进
        if (channel == null || !channel.isConnected()) {
            throw new RemotingException(this, "message can not send, because channel is closed . url:" + getUrl());
        }
        channel.send(message, sent);
    }

    /**
     *
     *
     */
    protected void connect() throws RemotingException {
        connectLock.lock();
        try {
            if (isConnected()) { //判断通道是否已经被连接
                return;
            }
            initConnectStatusCheckCommand(); //创建重连线程
            doConnect();
            if (!isConnected()) {
                throw new RemotingException(this, "Failed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                        + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                        + ", cause: Connect wait timeout: " + getTimeout() + "ms.");
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("Successed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                            + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                            + ", channel is " + this.getChannel());
                }
            }
            reconnect_count.set(0); //连接正常，不再重连
            reconnect_error_log_flag.set(false);
        } catch (RemotingException e) {
            throw e;
        } catch (Throwable e) {
            throw new RemotingException(this, "Failed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                    + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                    + ", cause: " + e.getMessage(), e);
        } finally {
            connectLock.unlock();
        }
    }

    public void disconnect() {
        connectLock.lock();
        try {
            destroyConnectStatusCheckCommand();
            try {
                Channel channel = getChannel();
                if (channel != null) {
                    channel.close();
                }
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
            }
            try {
                doDisConnect();
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
            }
        } finally {
            connectLock.unlock();
        }
    }

    // 重新连接：取消连接、建立连接
    public void reconnect() throws RemotingException {
        disconnect();
        connect();
    }

    public void close() { //关闭客户端相关连接
        try {
            if (executor != null) {
                ExecutorUtil.shutdownNow(executor, 100);
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            super.close();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            disconnect();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            doClose();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public void close(int timeout) {
        ExecutorUtil.gracefulShutdown(executor, timeout);
        close();
    }

    @Override
    public String toString() {
        return getClass().getName() + " [" + getLocalAddress() + " -> " + getRemoteAddress() + "]";
    }

    /**
     * Open client.
     *
     * @throws Throwable
     */
    protected abstract void doOpen() throws Throwable;

    /**
     * Close client.
     *
     * @throws Throwable
     */
    protected abstract void doClose() throws Throwable;

    /**
     * Connect to server.
     *
     * @throws Throwable
     */
    protected abstract void doConnect() throws Throwable;

    /**
     * disConnect to server.
     *
     * @throws Throwable
     */
    protected abstract void doDisConnect() throws Throwable;

    /**
     * Get the connected channel.
     *
     * @return channel
     */
    protected abstract Channel getChannel();

}
