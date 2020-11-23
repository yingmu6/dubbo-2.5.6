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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

/**
 * 此处代理对象可以和invoker相互转换
 *
 * ProxyFactory. (API/SPI, Singleton, ThreadSafe)
 * @author william.liangf
 */
@SPI("javassist")
public interface ProxyFactory {

    /**
     * create proxy.(获取invoker的对应的代理对象)
     *
     * @param invoker
     * @return proxy
     */
    @Adaptive({Constants.PROXY_KEY})
    <T> T getProxy(Invoker<T> invoker) throws RpcException;

    /**
     * create invoker.（获取代理对象对应的invoker）
     *
     * @param <T>
     * @param proxy  代理接口
     * @param type   代理接口类型
     * @param url
     * @return invoker
     */
    @Adaptive({Constants.PROXY_KEY})
    <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException;

    /**
     * 10/25 自适应加载原理了解下，10/30-done
     * 自适应原理：不需要指定扩展名，可根据url中的配置动态创建扩展类
     * 1）会从url找Constants.PROXY_KEY对应的值
     * 2）若没查找会将类名处理查找，如proxy.factory
     * 3）若还没查到，则取SPI上声明的扩展值
     * 若@Adaptive声明有多个value，则会从右到左，依次取值作为下一个的默认值
     */

    /**
     * 泛型：泛化的类型
     * 强类型程序设计语言中编写代码时定义一些可变部分，那些部分在使用前必须作出指明。
     * 泛型:增加了极大的效力和灵活性，不会强行对值类型进行装箱和拆箱，或对引用类型进行向下强制类型转换，
     * 所以性能得到提高.泛型类和泛型方法同时具备可重用性、类型安全和效率，这是非泛型类和非泛型方法无法具备的。
     * --
     * java 泛型详解 https://blog.csdn.net/s10461/article/details/53941091
     */
}