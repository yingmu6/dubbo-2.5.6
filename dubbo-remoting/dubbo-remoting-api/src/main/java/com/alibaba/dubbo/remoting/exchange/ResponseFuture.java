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
package com.alibaba.dubbo.remoting.exchange;

import com.alibaba.dubbo.remoting.RemotingException;

/**
 * Future. (API/SPI, Prototype, ThreadSafe)
 *
 * @author qian.lei
 * @author william.liangf
 * @see com.alibaba.dubbo.remoting.exchange.ExchangeChannel#request(Object)
 * @see com.alibaba.dubbo.remoting.exchange.ExchangeChannel#request(Object, int)
 */
public interface ResponseFuture { //10/26 功能是啥？怎么使用的？异步响应结果

    /**
     * get result.（实现类中：阻塞地获取结果，有加锁）
     *
     * @return result.
     */
    Object get() throws RemotingException;

    /**
     * get result with the specified timeout.（带着指定的超时时间获取结果）
     *
     * @param timeoutInMillis timeout.
     * @return result.
     */
    Object get(int timeoutInMillis) throws RemotingException;

    /**
     * set callback.（设置响应回调）
     *
     * @param callback
     */
    void setCallback(ResponseCallback callback);

    /**
     * check is done.（检查是否是完成）
     *
     * @return done or not.
     */
    boolean isDone();

}