package com.java.relative;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ScheduledExecutorService定时周期执行指定的任务
 *  https://blog.csdn.net/tsyj810883979/article/details/8481621
 *
 *
 * 定时任务执行
 * @author chensy
 * @date 2019-05-29 19:50
 */
public class ScheduledExecutorTest {
    private static final ScheduledExecutorService delayExportExecutor =
            Executors.newSingleThreadScheduledExecutor(
                    new com.alibaba.dubbo.common.utils.NamedThreadFactory("DubboServiceDelayExporter", true));

    public static void main(String[] args) throws Exception {
        final long start = System.currentTimeMillis();

        ScheduledExecutorTest executorTest = new ScheduledExecutorTest();
        //多线程执行
        //executorTest.delay();

        //executeFixedRate();

        //executeFixedDelay();

        executorTest.executeEightAtNightPerDay();

        System.out.println("主线程 耗时 " + (System.currentTimeMillis() - start));
        System.in.read();
    }

    /**
     * 从当前时间延迟指定时间执行
     *
     * command：执行线程
     * delay：从当时开始延迟指定时间
     * unit：计时单位
     *
     * schedule(Runnable command,long delay, TimeUnit unit)
     */
    public void delay() {
        final long start = System.currentTimeMillis();
        // 此处创建了线程并放入线程值中，从这里主线程和新的线程就并发执行了
        delayExportExecutor.schedule(new Runnable() { //内部类访问局部变量需要加final
            public void run() {
                System.out.println("值：耗时：" + (System.currentTimeMillis() - start));
            }
        }, 5000, TimeUnit.MILLISECONDS);
    }

    /**
     * initialDelay：初始化延时 貌似没用
     * 以固定周期频率执行任务（每隔一段时间执行一次）
     */
    public static void executeFixedRate() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        final int no = 1;
        final long start = System.currentTimeMillis();
        executor.scheduleAtFixedRate(
                new Runnable() { //内部类访问局部变量需要加final
                    public void run() {
                        System.out.println(no + " 值：耗时：" + (System.currentTimeMillis() - start));
                    }
                 },
                20,
                100,
                TimeUnit.MILLISECONDS);
    }

    /**
     * 以固定延迟时间进行执行
     * 本次任务执行完成后，需要延迟设定的延迟时间，才会执行新的任务
     */
    public static void executeFixedDelay() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        final long start = System.currentTimeMillis();
        executor.scheduleWithFixedDelay(
                new Runnable() { //内部类访问局部变量需要加final
                    public void run() {
                        System.out.println(" 值：耗时：" + (System.currentTimeMillis() - start));
                    }
                },
                0,
                100,
                TimeUnit.MILLISECONDS);
    }

    /**
     * 每天晚上8点执行一次
     * 每天定时安排任务进行执行
     */
    public  void executeEightAtNightPerDay() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        long oneDay = 24 * 60 * 60 * 1000;
        long initDelay  = getTimeMillis("23:03:00") - System.currentTimeMillis();
        initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;

        executor.scheduleAtFixedRate(
                new EchoServer(),
                initDelay,
                oneDay,
                TimeUnit.MILLISECONDS);
    }

    /**
     * 获取指定时间对应的毫秒数
     * @param time "HH:mm:ss"
     * @return
     */
    private static long getTimeMillis(String time) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
            DateFormat dayFormat = new SimpleDateFormat("yy-MM-dd");
            Date curDate = dateFormat.parse(dayFormat.format(new Date()) + " " + time);
            return curDate.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    class EchoServer implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("This is a echo server. The current time is " +
                    System.currentTimeMillis() + ".");
        }
    }




}
