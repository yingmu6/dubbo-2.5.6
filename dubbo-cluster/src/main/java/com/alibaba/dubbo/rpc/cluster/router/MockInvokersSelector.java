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
package com.alibaba.dubbo.rpc.cluster.router;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Router;

import java.util.ArrayList;
import java.util.List;

/**
 * mock invoker选择器（对使用mock的调用invoker进行处理）
 *
 * @author chao.liuc
 */
public class MockInvokersSelector implements Router {

    /**
     * 对调用列表进行路由、过滤（返回正常列表或mock列表）
     *  判断invocation是否为空
     *  1）若为空，返回正常invoker列表
     *  2）若不为空，获取invocation.need.mock参数的值
     *   2.1）若为空，返回正常invoker列表
     *   2.2）若为true，返回Mock的invoker列表
     */
    public <T> List<Invoker<T>> route(final List<Invoker<T>> invokers,
                                      URL url, final Invocation invocation) throws RpcException {
        if (invocation.getAttachments() == null) {
            return getNormalInvokers(invokers);
        } else {
            String value = invocation.getAttachments().get(Constants.INVOCATION_NEED_MOCK); //根据附加的值是否带上invocation.need.mock，判断是否选择mock invoker
            if (value == null)
                return getNormalInvokers(invokers); //正常Invoker
            else if (Boolean.TRUE.toString().equalsIgnoreCase(value)) {
                return getMockedInvokers(invokers); //mock Invoker
            }
        }
        return invokers;
    }

    /**
     * 获取包含mock协议的invoker列表
     */
    private <T> List<Invoker<T>> getMockedInvokers(final List<Invoker<T>> invokers) {
        if (!hasMockProviders(invokers)) {
            return null;
        }
        List<Invoker<T>> sInvokers = new ArrayList<Invoker<T>>(1);
        for (Invoker<T> invoker : invokers) {
            if (invoker.getUrl().getProtocol().equals(Constants.MOCK_PROTOCOL)) {/**@c 比较是否满足条件 */
                sInvokers.add(invoker);
            }
        }
        return sInvokers;
    }

    /**
     * 获取正常调用的invoker列表
     *  判断是否包含mock的invoker
     *  1）若不包含则返回invoker列表
     *  2）若包含mock的invoker，则排除mock的invoker
     */
    private <T> List<Invoker<T>> getNormalInvokers(final List<Invoker<T>> invokers) {
        if (!hasMockProviders(invokers)) { //判断是否有mock 提供者
            return invokers;
        } else {
            List<Invoker<T>> sInvokers = new ArrayList<Invoker<T>>(invokers.size());
            for (Invoker<T> invoker : invokers) {
                if (!invoker.getUrl().getProtocol().equals(Constants.MOCK_PROTOCOL)) {
                    sInvokers.add(invoker);
                }
            }
            return sInvokers;
        }
    }

    /**
     * 判断调用列表是否包含mock提供者
     * 1）依次遍历invoker列表，判断invoker的协议有否包含mock协议
     */
    private <T> boolean hasMockProviders(final List<Invoker<T>> invokers) {
        boolean hasMockProvider = false;
        for (Invoker<T> invoker : invokers) {
            if (invoker.getUrl().getProtocol().equals(Constants.MOCK_PROTOCOL)) { //判断协议是否包含mock协议
                hasMockProvider = true;
                break;
            }
        }
        return hasMockProvider;
    }
    public URL getUrl() {
        return null;
    }

    /**
     * 此处没有实现具体的比较逻辑，a.compareTo(b)，默认a > b
     */
    public int compareTo(Router o) {
        return 1;
    }

}
