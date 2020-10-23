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

import com.alibaba.dubbo.common.URL;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * URL statistics. (API, Cached, ThreadSafe)
 * Rpc调用的统计信息（对服务调用、方法调用进行统计）
 *
 * @author william.liangf
 * @see com.alibaba.dubbo.rpc.filter.ActiveLimitFilter
 * @see com.alibaba.dubbo.rpc.filter.ExecuteLimitFilter
 */
public class RpcStatus {// read finish

    private static final ConcurrentMap<String, RpcStatus> SERVICE_STATISTICS = new ConcurrentHashMap<String, RpcStatus>(); /**@c 服务统计（内嵌对象，避免新增加类了）*/

    private static final ConcurrentMap<String, ConcurrentMap<String, RpcStatus>> METHOD_STATISTICS = new ConcurrentHashMap<String, ConcurrentMap<String, RpcStatus>>(); /**@c 方法统计 */
    private final ConcurrentMap<String, Object> values = new ConcurrentHashMap<String, Object>();
    private final AtomicInteger active = new AtomicInteger(); //激活数
    private final AtomicLong total = new AtomicLong(); //10/23 AtomicLong代码看下：解：atomic variables（原子变量），底层使用Unsafe的CAS机制保证原子性、线程安全e
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicLong totalElapsed = new AtomicLong(); //10/23 Elapsed相关的值的含义是啥？ 解：表示执行的时间
    private final AtomicLong failedElapsed = new AtomicLong();
    private final AtomicLong maxElapsed = new AtomicLong();
    private final AtomicLong failedMaxElapsed = new AtomicLong();
    private final AtomicLong succeededMaxElapsed = new AtomicLong();

    /**
     * 用来实现executes属性的并发限制（即控制能使用的线程数）
     * 2017-08-21 yizhenqiang
     */
    private volatile Semaphore executesLimit;
    private volatile int executesPermits;

    private RpcStatus() {
    }

    /**
     * 获取服务对应的Rpc状态信息
     * @param url
     * @return status
     */
    public static RpcStatus getStatus(URL url) {
        String uri = url.toIdentityString();
        RpcStatus status = SERVICE_STATISTICS.get(uri);
        if (status == null) {
            SERVICE_STATISTICS.putIfAbsent(uri, new RpcStatus()); //设置服务统计信息
            status = SERVICE_STATISTICS.get(uri);
        }
        return status;
    }

    /**
     * @param url
     */
    public static void removeStatus(URL url) {
        String uri = url.toIdentityString();
        SERVICE_STATISTICS.remove(uri);
    }

    /**
     * 获取方法对应的Rpc状态信息
     * @param url
     * @param methodName
     * @return status
     */
    public static RpcStatus getStatus(URL url, String methodName) { //10/22 待了解：已解
        String uri = url.toIdentityString();
        ConcurrentMap<String, RpcStatus> map = METHOD_STATISTICS.get(uri);
        if (map == null) {
            METHOD_STATISTICS.putIfAbsent(uri, new ConcurrentHashMap<String, RpcStatus>());
            map = METHOD_STATISTICS.get(uri);
        }
        RpcStatus status = map.get(methodName);
        if (status == null) {
            map.putIfAbsent(methodName, new RpcStatus());
            status = map.get(methodName);
        }
        return status;
    }

    /**
     * @param url
     */
    public static void removeStatus(URL url, String methodName) {
        String uri = url.toIdentityString();
        ConcurrentMap<String, RpcStatus> map = METHOD_STATISTICS.get(uri);
        if (map != null) {
            map.remove(methodName);
        }
    }

    /**
     * 开始统计
     * @param url
     */
    public static void beginCount(URL url, String methodName) {
        beginCount(getStatus(url)); //统计服务的调用次数递增1
        beginCount(getStatus(url, methodName)); //方法的调用次数递增1
    }

    private static void beginCount(RpcStatus status) {
        status.active.incrementAndGet(); //原子自动加1
    }

    /**
     * 结束统计
     * @param url
     * @param elapsed
     * @param succeeded
     */
    public static void endCount(URL url, String methodName, long elapsed, boolean succeeded) {
        endCount(getStatus(url), elapsed, succeeded);
        endCount(getStatus(url, methodName), elapsed, succeeded);
    }

    /**
     * 10/22 用途含义是啥？计算逻辑是啥
     * 解：对统计的相关值进行处理
     *
     * @param status  RpcStatus信息
     * @param elapsed 调用执行的时间
     * @param succeeded 是否成功
     */
    private static void endCount(RpcStatus status, long elapsed, boolean succeeded) {
        status.active.decrementAndGet();
        status.total.incrementAndGet();
        status.totalElapsed.addAndGet(elapsed); //计算到总时长
        if (status.maxElapsed.get() < elapsed) {
            status.maxElapsed.set(elapsed); //重新更新最大执行时间
        }
        if (succeeded) { //执行成功
            if (status.succeededMaxElapsed.get() < elapsed) {
                status.succeededMaxElapsed.set(elapsed); //重新更新最大成功的执行时间
            }
        } else {        //执行失败
            status.failed.incrementAndGet(); //记录失败次数
            status.failedElapsed.addAndGet(elapsed); //记录失败时间
            if (status.failedMaxElapsed.get() < elapsed) { //若失败时间超过了最大失败执行时间，则更新最大失败时间
                status.failedMaxElapsed.set(elapsed);
            }
        }
    }

    /**
     * set value.
     *
     * @param key
     * @param value
     */
    public void set(String key, Object value) {
        values.put(key, value);
    }

    /**
     * get value.
     *
     * @param key
     * @return value
     */
    public Object get(String key) {
        return values.get(key);
    }

    /**
     * get active.
     *
     * @return active
     */
    public int getActive() {
        return active.get();
    }

    /**
     * get total.
     *
     * @return total
     */
    public long getTotal() {
        return total.longValue();
    }

    /**
     * get total elapsed.
     *
     * @return total elapsed
     */
    public long getTotalElapsed() {
        return totalElapsed.get();
    }

    /**
     * get average elapsed.
     *
     * @return average elapsed
     */
    public long getAverageElapsed() {
        long total = getTotal();
        if (total == 0) {
            return 0;
        }
        return getTotalElapsed() / total;
    }

    /**
     * get max elapsed.
     *
     * @return max elapsed
     */
    public long getMaxElapsed() {
        return maxElapsed.get();
    }

    /**
     * get failed.
     *
     * @return failed
     */
    public int getFailed() {
        return failed.get();
    }

    /**
     * get failed elapsed.
     *
     * @return failed elapsed
     */
    public long getFailedElapsed() {
        return failedElapsed.get();
    }

    /**
     * get failed average elapsed.
     *
     * @return failed average elapsed
     */
    public long getFailedAverageElapsed() {
        long failed = getFailed();
        if (failed == 0) {
            return 0;
        }
        return getFailedElapsed() / failed;
    }

    /**
     * get failed max elapsed.
     *
     * @return failed max elapsed
     */
    public long getFailedMaxElapsed() {
        return failedMaxElapsed.get();
    }

    /**
     * get succeeded.
     *
     * @return succeeded
     */
    public long getSucceeded() {
        return getTotal() - getFailed();
    }

    /**
     * get succeeded elapsed.
     *
     * @return succeeded elapsed
     */
    public long getSucceededElapsed() {
        return getTotalElapsed() - getFailedElapsed();
    }

    /**
     * get succeeded average elapsed.
     *
     * @return succeeded average elapsed
     */
    public long getSucceededAverageElapsed() {
        long succeeded = getSucceeded();
        if (succeeded == 0) {
            return 0;
        }
        return getSucceededElapsed() / succeeded;
    }

    /**
     * get succeeded max elapsed.
     *
     * @return succeeded max elapsed.
     */
    public long getSucceededMaxElapsed() {
        return succeededMaxElapsed.get();
    }

    /**
     * Calculate average TPS (Transaction per second).
     *
     * @return tps
     */
    public long getAverageTps() {
        if (getTotalElapsed() >= 1000L) {
            return getTotal() / (getTotalElapsed() / 1000L);
        }
        return getTotal();
    }

    /**
     * 获取限制线程数的信号量，信号量的许可数就是executes设置的值
     * 2017-08-21 yizhenqiang
     * @param maxThreadNum executes设置的值
     * @return
     */
    public Semaphore getSemaphore(int maxThreadNum) {
        if(maxThreadNum <= 0) {
            return null;
        }

        if (executesLimit == null || executesPermits != maxThreadNum) {
            synchronized (this) {
                if (executesLimit == null || executesPermits != maxThreadNum) { //双重判断，确保线程安全
                    executesLimit = new Semaphore(maxThreadNum);
                    executesPermits = maxThreadNum;
                }
            }
        }

        return executesLimit;
    }
}