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
package com.alibaba.dubbo.common.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * InternalThreadFactory.
 *
 * @author qian.lei
 */

public class NamedThreadFactory implements ThreadFactory {/**@c 线程工厂，创建线程 */ // ThreadFactory工厂模式了解，解：属于工厂方法模式
    private static final AtomicInteger POOL_SEQ = new AtomicInteger(1);

    private final AtomicInteger mThreadNum = new AtomicInteger(1);

    private final String mPrefix;

    private final boolean mDaemo;

    /**
     * 线程组 ThreadGroup：A thread group represents a set of threads.（一组线程的集合）
     * https://juejin.im/post/6844903811899719694
     *
     * 线程组(ThreadGroup)简单来说就是一个线程集合。线程组的出现是为了更方便地管理线程。
     * 线程组是父子结构的，一个线程组可以集成其他线程组，同时也可以拥有其他子线程组。从结构上看，线程组是一个树形结构，
     * 每个线程都隶属于一个线程组，线程组又有父线程组，这样追溯下去，可以追溯到一个根线程组——System线程组
     *
     * 一个线程可以访问其所属线程组的信息，但不能访问其所属线程组的父线程组或者其他线程组的信息。
     */
    private final ThreadGroup mGroup;

    public NamedThreadFactory() {
        this("pool-" + POOL_SEQ.getAndIncrement(), false);
    }

    public NamedThreadFactory(String prefix) {
        this(prefix, false);
    }

    /**
     * 守护线程：
     * 1）守护线程是为其他线程服务的线程；
     * 2）所有非守护线程都执行完毕后，虚拟机退出；
     * 3）守护线程不能持有需要关闭的资源（如打开文件等）
     *
     * https://www.liaoxuefeng.com/wiki/1252599548343744/1306580788183074
     *
     * SecurityManager:
     * Java安全：SecurityManager与AccessController   https://juejin.im/post/6844903657775824910
     * 安全管理器是Java API和应用程序之间的“第三方权威机构”。
     */
    public NamedThreadFactory(String prefix, boolean daemo) {
        mPrefix = prefix + "-thread-";
        mDaemo = daemo;
        SecurityManager s = System.getSecurityManager();
        mGroup = (s == null) ? Thread.currentThread().getThreadGroup() : s.getThreadGroup();
    }

    public Thread newThread(Runnable runnable) {
        String name = mPrefix + mThreadNum.getAndIncrement();
        Thread ret = new Thread(mGroup, runnable, name, 0);
        ret.setDaemon(mDaemo);
        return ret;
    }

    public ThreadGroup getThreadGroup() {
        return mGroup;
    }
}