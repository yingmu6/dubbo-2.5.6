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
package com.alibaba.dubbo.rpc.protocol.hessian;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;

import com.caucho.hessian.HessianException;
import com.caucho.hessian.client.HessianConnectionException;
import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.io.HessianMethodSerializationException;
import com.caucho.hessian.server.HessianSkeleton;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * http rpc support.
 * Hessian是一个轻量级的RPC框架，它基于HTTP协议传输，使用Hessian二进制序列化，对于数据包比较大的情况比较友好
 *  相比WebService，Hessian更简单、快捷。采用的是二进制RPC协议，因为采用的是二进制协议，所以它很适合于发送二进制数据。
 *
 * Hessian 是 Caucho 开源的一个 RPC 框架，其通讯效率高于 WebService 和 Java 自带的序列化。
 * http://dubbo.apache.org/zh-cn/docs/2.7/user/references/protocol/hessian/ dubbo官网
 * http://hessian.caucho.com/  Hessian官方网址
 *
 * @author qianlei
 */
public class HessianProtocol extends AbstractProxyProtocol {

    private final Map<String, HttpServer> serverMap = new ConcurrentHashMap<String, HttpServer>(); //服务缓存

    private final Map<String, HessianSkeleton> skeletonMap = new ConcurrentHashMap<String, HessianSkeleton>(); // skeleton:骨架，HessianSkeleton：Proxy class for Hessian services（Hessian服务的代理类）

    private HttpBinder httpBinder; //http绑定服务，默认是jetty

    public HessianProtocol() {
        super(HessianException.class);
    }

    public void setHttpBinder(HttpBinder httpBinder) {
        this.httpBinder = httpBinder;
    }

    public int getDefaultPort() {
        return 80;
    }

    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException { //11/06 这是协议的默认实现吗？解：是
        String addr = url.getIp() + ":" + url.getPort();
        HttpServer server = serverMap.get(addr);
        if (server == null) { // 若本地缓存没有，则绑定url生成server
            server = httpBinder.bind(url, new HessianHandler());
            serverMap.put(addr, server);
        }
        final String path = url.getAbsolutePath();
        HessianSkeleton skeleton = new HessianSkeleton(impl, type);
        skeletonMap.put(path, skeleton); // https://www.jianshu.com/p/99c87815fc31  Dubbo Rest服务发布流程
        return new Runnable() {
            public void run() {
                skeletonMap.remove(path); // @csy-v1 此处为啥要移除key？ 暂不了解
            }
        };
    }

    @SuppressWarnings("unchecked")
    protected <T> T doRefer(Class<T> serviceType, URL url) throws RpcException {
        HessianProxyFactory hessianProxyFactory = new HessianProxyFactory();
        String client = url.getParameter(Constants.CLIENT_KEY, Constants.DEFAULT_HTTP_CLIENT);
        if ("httpclient".equals(client)) {
            hessianProxyFactory.setConnectionFactory(new HttpClientConnectionFactory());
        } else if (client != null && client.length() > 0 && !Constants.DEFAULT_HTTP_CLIENT.equals(client)) {
            throw new IllegalStateException("Unsupported http protocol client=\"" + client + "\"!");
        }
        int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        hessianProxyFactory.setConnectTimeout(timeout);
        hessianProxyFactory.setReadTimeout(timeout);
        return (T) hessianProxyFactory.create(serviceType, url.setProtocol("http").toJavaURL(), Thread.currentThread().getContextClassLoader());
    }

    protected int getErrorCode(Throwable e) {
        if (e instanceof HessianConnectionException) {
            if (e.getCause() != null) {
                Class<?> cls = e.getCause().getClass();
                if (SocketTimeoutException.class.equals(cls)) {
                    return RpcException.TIMEOUT_EXCEPTION;
                }
            }
            return RpcException.NETWORK_EXCEPTION;
        } else if (e instanceof HessianMethodSerializationException) {
            return RpcException.SERIALIZATION_EXCEPTION;
        }
        return super.getErrorCode(e);
    }

    public void destroy() {
        super.destroy();
        for (String key : new ArrayList<String>(serverMap.keySet())) {
            HttpServer server = serverMap.remove(key);
            if (server != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close hessian server " + server.getUrl());
                    }
                    server.close();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
    }

    private class HessianHandler implements HttpHandler {

        public void handle(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            String uri = request.getRequestURI();
            HessianSkeleton skeleton = skeletonMap.get(uri);
            if (!request.getMethod().equalsIgnoreCase("POST")) {
                response.setStatus(500);
            } else {
                RpcContext.getContext().setRemoteAddress(request.getRemoteAddr(), request.getRemotePort()); //设置上下文中的远程地址和端口（客户端client的地址）
                try {
                    skeleton.invoke(request.getInputStream(), response.getOutputStream());
                } catch (Throwable e) {
                    throw new ServletException(e);
                }
            }
        }

    }

    /**
     * 11/09 Servlet了解以及ServletRequest、ServletResponse
     * Servlet: 在服务器上运行的小型应用程序，用于处理web请求的逻辑
     * 工作模式：
     * 1）客户端发送请求至服务器
     * 2）服务器启动并调用Servlet，Servlet根据客户端请求生成响应内容并将其传给服务器
     * 3）服务器将响应返回客户端
     *
     * 生命周期：
     * servlet在服务器的运行生命周期为，在第一次请求（或其实体被内存垃圾回收后再被访问）时被加载并执行一次初始化方法，
     * 跟着执行正式运行方法，之后会被常驻并每次被请求时直接执行正式运行方法，直到服务器关闭或被清理时执行一次销毁方法后实体销毁。
     *
     * https://zh.wikipedia.org/wiki/Java_Servlet Java Servlet - 维基百科
     * https://www.cnblogs.com/rickiyang/p/12764615.html Servlet 和 Servlet容器
     */

    /**
     * 11/09 HessianSkeleton了解
     * 1） https://blog.csdn.net/qq924862077/article/details/52799553 HessianSkeleton分析
     * HessianSkeleton是Hessian的服务端的核心，简单总结来说：HessianSkeleton根据客户端请求的链接，获取到需要执行的接口及实现类，
     * 对客户端发送过来的二进制数据进行反序列化，获得需要执行的函数及参数值，然后根据函数和参数值执行具体的函数，接下来对执行的结果进行序列化然后通过连接返回给客户端
     *
     * RPC机制就是客户端发送给服务端想要调用的函数及参数值，服务端通过客户端发送过来的函数和参数值通过反射机制进行函数调用执行，然后将执行结果返回给客户端，这样一个RPC的调用过程结束了
     *
     * 2）https://www.cnblogs.com/wynjauu/articles/9010719.html  Hessian的使用以及理解
     */

}