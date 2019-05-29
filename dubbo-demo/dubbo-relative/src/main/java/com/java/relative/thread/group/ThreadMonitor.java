package com.java.relative.thread.group;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;

/**
 * 线程监控程序
 *
 * 1、通过java线程管理器MXBean
 * 2、直接通过线程组获取线程总数，要注意需要获取根线程组的总数，否则不准确
 *
 * @author chensy
 * @date 2019-05-30 00:07
 */
public class ThreadMonitor {
    public static void main(String[] args) {
        // 计算方法1
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        while (threadGroup.getParent() != null) {
            threadGroup = threadGroup.getParent();
        }
        int totalThread = threadGroup.activeCount();
        System.out.println("当前程序线程总数： " + totalThread);
        Thread[] lstThreads = new Thread[totalThread];
        threadGroup.enumerate(lstThreads);
        for (int i = 0; i < totalThread; i++) {
            System.out.println("线程号：" + lstThreads[i].getId() + " = " + lstThreads[i].getName());
        }
        // 计算方法2
        // 获取java线程管理器MXBean，dumpAllThreads参数：lockedMonitors参数表示是否获取同步的monitor信息，
        //lockedSynchronizers表示是否获取同步的synchronizer
        ThreadInfo[] threadInfos = ManagementFactory.getThreadMXBean().dumpAllThreads(false, false);
        for (ThreadInfo threadInfo : threadInfos) {
            System.out.println("[" + threadInfo.getThreadId() + "]" + threadInfo.getThreadName());
        }

    }
}
