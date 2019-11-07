package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.DemoService;

import com.alibaba.dubbo.rpc.RpcContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Map;

/**
 * Created by ken.lj on 2017/7/31.
 */
public class Consumer {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();

        DemoService demoService = (DemoService) context.getBean("demoService"); // 获取远程服务代理
        //循环调用3次
//        for (int i = 0; i < 3; i++) {
//            String hello = demoService.sayHello("world : " + i); // 执行远程方法
//            System.out.println("消费者：" + i + "," + hello); // 显示调用结果
//        }
        String hello = demoService.sayHello("你好！"); // 执行远程方法
        System.out.println("消费者：" + hello); // 显示调用结果

        new Consumer().showRpcContext();
        System.in.read();
    }

    /**
     * RpcContext上下文信息问题：
     * 1）RpcContext是在哪里设置的？
     */
    public void showRpcContext() {
        // 获取上下文信息
        RpcContext rpcContext = RpcContext.getContext();
        Map<String, Object> map =  rpcContext.get();

        System.out.println("RpcContext内容=" + rpcContext.getUrl());
    }
}
