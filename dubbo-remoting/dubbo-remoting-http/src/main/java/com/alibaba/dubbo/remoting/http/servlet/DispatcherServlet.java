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
package com.alibaba.dubbo.remoting.http.servlet;

import com.alibaba.dubbo.remoting.http.HttpHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 11/09 Dispatcher是拦截器吗？该模式是怎样的
 * 解：不是拦截器，是派发器，基于不同的事件采取对应的行动
 *  Java NIO浅析Reactor模式 https://juejin.im/post/6844903682509635598
 *  事件派发器模式 https://cloud.tencent.com/developer/article/1178048
 *
 * Service dispatcher Servlet.
 * @author qian.lei
 */
public class DispatcherServlet extends HttpServlet {

    private static final long serialVersionUID = 5766349180380479888L;
    private static final Map<Integer, HttpHandler> handlers = new ConcurrentHashMap<Integer, HttpHandler>(); //端口与处理类的缓存
    private static DispatcherServlet INSTANCE;

    public DispatcherServlet() {
        DispatcherServlet.INSTANCE = this;
    }

    public static void addHttpHandler(int port, HttpHandler processor) { //注册事件处理类
        handlers.put(port, processor);
    }

    public static void removeHttpHandler(int port) {
        handlers.remove(port);
    }

    public static DispatcherServlet getInstance() {
        return INSTANCE;
    }

    /**
     * 事件派发
     * 1）从请求对象request中获取端口port，查找对应的处理类HttpHandler
     * 2）若查到处理类，则调用处理类的handle方法，否则则抛出未找到服务的异常
     */
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpHandler handler = handlers.get(request.getLocalPort());
        if (handler == null) {// service not found.
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Service not found.");
        } else {
            handler.handle(request, response);
        }
    }

}