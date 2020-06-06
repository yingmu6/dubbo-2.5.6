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
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryFactory;
import com.alibaba.dubbo.registry.RegistryService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AbstractRegistryFactory. (SPI, Singleton, ThreadSafe)
 *
 * @author william.liangf
 * @see com.alibaba.dubbo.registry.RegistryFactory
 */
public abstract class AbstractRegistryFactory implements RegistryFactory {

    // 日志输出
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRegistryFactory.class);

    // 注册中心获取过程锁
    private static final ReentrantLock LOCK = new ReentrantLock();

    // 注册中心集合 Map<RegistryAddress, Registry> 缓存实例
    private static final Map<String, Registry> REGISTRIES = new ConcurrentHashMap<String, Registry>();

    /**
     * 获取所有注册中心
     *
     * @return 所有注册中心
     */
    public static Collection<Registry> getRegistries() {
        return Collections.unmodifiableCollection(REGISTRIES.values());
    }

    /**
     * 关闭所有已创建注册中心
     */
    // todo @system: 2017/8/30 to move somewhere else better
    public static void destroyAll() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Close all registries " + getRegistries());
        }
        // 锁定注册中心关闭过程
        LOCK.lock();
        try {
            for (Registry registry : getRegistries()) {
                try {
                    registry.destroy(); //销毁节点Node
                } catch (Throwable e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
            REGISTRIES.clear(); //移除map中的所有映射
        } finally {
            // 释放锁
            LOCK.unlock();
        }
    }

    /**
     * 获取URL对应的注册实例
     * 1）url参数处理
     *   1.1）设置url的path值 com.alibaba.dubbo.registry.RegistryService，原始url 如zookeeper://host:port/com.alibaba.dubbo.registry.RegistryService....
     *   1.2）设置interface值 com.alibaba.dubbo.registry.RegistryService
     *   1.3）移除export的值, 此处处理后的url，zookeeper://localhost:2181/com.alibaba.dubbo.registry.RegistryService?application=api_demo&
     *       dubbo=2.0.0&interface=com.alibaba.dubbo.registry.RegistryService&pid=57000&timestamp=1591274736172
     * 2）以服务字符串以构建map中的键key
     * 3）从注册中心获取注册实例时加锁
     *   3.1）从注册中心实例缓存map中key对应的值，若存在直接返回（确保单一实例）
     *   3.2）若没有key对应的实例，则根据url创建注册实例，并且存入本地缓存
     * 4）释放锁
     */
    public Registry getRegistry(URL url) {
        url = url.setPath(RegistryService.class.getName())
                .addParameter(Constants.INTERFACE_KEY, RegistryService.class.getName())
                .removeParameters(Constants.EXPORT_KEY, Constants.REFER_KEY);
        String key = url.toServiceString();
        // 锁定注册中心获取过程，保证注册中心单一实例
        LOCK.lock();
        try {
            Registry registry = REGISTRIES.get(key);/**@c 从本地缓存获取，若没有则创建 */
            if (registry != null) {
                return registry;
            }
            registry = createRegistry(url); //获取注册实例（创建对象，如ZookeeperRegistry，并做初始化）
            if (registry == null) {
                throw new IllegalStateException("Can not create registry " + url);
            }
            REGISTRIES.put(key, registry); //注册url与注册实例映射起来,key 如：zookeeper://127.0.0.1:2181/com.alibaba.dubbo.registry.RegistryService
            return registry;
        } finally {
            // 释放锁
            LOCK.unlock();
        }
    }

    /**
     * 创建注册实例是抽象方法，交由子类去实现逻辑
     * 此处的子类有DubboRegistryFactory、ZookeeperRegistryFactory等，通过工厂模式创建对象
     * 一般情况下，用zookeeper作为注册中心
     */
    protected abstract Registry createRegistry(URL url);

}