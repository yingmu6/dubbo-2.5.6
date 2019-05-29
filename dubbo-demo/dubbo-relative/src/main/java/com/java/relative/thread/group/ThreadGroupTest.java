package com.java.relative.thread.group;

/**
 * 线程组代表一组线程。 此外，线程组还可以包括其他线程组。 线程组形成一个树，除了初始线程组之外，每个线程组都有一个父进程。
 * 允许线程访问有关其线程组的信息，但不能访问有关其线程组的父线程组或任何其他线程组的信息。
 *
 * https://www.cnblogs.com/leoliu168/p/9928569.html
 *
 * @author chensy
 * @date 2019-05-29 23:22
 */
public class ThreadGroupTest {
    public static void main(String[] args) {
        //获取当前线程的group
        ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();
        //在当前线程执行流中新建一个Group1
        ThreadGroup group1 = new ThreadGroup("Group1");
        //Group1的父线程,就是main线程所在Group
        System.out.println(group1.getParent() == currentGroup);
        //定义Group2, 指定group1为其父线程
        ThreadGroup group2 = new ThreadGroup(group1, "Group2");
        System.out.println(group2.getParent() == group1);
    }
}



