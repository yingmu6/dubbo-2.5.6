package com.alibaba.dubbo.demo.consumer.self.consumer;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.demo.ApiDemo;

/**
 * @author : chensy
 * Date : 2020/5/19 上午7:50
 */
public class ApiConsumer2 {
    public static void main(String[] args) throws Exception {
        basicUse();
    }

    public static void basicUse() throws Exception {
        ReferenceConfig referenceConfig = initApplication();
        ApiDemo apiDemo = (ApiDemo) referenceConfig.get();
        String resultStr = apiDemo.sayHello("测试API");
        System.out.println(resultStr);
        System.in.read();
    }

    public static void extendUse() throws Exception {
        ReferenceConfig referenceConfig = initApplication();
        /**
         * history-v2 destroy() 方法来自哪里？是Node吗？都销毁了啥，怎么使用
         */
//        referenceConfig.destroy();
//        referenceConfig
        /**
         * history-v2 引用配置ReferenceConfig获取实例的过程都经历了什么？
         */
        ApiDemo apiDemo = (ApiDemo) referenceConfig.get();
        String resultStr = apiDemo.sayHello("扩展使用");
        System.out.println(resultStr);
        System.in.read();
    }

    public static ReferenceConfig initApplication() {
        ApplicationConfig applicationConfig = new ApplicationConfig("api_demo");
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setCheck(true);
        registryConfig.setAddress("localhost:2181");
        registryConfig.setProtocol("zookeeper"); // 不设置协议的话，默认dubbo
        /**
         * history-v2 为啥填写了注册中心后，消费者启动时会先调用 com.alibaba.dubbo.registry.RegistryService？
         */
        applicationConfig.setRegistry(registryConfig);

        ReferenceConfig referenceConfig = new ReferenceConfig();
//        referenceConfig.setInterface(ApiDemo.class); 设置接口的class
        // 设置接口时，类名需要全路径，简写会报java.lang.ClassNotFoundException: ApiDemo，因为使用了Class.forName根据全限定名称获取class的方法
        //referenceConfig.setInterface("ApiDemo"); 报错java.lang.ClassNotFoundException
        referenceConfig.setInterface("com.alibaba.dubbo.demo.ApiDemo");
        referenceConfig.setCheck(true);
        referenceConfig.setApplication(applicationConfig);

        /**
         * history-v2 目前zookeeper中消费者consumers的com.alibaba.dubbo.demo.ApiDemo节点的内容如下，解释下是如何产生这个url的
         * consumer://192.163.103.102/com.alibaba.dubbo.demo.ApiDemo?application=api_demo&category=consumers&check=false&dubbo=2.0.0&interface=com.alibaba.dubbo.demo.ApiDemo&methods=sayHello,sayApi&pid=11902&side=consumer&timestamp=1589933405741
         */
        return referenceConfig;
    }

}
