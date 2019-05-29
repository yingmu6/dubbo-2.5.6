package com.alibaba.dubbo.demo.provider.self.provider;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.demo.ApiDemo;

/**
 * @author chensy
 * @date 2019-05-29 08:13
 */
public class ApiProvider {
    public static void main(String[] args) throws Exception {
        ApplicationConfig application = new ApplicationConfig();
        application.setName("api_demo");

        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("localhost:2181");
        registry.setProtocol("zookeeper");

        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("dubbo");
        protocol.setPort(20880);

        ApiDemo apiDemo = new ApiDemoImpl();
        ServiceConfig service = new ServiceConfig();
        service.setApplication(application);
        service.setRegistry(registry);
        service.setProtocol(protocol);
        service.setInterface(ApiDemo.class);
        service.setRef(apiDemo);
        service.export();

        System.in.read();

    }
}
