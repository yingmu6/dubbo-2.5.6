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
 * @csy-v1 代理对象的使用？ 代理模式使用
 * 此处代理对象可以和invoker相互转换
 *
 * ProxyFactory. (API/SPI, Singleton, ThreadSafe)
 * @author william.liangf
 */
@SPI("javassist")
public interface ProxyFactory {// read finish //todo 11/07 画类结构图

    /**
     * create proxy.(创建invoker的代理对象)
     *
     * @param invoker
     * @return proxy
     */
    @Adaptive({Constants.PROXY_KEY})
    <T> T getProxy(Invoker<T> invoker) throws RpcException;

    /**
     * create invoker.（通过代理对象创建invoker）
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
}