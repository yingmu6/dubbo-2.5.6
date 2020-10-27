package com.alibaba.dubbo.demo.provider.self.provider;

import com.alibaba.dubbo.config.*;
import com.alibaba.dubbo.demo.ApiDemo;
import com.alibaba.dubbo.demo.ApiDemo2;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chensy
 * @date 2019-05-29 08:13
 */
public class providerStart1 {
    public static void main(String[] args) throws Exception {
        //配置优先级别  方法级别 > 接口级别 > 全局级别 ； 消费者级别 > 提供者级别

        //应用设置
        ApplicationConfig application = new ApplicationConfig();
        //非法字符
        //application.setName(".&api_demo");
        application.setName("api_demo");


        //注册中心设置
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("localhost:2181");
        registry.setProtocol("zookeeper");
        //registry.setFile();

        //协议测试
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("dubbo");
        protocol.setPort(20881);

        //提供者设置(可以被一个应用下的所有接口引用)
        ProviderConfig provider = new ProviderConfig();
        provider.setTimeout(3000);
        provider.setExport(false); //不暴露服务

        //暴露接口1
        ApiDemo apiDemo = new ApiDemoImpl();
        ServiceConfig service = new ServiceConfig();
        service.setApplication(application);
        service.setRegistry(registry);
        service.setProtocol(protocol);
        //若service和provider都配置了 export的值，以service为主
        service.setExport(true);
        service.setProvider(provider);
        service.setDelay(5);

        service.setInterface(ApiDemo.class);
        service.setRef(apiDemo);
        service.setFilter("selfFilter");


//        service.setVersion("1.3.4");
        //设置演示暴露
//        service.setDelay(15000);

        //方法级配置设置
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("sayApi");
        methodConfig.setTimeout(3000);
        ArgumentConfig argumentConfig0 = new ArgumentConfig();
        //index、type两个字段都会被忽略
        //argumentConfig0.setIndex(0);
        argumentConfig0.setType("java.lang.String");    //根据类型推出下标(不会推出下标，而是把)
        argumentConfig0.setCallback(false);

        ArgumentConfig argumentConfig1 = new ArgumentConfig();
        argumentConfig1.setIndex(1);
        //argumentConfig1.setType("java.lang.Integer");

        ArgumentConfig argumentConfig2 = new ArgumentConfig(); //检验下标与类型是否对应
        argumentConfig2.setIndex(2);
        argumentConfig2.setType("java.lang.Double");
        //argumentConfig2.setCallback(true); //java.lang.RuntimeException: java.lang.Double is not a interface 回调的参数需要接口形式，不能是个简单类型

        ArgumentConfig argumentConfig3 = new ArgumentConfig(); //检验下标与类型是否对应
        argumentConfig3.setIndex(3);
        argumentConfig3.setType("java.lang.String");
        //argumentConfig3.setCallback(true);           //若没设置，以同类型的值为准，若设置了，以设置为准

        List<ArgumentConfig> argumentConfigList = new ArrayList<>();
        argumentConfigList.add(argumentConfig0);
        argumentConfigList.add(argumentConfig1);
        argumentConfigList.add(argumentConfig2);
        argumentConfigList.add(argumentConfig3);

        methodConfig.setRetry(false);
        methodConfig.setArguments(argumentConfigList);

        List<MethodConfig> list = new ArrayList<>();
        list.add(methodConfig);

        service.setMethods(list);
        try {
            service.export(); //history-h1 为啥IllegalArgumentException异常不抛出来
        } catch (Exception e) {
            System.out.println("暴露服务异常:" + e.getMessage());
        }

        //暴露接口2 history-h1 为啥此处的接口没暴露出
//        ApiDemo2 apiDemo2 = new ApiDemoImpl2();
//        ServiceConfig service2 = new ServiceConfig();
//        service2.setApplication(application);
//        service2.setRegistry(registry);
//        service2.setProtocol(protocol);
//        service2.setProvider(provider);
//
//        service2.setInterface(ApiDemo2.class);
//        service2.setRef(apiDemo2);
//        service2.export();



        System.in.read();

    }

    /**
     * 同一台机器、同一个端口、同一个接口只暴露一个
     *
     * 同一台机器、不同端口、同一个接口暴露的服务不同
     */

    /**
     * 一个应用占用一个端口，不同应用不同端口
     */
}
