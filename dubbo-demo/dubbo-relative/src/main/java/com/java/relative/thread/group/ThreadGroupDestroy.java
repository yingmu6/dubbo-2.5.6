package com.java.relative.thread.group;

/**
 * @author chensy
 * @date 2019-05-29 23:59
 */
public class ThreadGroupDestroy {
    public static void main(String[] args) {
        ThreadGroup group = new ThreadGroup("TestGroup");
        ThreadGroup mainGroup = Thread.currentThread().getThreadGroup();
        //before destroy
        System.out.println("group.isDestroyed=" + group.isDestroyed());
        mainGroup.list();

        group.destroy();
        //after destroy
        System.out.println("group.isDestroyed=" + group.isDestroyed());
        mainGroup.list();
    }
}
