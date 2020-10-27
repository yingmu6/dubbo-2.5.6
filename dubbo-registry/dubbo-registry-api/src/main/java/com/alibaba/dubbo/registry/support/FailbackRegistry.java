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
package com.alibaba.dubbo.registry.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.registry.NotifyListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FailbackRegistry. (SPI, Prototype, ThreadSafe)
 *
 * @author william.liangf
 */
public abstract class FailbackRegistry extends AbstractRegistry {

    // 定时任务执行器
    private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("DubboRegistryFailedRetryTimer", true));

    // 失败重试定时器，定时检查是否有请求失败，如有，无限次重试
    private final ScheduledFuture<?> retryFuture;

    private final Set<URL> failedRegistered = new ConcurrentHashSet<URL>(); //注册失败的url列表

    private final Set<URL> failedUnregistered = new ConcurrentHashSet<URL>(); //取消注册失败的url列表

    private final ConcurrentMap<URL, Set<NotifyListener>> failedSubscribed = new ConcurrentHashMap<URL, Set<NotifyListener>>();

    private final ConcurrentMap<URL, Set<NotifyListener>> failedUnsubscribed = new ConcurrentHashMap<URL, Set<NotifyListener>>();

    private final ConcurrentMap<URL, Map<NotifyListener, List<URL>>> failedNotified = new ConcurrentHashMap<URL, Map<NotifyListener, List<URL>>>();

    private AtomicBoolean destroyed = new AtomicBoolean(false);

    public FailbackRegistry(URL url) { //默认每5秒进行失败检查尝试
        super(url);
        int retryPeriod = url.getParameter(Constants.REGISTRY_RETRY_PERIOD_KEY, Constants.DEFAULT_REGISTRY_RETRY_PERIOD);
        this.retryFuture = retryExecutor.scheduleWithFixedDelay(new Runnable() { //按固定的时间间隔执行任务
            public void run() { /**@c 失败后重试，怎样设置重试次数 ：无限次重试 */
                // 检测并连接注册中心
                try {
                    retry();
                } catch (Throwable t) { // 防御性容错
                    logger.error("Unexpected error occur at failed retry, cause: " + t.getMessage(), t);
                }
            }
        }, retryPeriod, retryPeriod, TimeUnit.MILLISECONDS);
    }

    public Future<?> getRetryFuture() {
        return retryFuture;
    }

    public Set<URL> getFailedRegistered() {
        return failedRegistered;
    }

    public Set<URL> getFailedUnregistered() {
        return failedUnregistered;
    }

    public Map<URL, Set<NotifyListener>> getFailedSubscribed() {
        return failedSubscribed;
    }

    public Map<URL, Set<NotifyListener>> getFailedUnsubscribed() {
        return failedUnsubscribed;
    }

    public Map<URL, Map<NotifyListener, List<URL>>> getFailedNotified() {
        return failedNotified;
    }

    /**
     * 将失败时的url以及监听者放到失败订阅的Map中
     * 这种设置方法与平时不太一样
     * 1）当前是：若对应的集合为空，先初始化一个集合设置到Map，然后再加对应加到初始化的集合中
     * 2）平时是：创建集合、为集合添加元素，最后再设置到Map中
     */
    private void addFailedSubscribed(URL url, NotifyListener listener) {
        Set<NotifyListener> listeners = failedSubscribed.get(url);
        if (listeners == null) {
            failedSubscribed.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
            listeners = failedSubscribed.get(url);
        }
        listeners.add(listener);
    }

    /**
     * 从url对应的失败监听者集合中将指定的lister移除
     * 1）从失败的failedSubscribed查询到url对应的失败集合listeners
     *    若集合不为空，则从集合中移除listener
     * 2）从失败的failedUnsubscribed查询到url对应的失败集合listeners
     *    若集合不为空，则从集合中移除listener
     * 3）从失败的failedNotified查询到url对应的失败集合notified
     *    若集合不为空，则从集合中移除listener
     */
    private void removeFailedSubscribed(URL url, NotifyListener listener) {/**@c  */
        Set<NotifyListener> listeners = failedSubscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
        listeners = failedUnsubscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
        Map<NotifyListener, List<URL>> notified = failedNotified.get(url);
        if (notified != null) {
            notified.remove(listener);
        }
    }

    @Override
    public void register(URL url) {
        if (destroyed.get()){
            return;
        }
        super.register(url); //调用父类的方法，将注册的url存储下来
        failedRegistered.remove(url);
        failedUnregistered.remove(url);
        try {
            // 向注册服务器端发送注册请求
            doRegister(url);/**@c 由子类实现注册 */
        } catch (Exception e) {
            Throwable t = e;

            // 如果开启了启动时检测，则直接抛出异常
            boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                    && url.getParameter(Constants.CHECK_KEY, true)
                    && !Constants.CONSUMER_PROTOCOL.equals(url.getProtocol()); //设置了check=true，并且不是消费方，抛出异常
            boolean skipFailback = t instanceof SkipFailbackWrapperException;
            if (check || skipFailback) {/**@c 注册失败 抛出异常*/
                if (skipFailback) {
                    t = t.getCause();
                }
                throw new IllegalStateException("Failed to register " + url + " to registry " + getUrl().getAddress() + ", cause: " + t.getMessage(), t);
            } else { // 不检查是否注册成功
                logger.error("Failed to register " + url + ", waiting for retry, cause: " + t.getMessage(), t);
            }

            // 将失败的注册请求记录到失败列表，定时重试
            failedRegistered.add(url);
        }
    }

    @Override
    public void unregister(URL url) {
        if (destroyed.get()){
            return;
        }
        super.unregister(url);
        failedRegistered.remove(url);
        failedUnregistered.remove(url);
        try {
            // 向服务器端发送取消注册请求
            doUnregister(url);
        } catch (Exception e) {
            Throwable t = e;

            // 如果开启了启动时检测，则直接抛出异常
            boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                    && url.getParameter(Constants.CHECK_KEY, true)
                    && !Constants.CONSUMER_PROTOCOL.equals(url.getProtocol());
            boolean skipFailback = t instanceof SkipFailbackWrapperException;
            if (check || skipFailback) {
                if (skipFailback) {
                    t = t.getCause();
                }
                throw new IllegalStateException("Failed to unregister " + url + " to registry " + getUrl().getAddress() + ", cause: " + t.getMessage(), t);
            } else {
                logger.error("Failed to uregister " + url + ", waiting for retry, cause: " + t.getMessage(), t);
            }

            // 将失败的取消注册请求记录到失败列表，定时重试
            failedUnregistered.add(url);
        }
    }

    /**
     * 订阅符合条件的已注册数据（当有注册数据变更时自动推送）
     * 1）若目录以销毁，则不处理
     * 2）调用父类AbstractRegistry的subscribe订阅方法，将listener监听者添加到
     *    url对应的监听者map缓存中，ConcurrentMap<URL, Set<NotifyListener>> （抽象类：将公共的方法以及属性抽离出来）
     * 3）从失败订阅Map failedSubscribed、失败取消订阅Map failedUnsubscribed、失败通知Map failedNotified中移除NotifyListener
     * 4）向服务器端发送订阅请求
     *   4.1）若订阅失败，做相关处理
     *    4.1.1）从缓存Properties获取到url中serviceKey对应的服务列表
     *    4.1.2）若缓存的服务url列表不为空，则尝试通知本地缓存中对应的服务，并打印错误日志
     *    4.1.3）若缓存中的服务url列表为空，获取当前url以及传入url中的"check"值
     *           以及判断是否是SkipFailbackWrapperException的实例
     *           4.1.3.1）若开启了检测或异常是SkipFailbackWrapperException的实例，
     *             则抛出IllegalStateException异常，停止程序执行
     *           4.1.3.2）没有开启检测check，且异常不是SkipFailbackWrapperException的实例，则只打印出异常日志
     *    4.1.4）
     */
    @Override
    public void subscribe(URL url, NotifyListener listener) {
        if (destroyed.get()){
            return;
        }
        //url = URL.valueOf("zookeeper://127.0.0.1:2181/com.alibaba.dubbo.registry.RegistryService?application=demo-provider&callbacks=10000&check=true&connect.timeout=10000&dubbo=2.0.0&interface=com.alibaba.dubbo.registry.RegistryService&lazy=true&methods=lookup,subscribe,unsubscribe,unregister,register&pid=21003&reconnect=false&sticky=true&subscribe.1.callback=true&timeout=10000&timestamp=1565075252678&unsubscribe.1.callback=false");
        super.subscribe(url, listener);
        removeFailedSubscribed(url, listener);
        try {
            // 向服务器端发送订阅请求
            doSubscribe(url, listener);
        } catch (Exception e) {
            Throwable t = e;

            List<URL> urls = getCacheUrls(url);
            if (urls != null && urls.size() > 0) {
                notify(url, listener, urls);
                logger.error("Failed to subscribe " + url + ", Using cached list: " + urls + " from cache file: " + getUrl().getParameter(Constants.FILE_KEY, System.getProperty("user.home") + "/dubbo-registry-" + url.getHost() + ".cache") + ", cause: " + t.getMessage(), t);
            } else {
                // 如果开启了启动时检测，则直接抛出异常
                boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                        && url.getParameter(Constants.CHECK_KEY, true);
                boolean skipFailback = t instanceof SkipFailbackWrapperException;
                if (check || skipFailback) {
                    if (skipFailback) {
                        t = t.getCause();
                    }
                    throw new IllegalStateException("Failed to subscribe " + url + ", cause: " + t.getMessage(), t);
                } else {/**@c 只是打错误日志，没有抛出异常，终止运行 */
                    logger.error("Failed to subscribe " + url + ", waiting for retry, cause: " + t.getMessage(), t);
                }
            }

            // 将失败的订阅请求记录到失败列表，定时重试
            addFailedSubscribed(url, listener);
        }
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        if (destroyed.get()){
            return;
        }
        super.unsubscribe(url, listener);
        removeFailedSubscribed(url, listener);
        try {
            // 向服务器端发送取消订阅请求
            doUnsubscribe(url, listener);
        } catch (Exception e) {
            Throwable t = e;

            // 如果开启了启动时检测，则直接抛出异常
            boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                    && url.getParameter(Constants.CHECK_KEY, true);
            boolean skipFailback = t instanceof SkipFailbackWrapperException;
            if (check || skipFailback) {
                if (skipFailback) {
                    t = t.getCause();
                }
                throw new IllegalStateException("Failed to unsubscribe " + url + " to registry " + getUrl().getAddress() + ", cause: " + t.getMessage(), t);
            } else {
                logger.error("Failed to unsubscribe " + url + ", waiting for retry, cause: " + t.getMessage(), t);
            }

            // 将失败的取消订阅请求记录到失败列表，定时重试
            Set<NotifyListener> listeners = failedUnsubscribed.get(url);
            if (listeners == null) {
                failedUnsubscribed.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
                listeners = failedUnsubscribed.get(url);
            }
            listeners.add(listener);
        }
    }

    /**
     * 通知
     * 1）判断url、listener是否为空，若为空则抛出非法参数异常
     * 2）处理通知
     *  2.1）若异常，将url加到失败集合中，定时重试
     */
    @Override
    protected void notify(URL url, NotifyListener listener, List<URL> urls) {
        if (url == null) {
            throw new IllegalArgumentException("notify url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }
        try {
            doNotify(url, listener, urls);
        } catch (Exception t) {
            // 将失败的通知请求记录到失败列表，定时重试
            Map<NotifyListener, List<URL>> listeners = failedNotified.get(url);
            if (listeners == null) {
                failedNotified.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, List<URL>>());
                listeners = failedNotified.get(url);
            }
            listeners.put(listener, urls);
            logger.error("Failed to notify for subscribe " + url + ", waiting for retry, cause: " + t.getMessage(), t);
        }
    }

    /**
     * 处理通知
     */
    protected void doNotify(URL url, NotifyListener listener, List<URL> urls) {
        super.notify(url, listener, urls);
    }

    @Override
    protected void recover() throws Exception {/**@c recover重新注册或订阅，把url放到失败的列表中，就会定时轮询尝试重新恢复了 */
        // register
        Set<URL> recoverRegistered = new HashSet<URL>(getRegistered());
        if (!recoverRegistered.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover register url " + recoverRegistered);
            }
            for (URL url : recoverRegistered) {
                failedRegistered.add(url);
            }
        }
        // subscribe
        Map<URL, Set<NotifyListener>> recoverSubscribed = new HashMap<URL, Set<NotifyListener>>(getSubscribed());
        if (!recoverSubscribed.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover subscribe url " + recoverSubscribed.keySet());
            }
            for (Map.Entry<URL, Set<NotifyListener>> entry : recoverSubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    addFailedSubscribed(url, listener);
                }
            }
        }
    }

    /**
     * 重试失败的动作 (每隔指定时间，比如5秒，执行失败检查)，若重试成功，从错误列表中移除
     * 总之，哪个失败集合中存在失败的url，就对应重试相应的功能，若成功，则把失败记录移除
     */

    /**
     * 注册中心重试 -- 代码流程 --
     * 1）若创建节点失败集合failedRegistered不为空
     *    遍历集合中的URL，尝试通过doRegister(URL url)创建节点，若创建节点没异常，则将url从失败集合中移除，若异常则抛出提醒warn日志
     * 2）若删除节点失败集合failedUnregistered不为空
     *    遍历集合中的URL，尝试通过doUnregister(URL url)删除节点，若删除节点没异常，则将url从失败集合中移除，若异常则抛出提醒warn日志
     * 3）若订阅节点失败集合failedSubscribed不为空
     *    3.1）构建新的集合，对集合进行筛选，对值为空的选项进行移除
     *    3.2）在新的错误集合存在选项时 failed.size() > 0 进行处理
     *     3.2.1）双重循环处理
     *       3.2.1.1）遍历错误Map<URL, Set<NotifyListener>>集合，获取每个url对应的值，Set<NotifyListener>
     *          3.2.1.1.1）遍历Set<NotifyListener> 通知监听者，尝试通过doSubscribe(url, listener)做订阅
     *                     若订阅成功没异常，则将监听者从失败集合中移除,否则抛出异常日志
     * 4）若取消订阅节点失败集合failedUnsubscribed不为空
     *   4.1）构建新的集合，对集合进行筛选，对值为空的选项进行移除
     *   4.2）在新的错误集合存在选项时 failed.size() > 0 进行处理
     *     4.2.1）双重循环处理
     *       4.2.1.1）遍历错误Map<URL, Set<NotifyListener>>集合，获取每个url对应的值，Set<NotifyListener>
     *           4.2.1.1.1）遍历Set<NotifyListener> 通知监听者，尝试通过doUnsubscribe(url, listener)做取消订阅
     *                     若取消订阅成功没异常，则将监听者从失败集合中移除,否则抛出异常日志
     *
     * 5）若失败节点失败集合failedNotified不为空
     *     5.1）构建新的集合，对集合进行筛选，对值为空的选项进行移除
     *     5.2）在新的错误集合存在选项时 failed.size() > 0 进行处理
     *       5.2.1）双重循环处理
     *         5.2.1.1）遍历错误Map<URL, Set<NotifyListener>>集合，获取每个url对应的值，Set<NotifyListener>
     *           5.2.1.1.1）遍历Set<NotifyListener> 通知监听者，尝试通过notify(urls) 做通知处理
     *                     若通知成功没异常，则将监听者从失败集合中移除,否则抛出异常日志
     *
     */
    protected void retry() {/**@c 重试失败的集合 注册、取消注册、订阅、取消订阅*/
        if (!failedRegistered.isEmpty()) { //注册失败，就尝试重新注册，若成功，将url从失败列表中移除
            Set<URL> failed = new HashSet<URL>(failedRegistered);
            if (failed.size() > 0) { // history 此处是否多余判断了，failedRegistered.isEmpty()是否会判断size？
                if (logger.isInfoEnabled()) {
                    logger.info("Retry register " + failed);
                }
                try {
                    for (URL url : failed) {
                        try {
                            doRegister(url);/**@c 重试成功后，从失败集合中去除URL */
                            failedRegistered.remove(url);
                        } catch (Throwable t) { // 忽略所有异常，等待下次重试
                            logger.warn("Failed to retry register " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry register " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        if (!failedUnregistered.isEmpty()) {
            Set<URL> failed = new HashSet<URL>(failedUnregistered);
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry unregister " + failed);
                }
                try {
                    for (URL url : failed) {
                        try {
                            doUnregister(url);
                            failedUnregistered.remove(url);
                        } catch (Throwable t) { // 忽略所有异常，等待下次重试
                            logger.warn("Failed to retry unregister  " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry unregister  " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        if (!failedSubscribed.isEmpty()) { //是否有订阅者
            Map<URL, Set<NotifyListener>> failed = new HashMap<URL, Set<NotifyListener>>(failedSubscribed);
            for (Map.Entry<URL, Set<NotifyListener>> entry : new HashMap<URL, Set<NotifyListener>>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry subscribe " + failed);
                }
                try {
                    for (Map.Entry<URL, Set<NotifyListener>> entry : failed.entrySet()) {
                        URL url = entry.getKey();
                        Set<NotifyListener> listeners = entry.getValue();
                        for (NotifyListener listener : listeners) {
                            try {
                                doSubscribe(url, listener);/**@c 重试成功后，移除监听器 */
                                listeners.remove(listener);
                            } catch (Throwable t) { // 忽略所有异常，等待下次重试
                                logger.warn("Failed to retry subscribe " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                            }
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry subscribe " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        if (!failedUnsubscribed.isEmpty()) {
            Map<URL, Set<NotifyListener>> failed = new HashMap<URL, Set<NotifyListener>>(failedUnsubscribed);
            for (Map.Entry<URL, Set<NotifyListener>> entry : new HashMap<URL, Set<NotifyListener>>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry unsubscribe " + failed);
                }
                try {
                    for (Map.Entry<URL, Set<NotifyListener>> entry : failed.entrySet()) {
                        URL url = entry.getKey();
                        Set<NotifyListener> listeners = entry.getValue();
                        for (NotifyListener listener : listeners) {
                            try {
                                doUnsubscribe(url, listener);
                                listeners.remove(listener);
                            } catch (Throwable t) { // 忽略所有异常，等待下次重试
                                logger.warn("Failed to retry unsubscribe " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                            }
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry unsubscribe " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        if (!failedNotified.isEmpty()) {
            Map<URL, Map<NotifyListener, List<URL>>> failed = new HashMap<URL, Map<NotifyListener, List<URL>>>(failedNotified);
            for (Map.Entry<URL, Map<NotifyListener, List<URL>>> entry : new HashMap<URL, Map<NotifyListener, List<URL>>>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry notify " + failed);
                }
                try {
                    for (Map<NotifyListener, List<URL>> values : failed.values()) {
                        for (Map.Entry<NotifyListener, List<URL>> entry : values.entrySet()) {
                            try {
                                NotifyListener listener = entry.getKey();
                                List<URL> urls = entry.getValue();
                                listener.notify(urls);
                                values.remove(listener);
                            } catch (Throwable t) { // 忽略所有异常，等待下次重试
                                logger.warn("Failed to retry notify " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                            }
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry notify " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
    }

    @Override
    public void destroy() {
        if (!canDestroy()){
            return;
        }
        super.destroy();
        try {
            retryFuture.cancel(true);
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    // history-h3: 2017/8/30 to abstract this method
    protected boolean canDestroy(){
        if (destroyed.compareAndSet(false, true)) {
            return true;
        }else{
            return false;
        }
    }


    // ==== 模板方法 ====

    protected abstract void doRegister(URL url);

    protected abstract void doUnregister(URL url);

    protected abstract void doSubscribe(URL url, NotifyListener listener);

    protected abstract void doUnsubscribe(URL url, NotifyListener listener);

}