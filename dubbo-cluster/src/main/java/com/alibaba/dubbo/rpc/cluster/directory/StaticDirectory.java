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
package com.alibaba.dubbo.rpc.cluster.directory;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Router;

import java.util.List;

/**
 * StaticDirectory（静态目录，包含的invoker列表不会改变）
 *
 * @author william.liangf
 */
public class StaticDirectory<T> extends AbstractDirectory<T> {

    private final List<Invoker<T>> invokers; /**@c 用final修饰，表明静态服务目录的invoker是不会改变的 */

    public StaticDirectory(List<Invoker<T>> invokers) {
        this(null, invokers, null);
    }

    public StaticDirectory(List<Invoker<T>> invokers, List<Router> routers) {
        this(null, invokers, routers);
    }

    public StaticDirectory(URL url, List<Invoker<T>> invokers) {
        this(url, invokers, null);
    }

    public StaticDirectory(URL url, List<Invoker<T>> invokers, List<Router> routers) {
        // 若url为空，且调用列表invokers不为空的话，取列表中第一个执行体invoker的url，否则直接取url
        super(url == null && invokers != null && invokers.size() > 0 ? invokers.get(0).getUrl() : url, routers);
        if (invokers == null || invokers.size() == 0)
            throw new IllegalArgumentException("invokers == null");
        this.invokers = invokers;
    }

    public Class<T> getInterface() {
        return invokers.get(0).getInterface(); //为啥直接取第一个？ 列表中的invoker类型是一样的，所以取其中一个即可
    }

    /**
     * 判断静态目录是否可用
     * 1）判断目录的标识destroyed是否被销毁
     * 2）判断目录维护的invoker列表中是否有可用的invoker
     */
    public boolean isAvailable() {
        if (isDestroyed()) {
            return false;
        }
        for (Invoker<T> invoker : invokers) {
            if (invoker.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 销毁静态目录
     * 1）判断是否有销毁，若已销毁，则不处理
     * 2）将父类AbstractDirectory的标识destroyed更新为销毁状态
     * 3）清空静态目录维护的列表invokers.clear()
     */
    public void destroy() {
        if (isDestroyed()) {
            return;
        }
        super.destroy();
        for (Invoker<T> invoker : invokers) {
            invoker.destroy();
        }
        invokers.clear();
    }

    /**
     * 获取调用信息对应的invoker列表
     * 1）直接返回静态目录维护的invoker列表
     */
    @Override
    protected List<Invoker<T>> doList(Invocation invocation) throws RpcException {

        return invokers;
    }

}