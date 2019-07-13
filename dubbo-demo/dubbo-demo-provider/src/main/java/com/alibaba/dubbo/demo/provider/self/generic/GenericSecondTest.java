package com.alibaba.dubbo.demo.provider.self.generic;

import com.alibaba.dubbo.config.*;
import com.alibaba.dubbo.rpc.service.GenericService;

/**
 * 泛化接口测试
 * @author chensy
 * @date 2019-07-09 18:11
 */
public class GenericSecondTest {
    public static void main(String[] args) throws Exception{
        //应用设置
        ApplicationConfig application = new ApplicationConfig();
        application.setName("generic_demo");


        //注册中心设置
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("localhost:2181");
        registry.setProtocol("zookeeper");

        //协议测试
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("dubbo");
        protocol.setPort(20885);

        //提供者设置(可以被一个应用下的所有接口引用)
        ProviderConfig provider = new ProviderConfig();
        provider.setTimeout(3000);


        // ----------
        GenericService genericSecond = new SecondGenericService();
        ServiceConfig<GenericService> serviceConfigSecond = new ServiceConfig<>();

        serviceConfigSecond.setApplication(application);
        serviceConfigSecond.setRegistry(registry);
        serviceConfigSecond.setProtocol(protocol);
        serviceConfigSecond.setExport(true);
        serviceConfigSecond.setProvider(provider);

        serviceConfigSecond.setInterface(GenericService.class);
        serviceConfigSecond.setRef(genericSecond);
        serviceConfigSecond.export();

        System.in.read();
    }
}

