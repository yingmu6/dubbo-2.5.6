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
 * Filter. (SPI, Singleton, ThreadSafe)
 * spring过滤器以及dubbo过滤器，过滤器链了解
 *
 * @author william.liangf
 */
@SPI
public interface Filter {  // read finish

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
    Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException;

}