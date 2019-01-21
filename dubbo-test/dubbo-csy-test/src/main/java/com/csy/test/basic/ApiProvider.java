package com.csy.test.basic;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.rpc.Protocol;

/**
 *
 * API 方式调用
 * @author chensy
 * @date 2019-01-16 12:02
 */
public class ApiProvider {
    public static void main(String[] args) throws Exception{
        HelloService helloService = new HelloServiceImpl();

        ApplicationConfig application = new ApplicationConfig();
        application.setName("csytest");
        //application.setMonitor();
        //application.setRegistries();

        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("192.168.0.104:2181");
        registry.setProtocol("zookeeper");

        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("dubbo");
        protocol.setPort(20880);
        protocol.setThreads(200);

        ServiceConfig<HelloService> service = new ServiceConfig<HelloService>();
        service.setApplication(application);
        service.setRegistry(registry);
        service.setProtocol(protocol);
        service.setInterface(HelloService.class);
        service.setVersion("1.0.0");
        service.setRef(helloService);


        service.export();

        System.in.read();
    }
}
