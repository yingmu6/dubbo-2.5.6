package com.java.relative.jvm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 进程类测试
 * https://blog.csdn.net/zhoulupeng2012/article/details/49430455
 *
 * Process类是一个抽象类（所有的方法均是抽象的），封装了一个进程（即一个执行程序）。
 * Process 类提供了执行从进程输入、执行输出到进程、等待进程完成、检查进程的退出状态以及销毁（杀掉）进程的方法。
 * ProcessBuilder.start() 和 Runtime.exec 方法创建一个本机进程，并返回 Process 子类的一个实例，
 * 该实例可用来控制进程并获取相关信息。
 *
 * @author chensy
 * @date 2019-05-30 14:17
 */
public class ProcessTest {
    private static Log log = LogFactory.getLog(ProcessTest.class);

    /**
     * 读取控制命令的输出结果
     *
     *  cmd                命令
     *  isPrettify 返回的结果是否进行美化（换行），美化意味着换行，默认不进行美化,当此参数为null时也不美化，
     *  控制命令的输出结果
     */
    public static void main(String[] args) throws Exception {
        String cmd = "zkServer start";
        boolean isPrettify = true;
        StringBuffer cmdout = new StringBuffer();
        log.info("执行命令：" + cmd);
        Process process = Runtime.getRuntime().exec(cmd);     //执行一个系统命令
        InputStream fis = process.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        String line = null;
        if (isPrettify) {
            while ((line = br.readLine()) != null) {
                cmdout.append(line);
            }
        } else {
            while ((line = br.readLine()) != null) {
                cmdout.append(line).append(System.getProperty("line.separator"));
            }
        }
        //log.info("执行系统命令后的结果为：\n" + cmdout.toString());
        System.out.println("执行结果22:" + cmdout.toString());
    }
}
