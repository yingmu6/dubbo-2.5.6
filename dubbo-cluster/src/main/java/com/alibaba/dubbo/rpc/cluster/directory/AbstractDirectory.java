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
package com.alibaba.dubbo.rpc.cluster.directory;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.cluster.RouterFactory;
import com.alibaba.dubbo.rpc.cluster.router.MockInvokersSelector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 增加router的Directory
 *
 * @author chao.liuc
 *
 * 设计模式-模板方法模式   https://juejin.im/post/5a2e42a06fb9a0452936b4f7
 * 模板方法优点（不需要将方法置为final，也是模板模式，就是把通用的算法逻辑放在父类，细节由子类实现）
 * 1）良好的封装性。把公有的不变的方法封装在父类，而子类负责实现具体逻辑。
 * 2）良好的扩展性：增加功能由子类实现基本方法扩展，符合单一职责原则和开闭原则。
 * 3）复用代码
 *
 * 缺点
 * 1）由于是通过继承实现代码复用来改变算法，灵活度会降低。
 * 2）子类的执行影响父类的结果，增加代码阅读难度。
 */
public abstract class AbstractDirectory<T> implements Directory<T> {

    // 日志输出
    private static final Logger logger = LoggerFactory.getLogger(AbstractDirectory.class);

    private final URL url;

    private volatile boolean destroyed = false; //维护着目录销毁状态

    private volatile URL consumerUrl;

    private volatile List<Router> routers;/**@c 一个目录下有多个Router */

    public AbstractDirectory(URL url) {
        this(url, null);
    }

    public AbstractDirectory(URL url, List<Router> routers) {
        this(url, url, routers);
    }

    /**
     * 构建AbstractDirectory，指定路由列表
     */
    public AbstractDirectory(URL url, URL consumerUrl, List<Router> routers) {
        if (url == null)
            throw new IllegalArgumentException("url == null");
        this.url = url;
        this.consumerUrl = consumerUrl;
        setRouters(routers);
    }

    /**
     * 获取指定Invocation调用信息对应的调用列表List<Invoker>
     * 1）按调用信息过滤doList(invocation)
     * 2）按路由器Router路由过滤
     */
    public List<Invoker<T>> list(Invocation invocation) throws RpcException {
        if (destroyed) {
            throw new RpcException("Directory already destroyed .url: " + getUrl());
        }
        List<Invoker<T>> invokers = doList(invocation); //从内存中获取调用方法对应的invoker列表，
        List<Router> localRouters = this.routers; // 若有路由器，则按路由器依次路由过滤（至少有一个MockInvokersSelector）
        if (localRouters != null && localRouters.size() > 0) { //此处有多个路由器，invokers去最优一个列表？还是有前后关联的？解：依次拿上一次的调用列表去路由invokers = router.route(invokers...)
            for (Router router : localRouters) {
                try {
                    if (router.getUrl() == null || router.getUrl().getParameter(Constants.RUNTIME_KEY, true)) {
                        invokers = router.route(invokers, getConsumerUrl(), invocation);
                    }
                } catch (Throwable t) {
                    logger.error("Failed to execute router: " + getUrl() + ", cause: " + t.getMessage(), t);
                }
            }
        }
        return invokers;
    }

    public URL getUrl() {
        return url;
    }

    public List<Router> getRouters() {
        return routers;
    }

    /**
     * 将路由列表设置到AbstractDirectory中的List<Router>
     * 1）若传入的routers为空，则初始化新的列表，若不为空，则用传入的routers构建新的列表
     * 2）获取url中router对应的路由routerkey
     *   2.1）若routerkey不为空，通过SPI获取到RouterFactory的实例
     *   2.2）获取url对应的路由实例Router，并且加到List<Router>
     * 3）构建mock invoker选择器MockInvokersSelector，并加到List<Router>
     * 4）对路由列表进行排序，将路由列表设置到AbstractDirectory中的List<Router>
     * 5）遍历当前的List<Configurator>，Configurator.configure配置每个元素
     */

    /**
     * 思路整理：
     * 为抽象目录设置路由列表routers，并且附加指定路由router
     * 1）从url查找路由key，如设置了则加到router列表
     * 2）默认都加上MockInvokersSelector
     * 3）对路由进行排序，并设置到当前AbstractDirectory的路由列表中
     */
    protected void setRouters(List<Router> routers) {
        // copy list
        routers = routers == null ? new ArrayList<Router>() : new ArrayList<Router>(routers);
        // append url router
        String routerkey = url.getParameter(Constants.ROUTER_KEY);
        if (routerkey != null && routerkey.length() > 0) {
            RouterFactory routerFactory = ExtensionLoader.getExtensionLoader(RouterFactory.class).getExtension(routerkey);
            routers.add(routerFactory.getRouter(url)); //往路由规则列表里加路由
        }
        // append mock invoker selector
        routers.add(new MockInvokersSelector()); /**@c 至少有一个路由器MockInvokersSelector */
        Collections.sort(routers);
        this.routers = routers; /**@c 路由器列表来自：（传入路由列表 + url指定的路由器 + MockInvokersSelector ）*/
    }

    public URL getConsumerUrl() {
        return consumerUrl;
    }

    public void setConsumerUrl(URL consumerUrl) {
        this.consumerUrl = consumerUrl;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void destroy() {
        destroyed = true;
    }

    protected abstract List<Invoker<T>> doList(Invocation invocation) throws RpcException; /**@c 此处是如何选择StaticDirectory和RegistryDirectory？解：通过抽象类实现多态的方式 */

}