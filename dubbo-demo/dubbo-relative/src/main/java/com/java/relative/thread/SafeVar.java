package com.java.relative.thread;

/**
 * @author chensy
 * @date 2019-05-29 14:33
 */
public class SafeVar implements Runnable {
    private int var = 0;
    @Override
    public void run() {
        int a = 2;
        while (var < 10) {
            var = var + a;
            System.out.println("变量值" + var);
        }
    }
}
