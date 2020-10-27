package com.alibaba.dubbo.demo.consumer.self.consumer;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.demo.ApiDemo;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.service.GenericService;

/**
 * @author chensy
 * @date 2019-05-29 08:13
 */
public class ApiConsumer {
    public static void main(String[] args) throws Exception {
        ApplicationConfig application = new ApplicationConfig();
        application.setName("api_demo");

        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("localhost:2181");
        registry.setProtocol("zookeeper");

        ReferenceConfig reference = new ReferenceConfig();
        reference.setApplication(application);
        reference.setRegistry(registry);
        reference.setInterface(ApiDemo.class);

        reference.setMock("return 112233"); //history 10/01 mock怎么使用？为啥此处没生效？

        //reference.setMethods(); 可以指定调用的方法列表

        RpcContext rpcContext1 = RpcContext.getContext();

        ApiDemo apiDemo = (ApiDemo) reference.get();
        RpcContext rpcContext2 = RpcContext.getContext();
        System.out.println("上下文信息：" + rpcContext2.getUrl());

        System.out.println("调用输出值：" + apiDemo.sayApi("haha ",12, 13.45, "张三"));
        // 接口调用后，信息会设置到RpcContext
        RpcContext rpcContext3 = RpcContext.getContext();

        // rpcContext1、rpcContext2、rpcContext3三个对象是同一个对象，static final
        // RpcContext消费者或提供者一方都是一个对象，但消费者和提供者的对象不一样，各管各的
        System.in.read();
    }

    /**
     * mock使用(两种方式)
     * https://blog.csdn.net/wsm0712syb/article/details/61413276
     * 1) mock配置方式，setMock("return 112233")
     * 2）mock实现接口方式 （在提供方增加mock实现类）
     */
}
