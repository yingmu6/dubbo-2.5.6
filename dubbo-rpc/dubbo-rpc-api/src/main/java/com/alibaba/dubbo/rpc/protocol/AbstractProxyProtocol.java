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
public abstract class AbstractProxyProtocol extends AbstractProtocol {// read finish  todo 11/07 抽象代理协议是何时被使用的？

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
     * 暴露服务，获取invoker对应的管理者Exporter
     * 1）从本地缓存中查找Exporter对象，若查到则直接返回，若没查到则创建
     * 2）通过匿名类方式创建AbstractExporter对象，重写unexport方法，将父类、当前对象、子类对象的缓存都清除
     * 3）返回创建的Exporter对象
     */
    @SuppressWarnings("unchecked")
    public <T> Exporter<T> export(final Invoker<T> invoker) throws RpcException {
        final String uri = serviceKey(invoker.getUrl());
        Exporter<T> exporter = (Exporter<T>) exporterMap.get(uri);
        if (exporter != null) {
            return exporter;
        }
        final Runnable runnable = doExport(proxyFactory.getProxy(invoker), invoker.getInterface(), invoker.getUrl());
        exporter = new AbstractExporter<T>(invoker) { //匿名类构建AbstractExporter，用来管理invoker生命周期
            /**
             * 11/07 这个线程都做了啥？11/07-done
             * 解：这里是取消暴露的操作
             * 1）销毁父类中invoker节点
             * 2）移除当前对象维护的对应key的缓存
             * 3）移除子类对象维护的对应key的缓存
             *  （子类对象，把移除操作封装为线程，当需要移除时调用线程即可，就不需要方法回调了）
             */
            public void unexport() { //重写取消暴露方法
                super.unexport();
                exporterMap.remove(uri);
                if (runnable != null) {
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
