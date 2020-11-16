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
package com.alibaba.dubbo.remoting;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;
import com.alibaba.dubbo.remoting.transport.dispatcher.all.AllDispatcher;

/**
 * ChannelHandlerWrapper (SPI, Singleton, ThreadSafe)
 *
 * @author chao.liuc
 */
@SPI(AllDispatcher.NAME) //11/16 线程模式了解下
public interface Dispatcher { //派发器：将消息派发到I/O线程或线程池中

    /**
     * dispatch the message to threadpool.（拦截到线程池的消息）
     *
     * @param handler
     * @param url
     * @return channel handler
     */
    @Adaptive({Constants.DISPATCHER_KEY, "dispather", "channel.handler"}) //自适应条件
    // 后两个参数为兼容旧配置
    ChannelHandler dispatch(ChannelHandler handler, URL url);

    /**
     * 线程模型： http://dubbo.apache.org/zh-cn/docs/2.7/user/demos/thread-model/
     * 如果事件处理的逻辑能迅速完成，并且不会发起新的 IO 请求，比如只是在内存中记个标识，则直接在 IO 线程上处理更快，因为减少了线程池调度。
     * 但如果事件处理逻辑较慢，或者需要发起新的 IO 请求，比如需要查询数据库，则必须派发到线程池，否则 IO 线程阻塞，将导致不能接收其它请求。
     *
     * all 所有消息都派发到线程池，包括请求，响应，连接事件，断开事件，心跳等。
     * direct 所有消息都不派发到线程池，全部在 IO 线程上直接执行。
     * message 只有请求响应消息派发到线程池，其它连接断开事件，心跳等消息，直接在 IO 线程上执行。
     * execution 只有请求消息派发到线程池，不含响应，响应和其它连接断开事件，心跳等消息，直接在 IO 线程上执行。
     * connection 在 IO 线程上，将连接断开事件放入队列，有序逐个执行，其它消息派发到线程池。
     *
     * Dubbo线程模型与线程池策略（图解） https://cloud.tencent.com/developer/article/1557131
     */

}