package com.java.relative;

/**
 * 多线程通过final访问主线程局部变量
 * https://xtuhcy.iteye.com/blog/2170295
 *
 * 有关线程安全的探讨--final、static、单例、线程安全
 * https://www.cnblogs.com/bellkosmos/p/5340711.html
 *
 * @author chensy
 * @date 2019-05-29 14:36
 */
public class Test {
    public static void main(String[] args) {
        SafeVar safeVar = new SafeVar();
        Thread thread = new Thread(safeVar);
        thread.start();
    }
}
