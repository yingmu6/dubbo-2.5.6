package com.csy.test.basic;

import com.alibaba.dubbo.config.*;

/**
 * @author chensy
 * @date 2019-01-16 12:02
 */
public class ApiConsumer {
    public static void main(String[] args) {
        ApplicationConfig application = new ApplicationConfig();
        application.setName("csytest");

        RegistryConfig registry = new RegistryConfig();
        //registry.setProtocol("zookeeper");
        registry.setAddress("192.168.0.104:2181");


        ReferenceConfig<HelloService> refService = new ReferenceConfig<HelloService>();
        refService.setApplication(application);
        //refService.setId("helloService");
        //refService.setCheck(false);
        refService.setInterface(HelloService.class);
        refService.setRegistry(registry);
        //refService.setProtocol("dubbo");
        refService.setVersion("1.0.0");

        //refService.setRef();
        HelloService helloService = refService.get();
        System.out.println(helloService.sayHello("zh"));
    }
}
