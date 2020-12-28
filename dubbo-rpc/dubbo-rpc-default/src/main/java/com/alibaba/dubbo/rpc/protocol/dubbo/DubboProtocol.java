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
package com.alibaba.dubbo.rpc.protocol.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Transporter;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.remoting.exchange.Exchangers;
import com.alibaba.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.protocol.AbstractProtocol;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * dubbo protocol support.（DubboProtocol是Protocol的默认协议，若没指定协议，就会进入此类的方法）
 *
 * @author qian.lei
 * @author william.liangf
 * @author chao.liuc
 */
public class DubboProtocol extends AbstractProtocol {
    /**
     * 数据结构 -》
     *
     * 类继承关系：
     * 1）DubboProtocol继承了AbstractProtocol抽象类
     * 2）AbstractProtocol实现了Protocol接口
     *
     * 包含的数据：
     * 1）从父类继承的数据：AbstractProtocol
     *   Map<String, Exporter<?>> exporterMap 暴露服务的缓存，key为ServiceKey，置为Exporter
     *   Set<Invoker<?>> invokers 服务执行者列表，如同一个服务有多个提供者
     *
     * 2）当前类维护的数据：DubboProtocol
     *   NAME（协议名）、DEFAULT_PORT（默认端口）、DubboProtocol INSTANCE 实例的缓存
     *   ExchangeServer（交换服务）、ReferenceCountExchangeClient（引用计数）
     *   LazyConnectExchangeClient（延迟连接）、stubServiceMethodsMap（存根方法的映射）
     *   ExchangeHandler requestHandler（请求回复的事件处理）
     *
     * 包含的功能：
     * 1）<T> Exporter<T> export(Invoker<T> invoker)   服务暴露
     * 2）<T> Invoker<T> refer(Class<T> type, URL url) 服务引用
     * 3）void destroy() 释放协议
     */
    public static final String NAME = "dubbo";

    public static final int DEFAULT_PORT = 20880;
    // history-v2 此key值的用途是啥？
    private static final String IS_CALLBACK_SERVICE_INVOKE = "_isCallBackServiceInvoke";
    private static DubboProtocol INSTANCE;
    private final Map<String, ExchangeServer> serverMap = new ConcurrentHashMap<String, ExchangeServer>(); // <host:port,Exchanger>
    private final Map<String, ReferenceCountExchangeClient> referenceClientMap = new ConcurrentHashMap<String, ReferenceCountExchangeClient>(); // <host:port,Exchanger>
    private final ConcurrentMap<String, LazyConnectExchangeClient> ghostClientMap = new ConcurrentHashMap<String, LazyConnectExchangeClient>(); //本地缓存文件
    //consumer side export a stub service for dispatching event
    //servicekey-stubmethods
    private final ConcurrentMap<String, String> stubServiceMethodsMap = new ConcurrentHashMap<String, String>();
    /**
     * 创建客户端请求处理类 ExchangeHandler（回复客户端reply）
     * 1）判断回复的消息message是不是调用信息Invocation的实例，若不是则抛出异常
     * 2）通过Channel和Invocation 通道信息和调用信息获取invoker， getInvoker(channel, inv)
     * 3）对回调方法进行判断hasMethod，若调用信息Invocation中不存在回调方法，则提示并返回
     * 4）设置上下文远程地址，并执行调用invoker.invoke(inv)
     */
    private ExchangeHandler requestHandler = new ExchangeHandlerAdapter() { /**@c history-h1 成员变量若有对象引用，在什么时候创建的 export中没有调用此方法*/

        //reply 回答、答复(服务端调用、回复客户端)
        public Object reply(ExchangeChannel channel, Object message) throws RemotingException { //history-h1 逻辑待覆盖
            if (message instanceof Invocation) {
                Invocation inv = (Invocation) message; //message转化为的会话信息
                Invoker<?> invoker = getInvoker(channel, inv);
                //如果是callback 需要处理高版本调用低版本的问题
                if (Boolean.TRUE.toString().equals(inv.getAttachments().get(IS_CALLBACK_SERVICE_INVOKE))) {
                    String methodsStr = invoker.getUrl().getParameters().get("methods");
                    boolean hasMethod = false;
                    if (methodsStr == null || methodsStr.indexOf(",") == -1) { //没有方法名或只有一个方法名
                        hasMethod = inv.getMethodName().equals(methodsStr);
                    } else {
                        String[] methods = methodsStr.split(",");
                        for (String method : methods) {
                            if (inv.getMethodName().equals(method)) {
                                hasMethod = true;
                                break;
                            }
                        }
                    }
                    if (!hasMethod) { //判断回调方法是否存在于URL方法名列表中
                        logger.warn(new IllegalStateException("The methodName " + inv.getMethodName() + " not found in callback service interface ,invoke will be ignored. please update the api interface. url is:" + invoker.getUrl()) + " ,invocation is :" + inv);
                        return null;
                    }
                }
                ////回复客户端（服务端主动调用客户端）
                RpcContext.getContext().setRemoteAddress(channel.getRemoteAddress());
                return invoker.invoke(inv);
            }
            throw new RemotingException(channel, "Unsupported request: " + message == null ? null : (message.getClass().getName() + ": " + message) + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress());
        }

        @Override
        public void received(Channel channel, Object message) throws RemotingException {
            if (message instanceof Invocation) {//回复或者接收
                reply((ExchangeChannel) channel, message);
            } else {
                super.received(channel, message);
            }
        }

        @Override
        public void connected(Channel channel) throws RemotingException {
            invoke(channel, Constants.ON_CONNECT_KEY);
        }

        @Override
        public void disconnected(Channel channel) throws RemotingException {
            if (logger.isInfoEnabled()) {
                logger.info("disconected from " + channel.getRemoteAddress() + ",url:" + channel.getUrl());
            }
            invoke(channel, Constants.ON_DISCONNECT_KEY);
        }

        private void invoke(Channel channel, String methodKey) {
            Invocation invocation = createInvocation(channel, channel.getUrl(), methodKey);
            if (invocation != null) {
                try {
                    received(channel, invocation);
                } catch (Throwable t) {
                    logger.warn("Failed to invoke event method " + invocation.getMethodName() + "(), cause: " + t.getMessage(), t);
                }
            }
        }

        //创建调用信息
        private Invocation createInvocation(Channel channel, URL url, String methodKey) {
            String method = url.getParameter(methodKey);//methodKey的值是怎样？onconnect等
            if (method == null || method.length() == 0) {
                return null;
            }
            RpcInvocation invocation = new RpcInvocation(method, new Class<?>[0], new Object[0]);
            invocation.setAttachment(Constants.PATH_KEY, url.getPath());
            invocation.setAttachment(Constants.GROUP_KEY, url.getParameter(Constants.GROUP_KEY));
            invocation.setAttachment(Constants.INTERFACE_KEY, url.getParameter(Constants.INTERFACE_KEY));
            invocation.setAttachment(Constants.VERSION_KEY, url.getParameter(Constants.VERSION_KEY));
            if (url.getParameter(Constants.STUB_EVENT_KEY, false)) {
                invocation.setAttachment(Constants.STUB_EVENT_KEY, Boolean.TRUE.toString());
            }
            return invocation;
        }
    };

    public DubboProtocol() {
        INSTANCE = this;
    }

    public static DubboProtocol getDubboProtocol() {
        if (INSTANCE == null) {
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(DubboProtocol.NAME); // load
        }
        return INSTANCE;
    }

    public Collection<ExchangeServer> getServers() {
        return Collections.unmodifiableCollection(serverMap.values());
    }

    public Collection<Exporter<?>> getExporters() {
        return Collections.unmodifiableCollection(exporterMap.values());
    }

    Map<String, Exporter<?>> getExporterMap() {
        return exporterMap;
    }

    //是否是客户端
    private boolean isClientSide(Channel channel) {
        InetSocketAddress address = channel.getRemoteAddress();
        URL url = channel.getUrl(); //服务暴露提供的url
        return url.getPort() == address.getPort() &&
                NetUtils.filterLocalHost(channel.getUrl().getIp())
                        .equals(NetUtils.filterLocalHost(address.getAddress().getHostAddress()));
        /**
         * 11/16 此处比较逻辑待了解
         * 解：通道中的远程地址和url中的地址相同，表明是客户端
         */
    }

    /**
     * 获取Invoker（查找缓存中的export，并获取对应的invoker，exporter.getInvoker()）
     * 1）构建serviceKey ，path、port、version、group
     * 2）从缓存中获取export，exporterMap.get(serviceKey)
     * 3）调用export中方法获取invoker，exporter.getInvoker()
     */
    Invoker<?> getInvoker(Channel channel, Invocation inv) throws RemotingException {
        boolean isCallBackServiceInvoke = false;
        boolean isStubServiceInvoke = false;
        int port = channel.getLocalAddress().getPort();
        String path = inv.getAttachments().get(Constants.PATH_KEY);
        //如果是客户端的回调服务.
        isStubServiceInvoke = Boolean.TRUE.toString().equals(inv.getAttachments().get(Constants.STUB_EVENT_KEY));
        if (isStubServiceInvoke) { //是存根调用，就选remote地址中的port，否则去local地址中port
            port = channel.getRemoteAddress().getPort();
        }
        //callback 判断是否是回调服务（是客户端且不是存根调用，即为回调调用）
        isCallBackServiceInvoke = isClientSide(channel) && !isStubServiceInvoke;
        if (isCallBackServiceInvoke) { //判断是否回调，若是，path需要拼接，否是直接从会话参数中取 inv.getAttachments().get(Constants.PATH_KEY)
            path = inv.getAttachments().get(Constants.PATH_KEY) + "." + inv.getAttachments().get(Constants.CALLBACK_SERVICE_KEY);
            inv.getAttachments().put(IS_CALLBACK_SERVICE_INVOKE, Boolean.TRUE.toString());
        }
        String serviceKey = serviceKey(port, path, inv.getAttachments().get(Constants.VERSION_KEY), inv.getAttachments().get(Constants.GROUP_KEY));

        //构建serviceKey 然后通过DubboExporter获取Invoker
        DubboExporter<?> exporter = (DubboExporter<?>) exporterMap.get(serviceKey);

        if (exporter == null)
            throw new RemotingException(channel, "Not found exported service: " + serviceKey + " in " + exporterMap.keySet() + ", may be version or group mismatch " + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress() + ", message:" + inv);

        return exporter.getInvoker();
    }

    public Collection<Invoker<?>> getInvokers() {
        return Collections.unmodifiableCollection(invokers);
    }

    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    //重点：将invoker转换为exporter，Invoker由框架传入

    /**
     * 暴露服务：打开服务并返回构建的服务
     * 1）通过invoker构建DubboExporter，并写入缓存
     * 2）检查存根方法是否正确
     * 3）打开服务并返回DubboExporter
     */
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        URL url = invoker.getUrl();

        // export service.（根据执行者信息，构造服务暴露引用的信息）
        String key = serviceKey(url); //与exporter映射的key（格式：group/interface:version:port）如：com.alibaba.dubbo.demo.ApiDemo:1.3.4:20881
        DubboExporter<T> exporter = new DubboExporter<T>(invoker, key, exporterMap);
        exporterMap.put(key, exporter); //将dubbo export按serviceKey 缓存起来

        //export an stub service for dispaching event
        //stub method方法是啥？ 解：本地存根，把部分逻辑放在客户端实现,默认为false
        Boolean isStubSupportEvent = url.getParameter(Constants.STUB_EVENT_KEY, Constants.DEFAULT_STUB_EVENT);
        /**@c 回调方法的用途？ 服务端调用客户端逻辑（一般都是客户端调用服务端） */
        Boolean isCallbackservice = url.getParameter(Constants.IS_CALLBACK_SERVICE, false);
        if (isStubSupportEvent && !isCallbackservice) {/**@c 是本地存根 但不是参数回调*/
            //获取本地存根方法，若为空，则打印非法状态异常，否则记录下存根方法
            String stubServiceMethods = url.getParameter(Constants.STUB_EVENT_METHODS_KEY); //11/14 存根能指定方法？STUB_EVENT_METHODS_KEY是怎么设置的？解：StubProxyFactoryWrapper中getProxy设置的
            if (stubServiceMethods == null || stubServiceMethods.length() == 0) {
                if (logger.isWarnEnabled()) {//若支持了stub，就需要stud method
                    logger.warn(new IllegalStateException("consumer [" + url.getParameter(Constants.INTERFACE_KEY) +
                            "], has set stubproxy support event ,but no stub methods founded."));
                }
            } else {
                stubServiceMethodsMap.put(url.getServiceKey(), stubServiceMethods);
            }
        }
        //打开服务
        openServer(url);

        return exporter;
    }

    /**
     * 打开服务端 -- 代码流程：
     * 1）判断是否是服务端，只有服务端才打开服务，客户端不处理
     * 2）从本地缓存的交换服务获取key对应的服务，若不存在，则重新创建服务，否则重置服务的配置reset(url)
     */
    private void openServer(URL url) {
        // find server.
        String key = url.getAddress(); //形式为 host:port或host
        //client 也可以暴露一个只有server可以调用的服务。
        boolean isServer = url.getParameter(Constants.IS_SERVER_KEY, true);
        if (isServer) {
            ExchangeServer server = serverMap.get(key);
            if (server == null) {
                serverMap.put(key, createServer(url)); //重点：创建服务
            } else {
                //server支持reset,配合override功能使用
                server.reset(url); //是怎么选择调用ExchangeServerDelegate、HeaderExchangeServer？解：是从serverMap缓存中获取的，根据createServer(url)选择
            }
        }
    }

    /**
     * 创建指定url对应的交换服务ExchangeServer
     * 1）设置服务相关参数，如channel.readonly.sent通道只读事件、heartbeat心跳检测时间、codec编码方式等
     * 2）把通道处理类绑定到指定url上，并返回交换服务ExchangeServer
     */
    private ExchangeServer createServer(URL url) {
        //默认开启server关闭时发送readonly事件
        url = url.addParameterIfAbsent(Constants.CHANNEL_READONLYEVENT_SENT_KEY, Boolean.TRUE.toString());
        //默认开启heartbeat(设置心跳检测时间，默认每隔60秒检查一次)
        url = url.addParameterIfAbsent(Constants.HEARTBEAT_KEY, String.valueOf(Constants.DEFAULT_HEARTBEAT)); /**@c 会启动心跳定时任务，每隔指定时间检查心跳*/
        /**@c 默认使用Netty作为服务端 */
        String str = url.getParameter(Constants.SERVER_KEY, Constants.DEFAULT_REMOTING_SERVER);

        //判断Transporter是否有指定的扩展str，如netty，(判断扩展名是否存在缓存列表中)
        if (str != null && str.length() > 0 && !ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(str))
            throw new RpcException("Unsupported server type: " + str + ", url: " + url);

        url = url.addParameter(Constants.CODEC_KEY, DubboCodec.NAME);
        ExchangeServer server;
        try {
            //重要：requestHandler 请求处理器，注意看初始化的地方，匿名对象
            server = Exchangers.bind(url, requestHandler); /**@c 构建服务并且打开服务*/
        } catch (RemotingException e) {
            throw new RpcException("Fail to start server(url: " + url + ") " + e.getMessage(), e);
        }
        str = url.getParameter(Constants.CLIENT_KEY);
        if (str != null && str.length() > 0) {
            Set<String> supportedTypes = ExtensionLoader.getExtensionLoader(Transporter.class).getSupportedExtensions();
            if (!supportedTypes.contains(str)) {
                throw new RpcException("Unsupported client type: " + str);
            }
        }
        return server;
    }

    /**
     * 引用服务（引用某种类型type，数据内容为url的服务，返回服务的执行者invoker）
     * 1）构建DubboInvoker对象
     * 2）将invoker实例，加到Set<Invoker<?>>集合中
     * 3）返回实例Invoker
     * 不管是服务端还是客户端，所有的调用都往invoker靠
     */
    public <T> Invoker<T> refer(Class<T> serviceType, URL url) throws RpcException {
        // create rpc invoker.
        DubboInvoker<T> invoker = new DubboInvoker<T>(serviceType, url, getClients(url), invokers);
        invokers.add(invoker);
        return invoker;
    }

    /**
     * 通过url获取ExchangeClient 交换客户端
     * 1）从url中获取connections的配置，若没有设置连接数，则为共享连接
     * 2）遍历ExchangeClient数组
     *   2.1）若是共享连接，通过getSharedClient(url)获取共享连接
     *   2.2）若是每服务，每连接， 通过initClient(url)获取连接
     */
    private ExchangeClient[] getClients(URL url) {
        //通过连接数判断是否共享连接
        boolean service_share_connect = false;
        int connections = url.getParameter(Constants.CONNECTIONS_KEY, 0);
        //如果connections不配置，则共享连接，否则每服务每连接
        if (connections == 0) {
            service_share_connect = true;
            connections = 1;
        }

        //接口也能是数组元素
        ExchangeClient[] clients = new ExchangeClient[connections];
        for (int i = 0; i < clients.length; i++) {
            if (service_share_connect) {//共享连接（统计调用次数）
                clients[i] = getSharedClient(url);
            } else {                   //每服务每连接
                clients[i] = initClient(url);
            }
        }
        return clients;
    }

    /**
     * 获取共享连接（若不存在client，则创建；若存在client并且没有关闭，引用计数加1后直接返回）
     * 1）以通讯地址为key（host或host + ":" + port），从Map<String, ReferenceCountExchangeClient>
     *     获取到ReferenceCountExchangeClient的实例
     * 2）判断ReferenceCountExchangeClient是否为空，若为空则不处理
     *     若client没有关闭，则将AtomicInteger refenceCount引用计数加1
     *     若client关闭了，则将改key从referenceClientMap移除
     * 3）同步块处理
     *     3.1）initClient初始化client，获取到ExchangeClient
     *     3.2）构建ReferenceCountExchangeClient实例，赋值client
     *     3.3）referenceClientMap设置key对应的client值
     *         移除延迟client  ghostClientMap.remove(key)
     */
    private ExchangeClient getSharedClient(URL url) {
        String key = url.getAddress();
        ReferenceCountExchangeClient client = referenceClientMap.get(key);
        if (client != null) {//初始时client为NUll
            if (!client.isClosed()) {//如果没有关闭，则原子自增
                client.incrementAndGetCount();
                return client;
            } else { // 已经关闭的，但是referenceClientMap没有被移除（history-v2 这种会在什么场景出现？ ）
                referenceClientMap.remove(key);
            }
        }
        synchronized (key.intern()) {//返回字符串的规范表示
            ExchangeClient exchangeClient = initClient(url);
            client = new ReferenceCountExchangeClient(exchangeClient, ghostClientMap);
            referenceClientMap.put(key, client);
            ghostClientMap.remove(key);//移除延迟client
            return client;
        }
    }

    /**
     * 创建Client（没有判断客户端是否存在，每个请求服务就创建一个连接）
     * 1）获取传输方式，client尝试从url获取"client"键对应的值，若没有则取"server"键对应的值，"server"默认的值为"netty"
     *    配置先从客户端参数查找，若没有查到，则从服务端查找（此处体现出参数优先级，覆盖的原则 consumer > provider）
     * 2）添加参数到url中，参数有codec：编解码方式 默认为DubboCodec，heartbeat 心跳检测时间，默认60秒
     * 3）检测url中传输方式Transporter，是否支持
     * 4）判断是否设置了延迟连接
     *   4.1）若是延迟连接，则通过LazyConnectExchangeClient构建客户端Client
     *   4.2）若不是延迟连接，则通过Exchangers.connect 创建客户端Client
     */
    private ExchangeClient initClient(URL url) {

        // client type setting.
        String str = url.getParameter(Constants.CLIENT_KEY, url.getParameter(Constants.SERVER_KEY, Constants.DEFAULT_REMOTING_CLIENT));

        String version = url.getParameter(Constants.DUBBO_VERSION_KEY);
        boolean compatible = (version != null && version.startsWith("1.0."));
        url = url.addParameter(Constants.CODEC_KEY, DubboCodec.NAME);
        //默认开启heartbeat
        url = url.addParameterIfAbsent(Constants.HEARTBEAT_KEY, String.valueOf(Constants.DEFAULT_HEARTBEAT));

        // BIO存在严重性能问题，暂时不允许使用
        if (str != null && str.length() > 0 && !ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(str)) {
            throw new RpcException("Unsupported client type: " + str + "," +
                    " supported client type is " + StringUtils.join(ExtensionLoader.getExtensionLoader(Transporter.class).getSupportedExtensions(), " "));
        }

        ExchangeClient client;
        try {
            //设置连接应该是lazy的 
            if (url.getParameter(Constants.LAZY_CONNECT_KEY, false)) {
                client = new LazyConnectExchangeClient(url, requestHandler);
            } else {
                client = Exchangers.connect(url, requestHandler);
            }
        } catch (RemotingException e) {
            throw new RpcException("Fail to create remoting client for service(" + url + "): " + e.getMessage(), e);
        }
        return client;
    }

    public void destroy() { // 当服务停止时，临时节点被删除
        for (String key : new ArrayList<String>(serverMap.keySet())) {
            ExchangeServer server = serverMap.remove(key);
            if (server != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close dubbo server: " + server.getLocalAddress());
                    }
                    server.close(getServerShutdownTimeout());
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }

        for (String key : new ArrayList<String>(referenceClientMap.keySet())) {
            ExchangeClient client = referenceClientMap.remove(key);
            if (client != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close dubbo connect: " + client.getLocalAddress() + "-->" + client.getRemoteAddress());
                    }
                    client.close(getServerShutdownTimeout());
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }

        for (String key : new ArrayList<String>(ghostClientMap.keySet())) {
            ExchangeClient client = ghostClientMap.remove(key);
            if (client != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close dubbo connect: " + client.getLocalAddress() + "-->" + client.getRemoteAddress());
                    }
                    client.close(getServerShutdownTimeout());
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
        stubServiceMethodsMap.clear();
        super.destroy();
    }
}