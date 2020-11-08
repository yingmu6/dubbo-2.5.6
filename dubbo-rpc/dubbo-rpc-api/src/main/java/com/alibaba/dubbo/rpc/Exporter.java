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

/**
 * Exporter. (API/SPI, Prototype, ThreadSafe)
 *
 * @author william.liangf
 * @see com.alibaba.dubbo.rpc.Protocol#export(Invoker)
 * @see com.alibaba.dubbo.rpc.ExporterListener
 * @see com.alibaba.dubbo.rpc.protocol.AbstractExporter
 */
// 指服务提供者吗？ 解：暴露服务的引用（负责维护invoker的生命周期，获取invoker、销毁invoker）
public interface Exporter<T> {// read finish

    /**
     * get invoker. 方法debug调试分析：已调
     * 获取服务的执行者
     * @return invoker
     */
    Invoker<T> getInvoker();

    /**
     * unexport.
     * <p>
     * <code>
     * getInvoker().destroy();
     * </code>
     */
    void unexport();

    /**
     * https://segmentfault.com/a/1190000015274825  关于ProxyFactory、Invoker、Protocol、Exporter概念
     *
     * Invoker ：一个可执行对象，能够根据方法名称、参数得到相应的执行结果
     * Exporter：负责维护invoker的生命周期，包含一个Invoker对象
     *
     * Invoker这个可执行对象的执行过程分成三种类型：
     * 本地执行的Invoker
     * 远程通信执行的Invoker
     * 多个Invoker聚合成的集群版Invoker
     *
     * Invocation则包含了需要执行的方法、参数等信息
     *
     * ProxyFactory 对于Server端，主要负责将服务统一进行包装成一个Invoker，通过反射来执行具体对象的方法
     */

}
