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

        Consumer test = new Consumer();
        //test.showRpcContext();
        test.serviceGroup(context);
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

    /**
     * 服务分组
     * 同一个接口有不同的实现，需要分组管理，若不分组管理，获取的实例会随机
     * 1）接口调用A=Hello aaa ...  , 接口调用B=Hello bbb
     * 2) 接口调用A=Hello aaa... , 接口调用B=你好
     */
    public void serviceGroup(ClassPathXmlApplicationContext context) {
        DemoService demoService = (DemoService) context.getBean("demoChinese"); // 获取远程服务代理
        String hello = demoService.sayHello("aaa！"); // 执行远程方法
        System.out.println("接口调用A=" + hello); // 显示调用结果

        DemoService demoService2 = (DemoService) context.getBean("demoService"); // 获取远程服务代理
        System.out.println("接口调用B=" + demoService2.sayHello("bbb"));

        /**
         * group="*" 配置任意分组
         * 1) 任意调用=Hello any
         * 2) 任意调用=Hello any
         * 调用多次都是同一个结果，没有任意选择？
         * 解：若选择*，则由负载均衡算法计算使用哪个服务，如果计算条件不变，就总是同一个接口。测试中将不同服务的权重变更，发现group=*的值会变为具体的分组
         */
        DemoService anyService = (DemoService) context.getBean("demoAny");
        System.out.println("任意调用=" + anyService.sayHello("any"));
    }

}
