package com.java.relative.thread.group;

import java.util.concurrent.TimeUnit;

/**
 * @author chensy
 * @date 2019-05-29 23:45
 *
 * 注意：后添加进线程组的线程，其优先级不能大于线程组的优先级
 */
public class ThreadGroupBasic {
    public static void main(String[] args) throws InterruptedException {

        ThreadGroup group = new ThreadGroup("group1");
        Thread thread = new Thread(group, () -> {
            while(true) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "thread");
        thread.setDaemon(true);
        thread.start();

        TimeUnit.MILLISECONDS.sleep(1);

        ThreadGroup mainGroup = Thread.currentThread().getThreadGroup();
        //递归获取mainGroup中活跃线程的估计值
        System.out.println("activeCount = " + mainGroup.activeCount());
        //递归获mainGroup中的活跃子group
        System.out.println("activeGroupCount = " + mainGroup.activeGroupCount());
        //获取group的优先级, 默认为10
        System.out.println("getMaxPriority = " + mainGroup.getMaxPriority());
        //获取group的名字
        System.out.println("getName = " + mainGroup.getName());
        //获取group的父group, 如不存在则返回null
        System.out.println("getParent = " + mainGroup.getParent());
        //活跃线程信息全部输出到控制台
        mainGroup.list();
        System.out.println("----------------------------");
        //判断当前group是不是给定group的父线程, 如果两者一样,也会返回true
        System.out.println("parentOf = " + mainGroup.parentOf(group));
        System.out.println("parentOf = " + mainGroup.parentOf(mainGroup));

    }

    /**
     * 监控线程的3种方法
     * https://blog.csdn.net/weixin_33763244/article/details/86364863
     *
     * 1.jps -l 查询当前运行线程
     * 2.JDK1.7以后的命令行工具－jcmd
     * 3. 通过程序监控
     */
}
