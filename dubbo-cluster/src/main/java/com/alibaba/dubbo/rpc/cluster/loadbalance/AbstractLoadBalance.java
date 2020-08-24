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
package com.alibaba.dubbo.rpc.cluster.loadbalance;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;

import java.util.List;

/**
 * AbstractLoadBalance
 *
 * @author william.liangf
 */
public abstract class AbstractLoadBalance implements LoadBalance {
    /**@c  用于计算预热权重 静态方法，类加载的时候执行 */
    static int calculateWarmupWeight(int uptime, int warmup, int weight) { // 参数值如，uptime=48499，warmup=600000，weight=100，48499/(600000/100) 取整后为8
        int ww = (int) ((float) uptime / ((float) warmup / (float) weight)); // 计算方式：距离服务启动的时间 / (预热时间 / 参数中设置的权重值)
        return ww < 1 ? 1 : (ww > weight ? weight : ww); //若权重小于1，则取1，否则判断是否大于weight，若超过weight值则取weight，否则去算出的值
        /**
         *  参数值如，uptime=48499，warmup=600000，weight=100，48499/(600000/100) 取整后为8
         *  分析：预热时间为600000毫秒，即为10分钟，若服务启动6秒内即6000毫秒时，消费者就来请求服务，此时ww < 1
         *  若启动大于6秒，则ww > 1，当启动时间小于10分钟时，ww < weight，即相除小于100
         *  若启动时间超过10分钟后，ww > weight的，取weight的值
         */
    }

    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        if (invokers == null || invokers.size() == 0)
            return null;
        if (invokers.size() == 1)
            return invokers.get(0);
        return doSelect(invokers, url, invocation);
    }

    /**@c 由不同的负载均衡策略，做选择 */
    protected abstract <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation);

    /**
     * 获取权重值（负载均衡是应用于消费端的）
     * 从url中方法参数获取权重值
     * 若权重值大于0，则重新计算权重，只有达到预热时间，才能进行预测计算
     */
    protected int getWeight(Invoker<?> invoker, Invocation invocation) { //若设置的权重值不为0，就会在预热期间重新计算权重
        int weight = invoker.getUrl().getMethodParameter(invocation.getMethodName(), Constants.WEIGHT_KEY, Constants.DEFAULT_WEIGHT); //若不设置权重，默认为100
        if (weight > 0) {
            /**@c 获取远程的时间戳，即提供者的时间戳 */
            long timestamp = invoker.getUrl().getParameter(Constants.REMOTE_TIMESTAMP_KEY, 0L); //Invoker提供者启动时间，1598241796190，1598237994564，1598237994564
            if (timestamp > 0L) {/**@c 预热时间，默认10分钟，只有达到预热时间，才能调用方法 */
                int uptime = (int) (System.currentTimeMillis() - timestamp); // 距离服务启动的时间，不是zk启动的时间
                int warmup = invoker.getUrl().getParameter(Constants.WARMUP_KEY, Constants.DEFAULT_WARMUP); //预热时间，默认10分钟
                if (uptime > 0 && uptime < warmup) { //在预热的时间段内，会计算权重，超过了就不计算了。
                    weight = calculateWarmupWeight(uptime, warmup, weight);
                }
            }
        }
        return weight;
    }

}