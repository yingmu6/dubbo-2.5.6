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
package com.alibaba.dubbo.rpc.proxy.jdk;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.proxy.AbstractProxyFactory;
import com.alibaba.dubbo.rpc.proxy.AbstractProxyInvoker;
import com.alibaba.dubbo.rpc.proxy.InvokerInvocationHandler;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * JavaassistRpcProxyFactory
 *
 * @author william.liangf
 */
public class JdkProxyFactory extends AbstractProxyFactory {

    /**
     * 通过java动态代理生成代理对象，并设置代理对象的调用处理类InvokerInvocationHandler
     * 通过代理对象访问目标对象时，会回调设置的调用处理方法
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaces, new InvokerInvocationHandler(invoker));
    }

    //jdk与javassist代理的差异处？解：jdk是java提供的软件开发包，而javassist是字节码增强技术：修改编译好的字节码，让新生成的字节码能满足的定制需求
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) {
        return new AbstractProxyInvoker<T>(proxy, type, url) {
            @Override
            protected Object doInvoke(T proxy, String methodName,
                                      Class<?>[] parameterTypes,
                                      Object[] arguments) throws Throwable {
                Method method = proxy.getClass().getMethod(methodName, parameterTypes);
                return method.invoke(proxy, arguments); //通过jdk的反射机制执行方法调用
            }
        };
    }
    /**
     * 11/11 静态代理、动态代理了解？Cglib和java代理比较
     * 代理：为其他对象提供一个代理以控制对某个对象的访问
     *
     * 静态代理：
     * 1）代理类和委托类实现了相同的接口，代理类通过委托类实现了相同的方法。这样就出现了大量的代码重复。
     * 动态代理：
     * 2）代理对象只服务于一种类型的对象，如果要服务多类型的对象。
     * 即静态代理类只能为特定的接口(Service)服务。如想要为多个接口服务则需要建立很多个代理类。
     *
     * 动态代理：
     * 动态代理是在程序运行时，通过反射获取被代理类的字节码内容用来创建代理类，通过反射机制实现动态代理，并且能够代理各种类型的对象
     *
     * 动态代理与静态代理的区别   https://www.jianshu.com/p/7da6ad6107ad
     * 一文说透"静态代理"与"动态代理"  https://xie.infoq.cn/article/8114fba36dae232639f1c6efa
     * Java三种代理模式：静态代理、动态代理和cglib代理  https://segmentfault.com/a/1190000011291179
     */

}