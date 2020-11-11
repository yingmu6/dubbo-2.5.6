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
package com.alibaba.dubbo.rpc.proxy.javassist;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.bytecode.Proxy;
import com.alibaba.dubbo.common.bytecode.Wrapper;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.proxy.AbstractProxyFactory;
import com.alibaba.dubbo.rpc.proxy.AbstractProxyInvoker;
import com.alibaba.dubbo.rpc.proxy.InvokerInvocationHandler;

/**
 * JavaassistRpcProxyFactory
 *
 * @author william.liangf
 */
public class JavassistProxyFactory extends AbstractProxyFactory {// read finish

    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        /**
         * 代码流程：
         * 1) 静态方法Proxy.getProxy，获取到Proxy
         * 2）构建InvokerInvocationHandler处理类
         *    2.1）构建RpcInvocation --- new RpcInvocation(method, args)
         *    2.2）执行Invoker的invoke方法 --- invoker.invoke(new RpcInvocation(method, args))
         *    2.3）执行Result的recreate
         * 3）调用Proxy中newInstance产生Object
         */
        return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
    }

    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) {// todo 11/11-doing
        //@system Wrapper类不能正确处理带$的类名
        final Wrapper wrapper = Wrapper.getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type); //若类名中包含$，则取传入的type封装，否则取proxy的class封装
        return new AbstractProxyInvoker<T>(proxy, type, url) {
            @Override
            protected Object doInvoke(T proxy, String methodName,
                                      Class<?>[] parameterTypes,
                                      Object[] arguments) throws Throwable {
                //通过封装类Wrapper执行方法调用
                return wrapper.invokeMethod(proxy, methodName, parameterTypes, arguments); //todo 11/11 此处调用待了解，外部内访问内部类？
            }
        };
    }

}