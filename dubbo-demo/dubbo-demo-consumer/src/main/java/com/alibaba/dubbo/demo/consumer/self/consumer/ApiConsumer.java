package com.alibaba.dubbo.demo.consumer.self.consumer;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.demo.ApiDemo;

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

        ApiDemo apiDemo = (ApiDemo) reference.get();
        System.out.println(apiDemo.sayApi("haha "));

        System.in.read();
    }
}
