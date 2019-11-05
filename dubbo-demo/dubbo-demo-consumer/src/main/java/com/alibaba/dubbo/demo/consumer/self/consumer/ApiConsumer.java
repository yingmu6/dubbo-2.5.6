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

        reference.setMock("return 112233");

        //reference.setMethods(); 可以指定调用的方法列表

        ApiDemo apiDemo = (ApiDemo) reference.get();
        System.out.println("调用输出值：" + apiDemo.sayApi("haha ",12, 13.45, "张三"));

        RpcContext rpcContext = RpcContext.getContext();
        System.out.println("上下文信息：" + rpcContext.getUrl());
        System.in.read();
    }

    /**
     * mock使用(两种方式)
     * https://blog.csdn.net/wsm0712syb/article/details/61413276
     * 1) mock配置方式，setMock("return 112233")
     * 2）mock实现接口方式 （在提供方增加mock实现类）
     */
}
