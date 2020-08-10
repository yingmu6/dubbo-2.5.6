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

import java.util.Map;

/**
 * Invocation. (API, Prototype, NonThreadSafe)
 *
 * @author qian.lei
 * @author william.liangf
 * @serial Don't change the class name and package name.
 * @see com.alibaba.dubbo.rpc.Invoker#invoke(Invocation)
 * @see com.alibaba.dubbo.rpc.RpcInvocation
 */

//Invocation，一个调用方法的具体信息，包含方法名、参数类型、参数、调用者信息（会话信息）

/**
 * @csy 20/05/18 Invocation调用具体信息，类似gwweb
 * 包含：获取接口 getInterface()、获取Url信息 getUrl()、获取方法名称、
 * 获取方法参数类型以及参数值、获取隐式传递参数、获取调用者信息 invoker
 */
public interface Invocation { // invocation（调用方式）
    /**
     * get method name.
     *
     * @return method name.
     * @serial
     */
    String getMethodName(); //方法名称

    /**
     * get parameter types.
     *
     * @return parameter types.
     * @serial
     */
    Class<?>[] getParameterTypes(); //方法中参数类型

    /**
     * get arguments.
     *
     * @return arguments.
     * @serial
     */
    Object[] getArguments(); //方法中参数值

    /**
     * get attachments.以map形式存储参数
     *
     * @return attachments.
     * @serial
     */
    Map<String, String> getAttachments(); //隐式传递参数

    /**
     * get attachment by key. 获取隐式参数中对应key的值
     *
     * @return attachment value.
     * @serial
     */
    String getAttachment(String key);

    /**
     * get attachment by key with default value. 获取指定key对应的值，若没有获取到值，则返回默认值
     *
     * @return attachment value.
     * @serial
     */
    String getAttachment(String key, String defaultValue);

    /**
     * get the invoker in current context（当前环境）.
     *
     * @return invoker.
     * @transient
     */
    Invoker<?> getInvoker(); //获取执行者

}