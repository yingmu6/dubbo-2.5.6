/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.cache.support;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.cache.CacheFactory;
import com.alibaba.dubbo.common.URL;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * AbstractCacheFactory
 *
 * @author william.liangf
 */
public abstract class AbstractCacheFactory implements CacheFactory {

    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>();

    public Cache getCache(URL url) {
        String key = url.toFullString();
        Cache cache = caches.get(key);
        if (cache == null) {
            caches.put(key, createCache(url));
            cache = caches.get(key);
        }
        return cache;
    }

    protected abstract Cache createCache(URL url);

    /**
     * 缓存方式有哪些？用怎样的数据结构处理缓存的？
     * https://www.jianshu.com/p/866e8455e769  本地缓存（Java实现之理论篇）
     * https://segmentfault.com/a/1190000022700200  本地缓存高性能之王Caffeine
     *
     * 所谓缓存，就是将程序或系统经常要调用的对象存在内存中，方便其使用时可以快速调用，不必再去创建新的重复的实例。这样做可以减少系统开销，提高系统效率。
     * 缓存包含：本地缓存、远程缓存（单机版远程缓存、分布式远程缓存）
     * 所谓的本地缓存是相对于网络而言的（包括集群，数据库访问等）
     *
     * 缓存主要可分为二大类:
     * 1：通过文件缓存,顾名思义文件缓存是指把数据存储在磁盘上，不管你是以XML格式，序列化文件DAT格式还是其它文件格式；
     * 2：内存缓存，也就是创建一个静态内存区域，将数据存储进去，例如我们B/S架构的将数据存储在Application中或者存储在一个静态Map中。
     *
     * 本地缓存：JCache、Guava Cache等，分布式缓存：Redis、Memcached等
     * //一个本地的缓存Map（用map实现本地缓存）
     * private Map localCacheStore =new HashMap();
     */
}
