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
package com.alibaba.dubbo.rpc;

import com.alibaba.dubbo.common.extension.SPI;

/**
 * Filter. (SPI, Singleton, ThreadSafe) // 10/22 Singleton, ThreadSafe是啥意思: 解：给出约束信息，SPI的实现者都需要满足约束，分别表示单例和线程安全
 * spring过滤器以及dubbo过滤器，过滤器链了解
 *
 * @author william.liangf
 */
@SPI
public interface Filter { //test

    /**
     * do invoke filter.
     * <p>
     * <code>
     * // before filter
     * Result result = invoker.invoke(invocation);
     * // after filter
     * return result;
     * </code>
     *
     * @param invoker    service
     * @param invocation invocation.
     * @return invoke result.
     * @throws RpcException
     * @see com.alibaba.dubbo.rpc.Invoker#invoke(Invocation)
     */

    /**
     * 在执行invoke之前、之后过滤（传入调用者、调用的上下文信息）
     * 入参包括调用者invoker、调用上下文invocation
     * 通过调用invoker的invoke(invocation) 方法可以在调用前后进行处理
     */
    Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException; // 10/22 框架是怎么组装并传入invoker, invocation值的

    /**
     * 单例模式 https://www.runoob.com/design-pattern/singleton-pattern.html
     * 1、单例类只能有一个实例。
     * 2、单例类必须自己创建自己的唯一实例。
     * 3、单例类必须给所有其他对象提供这一实例。
     *
     * Spring过滤器和拦截器执行流程  https://www.jianshu.com/p/394480ae9b7c
     *
     * 过滤器与拦截器的区别 https://www.jianshu.com/p/3e6433ead5c3
     * Filter 是基于 函数回调的，而 Interceptor（拦截器） 则是基于 Java反射 和 动态代理。
     * Filter 依赖于 Servlet 容器，而 Interceptor 不依赖于 Servlet 容器。
     * Filter 对几乎 所有的请求 起作用，而 Interceptor 只对 Controller 对请求起作用。
     */

    /**
     *  10/24 过滤链是怎么构建起来的，哪些是系统的过滤链？ 11/04
     * 1）消费者、提供者都会经过过滤链
     */
}

