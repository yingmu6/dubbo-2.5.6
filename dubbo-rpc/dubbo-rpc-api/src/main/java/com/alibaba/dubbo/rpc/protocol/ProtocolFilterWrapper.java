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
package com.alibaba.dubbo.rpc.protocol;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.List;

/**
 * ListenerProtocol
 * @author william.liangf
 */
public class ProtocolFilterWrapper implements Protocol {// read finish  11/04 协议封装是在哪里引用的？解：这个Protocol的过滤封装类，在com.alibaba.dubbo.rpc.Protocol配置的filter值

    //ProtocolFilterWrapper与ProtocolListenerWrapper的区别？
    private final Protocol protocol;

    public ProtocolFilterWrapper(Protocol protocol) { //Protocol的封装类
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null");
        }
        this.protocol = protocol;
    }

    /**
     * 构建调用链（Filter：过滤器，具有拦截过滤的作用）
     * https://juejin.im/post/5ad40ee1f265da2375075a23  Filter链原理
     * invoker的实例是JavassistProxyFactory（ProxyFactory的默认实现）生成的代理对象
     * 提供者：key列如：service.filter，group列如：provider，
     * 消费者：key列如：reference.filter，group列如：consumer
     *
     * 11/04 Activate中group的provider、consumer是怎么分组的？ 解：ExtensionLoader中getActivateExtension会按@Activate声明的values、group进行比较
     */
    private static <T> Invoker<T> buildInvokerChain(final Invoker<T> invoker, String key, String group) {
        Invoker<T> last = invoker;
        /**
         * 11/04 是怎么得到EchoFilter、ClassLoaderFilter、ExceptionFilter等基础过滤器的？
         * 解：会加载所有模块，指定目录对应的dubbo配置文件，并解析文件中的内容
         */
        List<Filter> filters = ExtensionLoader.getExtensionLoader(Filter.class).getActivateExtension(invoker.getUrl(), key, group);
        if (filters.size() > 0) {
            for (int i = filters.size() - 1; i >= 0; i--) {//过滤器会从最后一个开始执行
                final Filter filter = filters.get(i);
                /**
                 * 对象的拷贝 https://blog.csdn.net/ztchun/article/details/79110096
                 * 对象的赋值，是引用的赋值，两个对象会指向内存堆中同一个对象，其中一个值的改变，会引起另一个值的改变
                 * 拷贝的目的：两个对象属性的值相同，但互不影响
                 * 浅复制：实现Cloneable接口，重写Object的clone() 方法
                 * 深复制：实现Cloneable接口，对象以及对象中的对象 都要重写Object的clone() 方法
                 */

                /**
                 * 11/04 此处的链表是怎么处理的？链表待实践了解
                 * 解：如过滤链中Filter的顺序为A、B、C（自定义的filter在最后，即为C）
                 * 1）先倒排，为C、B、A
                 * 2）然后依次各个filter构建的invoker，如C->invoker，B->C->invoker，A->B->C->invoker
                 * 3）最后实际调用时，是A->B->C->invoker
                 * 看filter.invoke(next, invocation)调用，以及Invoker<T> next = last
                 */
                final Invoker<T> next = last; //调试看节点中的next，可以看到引用的层级关系
                last = new Invoker<T>() { //11/04 匿名类了解、向上转型了解，11/05-done

                    public Class<T> getInterface() {
                        return invoker.getInterface();
                    }

                    public URL getUrl() {
                        return invoker.getUrl();
                    }

                    public boolean isAvailable() {
                        return invoker.isAvailable();
                    }

                    /**
                     * Java 实现单向列表  https://www.cnblogs.com/alsf/p/5520266.html
                     * https://www.jianshu.com/p/73d56c3d228c 链表的数据结构
                     */
                    public Result invoke(Invocation invocation) throws RpcException {
                        return filter.invoke(next, invocation); //执行具体Filter实例的调用invoke
                    }

                    public void destroy() {
                        invoker.destroy();
                    }

                    @Override
                    public String toString() {
                        return invoker.toString();
                    }
                };
            }
        }
        return last;
    }

    public int getDefaultPort() {
        return protocol.getDefaultPort();
    }

    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        if (Constants.REGISTRY_PROTOCOL.equals(invoker.getUrl().getProtocol())) {
            return protocol.export(invoker);
        }
        /**
         * 服务暴露前，服务先经过过滤链处理，再做暴露（过滤链的key为service.filter，group为provider）
         * service.filter是<dubbo:service filter=""/> 定义的filter，没有指定的，会使用系统默认的filter
         */
        return protocol.export(buildInvokerChain(invoker, Constants.SERVICE_FILTER_KEY, Constants.PROVIDER));
    }
    /**@c 在Protocol中refer执行以后执行*/
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
            return protocol.refer(type, url);
        }
        /**
         * 对引用的服务做过滤链处理（过滤链的key为reference.filter，group为consumer）
         */
        return buildInvokerChain(protocol.refer(type, url), Constants.REFERENCE_FILTER_KEY, Constants.CONSUMER);
    }

    public void destroy() {
        protocol.destroy();
    }

}