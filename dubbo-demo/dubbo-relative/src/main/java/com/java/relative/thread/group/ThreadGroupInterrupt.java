package com.java.relative.thread.group;

import java.util.concurrent.TimeUnit;

/**
 * 线程组的Interrupt
 * @author chensy
 * @date 2019-05-29 23:56
 */
public class ThreadGroupInterrupt {
    public static void main(String[] args) throws InterruptedException {
        ThreadGroup group = new ThreadGroup("TestGroup");
        new Thread(group, () -> {
            while(true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(2);
                } catch (InterruptedException e) {
                    //received interrupt signal and clear quickly
                    System.out.println("a ==" + Thread.currentThread().isInterrupted());
                    break;
                }
            }
            System.out.println("t1 will exit");
        }, "t1").start();
        new Thread(group, () -> {
            while(true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(2);
                } catch (InterruptedException e) {
                    //received interrupt signal and clear quickly
                    System.out.println("b == " + Thread.currentThread().isInterrupted());
                    break;
                }
            }
            System.out.println("t2 will exit");
        }, "t2").start();
        //make sure all threads start
        TimeUnit.MILLISECONDS.sleep(2);

        group.interrupt();
    }
}
