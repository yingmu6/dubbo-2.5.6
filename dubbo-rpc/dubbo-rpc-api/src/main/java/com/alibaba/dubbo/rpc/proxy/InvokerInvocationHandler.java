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
package com.alibaba.dubbo.rpc.proxy;

import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcInvocation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * InvokerHandler（Java的调用处理类）
 *
 * @author william.liangf
 */
public class InvokerInvocationHandler implements InvocationHandler {

    private final Invoker<?> invoker; //todo 11/24 具体实例对象是在哪里赋值的？

    public InvokerInvocationHandler(Invoker<?> handler) {
        this.invoker = handler;
    }

    /**
     * 重写java的InvocationHandler（调用处理类）
     * InvocationHandler：每一个代理实例都与一个调用处理类关联，
     *    当代理实例上的方法被调用时，会调用InvocationHandler的invoke方法（方法回调）
     * 1）
     *
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (method.getDeclaringClass() == Object.class) { // getDeclaringClass() 即为方法所在的类，如com.alibaba.dubbo.rpc.service.EchoService
            return method.invoke(invoker, args); //执行Object中的方法
        }
        if ("toString".equals(methodName) && parameterTypes.length == 0) {
            return invoker.toString();
        }
        if ("hashCode".equals(methodName) && parameterTypes.length == 0) {
            return invoker.hashCode();
        }
        if ("equals".equals(methodName) && parameterTypes.length == 1) {
            return invoker.equals(args[0]);
        }
        /**
         * 对创建结果判断，RpcResult.recreate() 若有异常抛出来，否则返回结果值
         */
        return invoker.invoke(new RpcInvocation(method, args)).recreate(); //todo 11/24 为啥一开始是MockClusterInvoker？
    }

}