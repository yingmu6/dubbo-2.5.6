package com.alibaba.dubbo.demo.provider.self.generic;

import com.alibaba.dubbo.config.*;
import com.alibaba.dubbo.rpc.service.GenericService;

/**
 * 泛化接口测试
 * @author chensy
 * @date 2019-07-09 18:11
 */
public class GenericTest {
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
        protocol.setPort(20883);

        //提供者设置(可以被一个应用下的所有接口引用)
        ProviderConfig provider = new ProviderConfig();
        provider.setTimeout(3000);

        GenericService genericService = new MyGenericService();
        ServiceConfig<GenericService> serviceConfig = new ServiceConfig<>();

        serviceConfig.setApplication(application);
        serviceConfig.setRegistry(registry);
        serviceConfig.setProtocol(protocol);
        serviceConfig.setExport(true);
        serviceConfig.setProvider(provider);

        serviceConfig.setInterface(GenericService.class);
        serviceConfig.setRef(genericService);
        serviceConfig.export();

        // ----------
//        GenericService genericSecond = new SecondGenericService();
//        ServiceConfig<GenericService> serviceConfigSecond = new ServiceConfig<>();
//
//        serviceConfigSecond.setApplication(application);
//        serviceConfigSecond.setRegistry(registry);
//        serviceConfigSecond.setProtocol(protocol);
//        serviceConfigSecond.setExport(true);
//        serviceConfigSecond.setProvider(provider);
//
//        serviceConfigSecond.setInterface(GenericService.class);
//        serviceConfigSecond.setRef(genericSecond);
//        serviceConfigSecond.export();

        System.in.read();
    }
}

/**
 * TODO
 * 1）泛化接口是不是 消费方、提供方联合使用，还是只要一方声明泛化接口？
 * 解答：需要联合使用，提供方暴露com.alibaba.dubbo.rpc.service.GenericService接口， 并且需要实现这个接口，是需要一个实现类即可。一个实现类代替所有泛化实现
 * 若写了多个实现类，不确定请求会落在哪个实现类
 *
 * 2）消费者，怎么选择提供者的接口？
 * 解答：提供方只需写一个实现类即可
 *
 * 3）提供者是否可以用不同的类，拥有相同的方法？
 * 解答：同上
 *
 * com.alibaba.dubbo.rpc.service.GenericService. No provider available for the service com.alibaba.dubbo.rpc.service.GenericService
 */
