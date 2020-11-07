/*
 * Copyright 1999-2012 Alibaba Group.
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

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * history-v1 代理协议的理解？
 * AbstractProxyProtocol
 *
 * @author william.liangf
 */
public abstract class AbstractProxyProtocol extends AbstractProtocol {// read finish

    //history-v1 既有export，又有refer，用途？CopyOnWriteArrayList的用途？
    private final List<Class<?>> rpcExceptions = new CopyOnWriteArrayList<Class<?>>();
    ;

    private ProxyFactory proxyFactory;

    public AbstractProxyProtocol() {
    }

    public AbstractProxyProtocol(Class<?>... exceptions) {
        for (Class<?> exception : exceptions) {
            addRpcException(exception);
        }
    }

    public void addRpcException(Class<?> exception) {
        this.rpcExceptions.add(exception);
    }

    public ProxyFactory getProxyFactory() {
        return proxyFactory;
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    /**
     * 代码流程：
     * 方法目的：暴露服务，并将invoker转换为export
     * 1）通过服务key，serviceGroup/serviceName:serviceVersion:port
     *    从本地缓存的Map中查找expoter对象，若查到则返回export对象
     * 2）创建invoker的代理对象 proxyFactory.getProxy(invoker)
     * 3）通过doExport 暴露服务，产生运行线程Runnable
     * 4）AbstractExporter通过invoker构造exporter对象
     *    并重写exporter的unexport，在解除暴露时，根据key移除exporter，若线程不会空时，要继续运行线程
     *    并且写入到exporterMap中，下次直接重本地缓存中获取，不需要多次暴露
     */
    @SuppressWarnings("unchecked")
    public <T> Exporter<T> export(final Invoker<T> invoker) throws RpcException {
        //uri内容是啥？exporterMap的内容？ 解：通过uri构建缓存的key，ServiceKey格式 serviceGroup/serviceName:serviceVersion:port
        final String uri = serviceKey(invoker.getUrl());
        Exporter<T> exporter = (Exporter<T>) exporterMap.get(uri);
        if (exporter != null) {/**@c 判断本地缓存是否存在exporter 若存在则直接返回缓存中暴露者exporter*/
            return exporter;
        }
        /**
         * 创建代理待研究 暴露了什么内容？todo 11/06 此处待了解
         * 表层含义：将某个invoker的某个接口interface进行暴露，接口中的参数从url获取
         */
        final Runnable runnable = doExport(proxyFactory.getProxy(invoker), invoker.getInterface(), invoker.getUrl());
        exporter = new AbstractExporter<T>(invoker) { //通过invoker构造exporter
            public void unexport() { //重写取消暴露
                super.unexport();
                exporterMap.remove(uri);
                if (runnable != null) { // 取消暴露时，若运行的线程没有结束，继续运行
                    try {
                        runnable.run();
                    } catch (Throwable t) {
                        logger.warn(t.getMessage(), t);
                    }
                }
            }
        };
        exporterMap.put(uri, exporter);
        return exporter;
    }

    public <T> Invoker<T> refer(final Class<T> type, final URL url) throws RpcException {
        final Invoker<T> tagert = proxyFactory.getInvoker(doRefer(type, url), type, url);
        Invoker<T> invoker = new AbstractInvoker<T>(type, url) {
            @Override
            protected Result doInvoke(Invocation invocation) throws Throwable {
                try {
                    Result result = tagert.invoke(invocation);
                    Throwable e = result.getException();
                    if (e != null) {
                        for (Class<?> rpcException : rpcExceptions) {
                            if (rpcException.isAssignableFrom(e.getClass())) {
                                throw getRpcException(type, url, invocation, e);
                            }
                        }
                    }
                    return result;
                } catch (RpcException e) {
                    if (e.getCode() == RpcException.UNKNOWN_EXCEPTION) {
                        e.setCode(getErrorCode(e.getCause()));
                    }
                    throw e;
                } catch (Throwable e) {
                    throw getRpcException(type, url, invocation, e);
                }
            }
        };
        invokers.add(invoker);
        return invoker;
    }

    protected RpcException getRpcException(Class<?> type, URL url, Invocation invocation, Throwable e) {
        RpcException re = new RpcException("Failed to invoke remote service: " + type + ", method: "
                + invocation.getMethodName() + ", cause: " + e.getMessage(), e);
        re.setCode(getErrorCode(e));
        return re;
    }

    protected int getErrorCode(Throwable e) {
        return RpcException.UNKNOWN_EXCEPTION;
    }
    /**@c impl：代理对象，type：接口类型 */
    protected abstract <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException;

    protected abstract <T> T doRefer(Class<T> type, URL url) throws RpcException;

}
