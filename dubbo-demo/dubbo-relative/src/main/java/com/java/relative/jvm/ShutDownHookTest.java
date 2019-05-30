package com.java.relative.jvm;

/**
 * 钩子函数使用，优雅停机
 * @author chensy
 * @date 2019-05-30 12:08
 */
public class ShutDownHookTest {
    public static void main(String[] args) throws Exception {

        Thread hook = new Thread(new Runnable() {
            public void run() {
                System.out.println("优雅停机");
            }
        }, "DubboShutdownHook");

        // 添加钩子
        Runtime.getRuntime().addShutdownHook(hook);
        // 移除钩子（退出时，不再执行钩子函数内内容）
        Runtime.getRuntime().removeShutdownHook(hook);
        System.out.println("main 退出");
        System.in.read();
    }
}

/**
 * 当JVM停止时会执行钩子函数
 * kill 命令会执行
 * 但kill -9 不会执行钩子函数
 */
