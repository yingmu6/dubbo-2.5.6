package com.alibaba.dubbo.demo.consumer.self.consumer;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.demo.ApiDemo;
import com.alibaba.dubbo.rpc.service.GenericService;

import java.util.HashMap;
import java.util.Map;

/**
 * 泛化调用：
 * 泛化接口调用方式主要用于客户端没有 API 接口及模型类元的情况，参数及返回值中的所有 POJO 均用 Map 表示，
 * 通常用于框架集成，比如：实现一个通用的服务测试框架，可通过 GenericService 调用所有服务实现。
 *
 * @author chensy
 * @date 2019-07-09 19:35
 */
public class ConsumerGenericTest {
    public static void main(String[] args) throws Exception{
        ApplicationConfig application = new ApplicationConfig();
        application.setName("generic_demo33");

        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("localhost:2181");
        registry.setProtocol("zookeeper");

        ReferenceConfig reference = new ReferenceConfig();
        reference.setApplication(application);
        reference.setRegistry(registry);
        reference.setInterface(GenericService.class);

        GenericService genericService = (GenericService) reference.get();
        String[] paramtypes = new String[2];
        paramtypes[0] = "java.lang.String";
        paramtypes[1] = "java.lang.Integer";


//        Object[] argValus = new Object[2];
//        argValus[0] = "consumer hh ";
//        argValus[1] = 12;
//        Object object = genericService.$invoke("sayHi", paramtypes, argValus);
//        System.out.println(object);

        //指定什么类型，就转换什么类型
        Map<String, Object> person = new HashMap<>();
        person.put("name", "张三");
        person.put("sex", 13);
        //Map<String, Object> resultMap = (Map<String, Object>)genericService.$invoke("sayPojo", new String[]{"com.alibaba.dubbo.demo.Person"}, new Object[]{person});
        //System.out.println(resultMap);

        //返回类型是对象类型时，对象需要实现序列化接口
        System.out.println(genericService.$invoke("sayPojo", new String[]{"com.alibaba.dubbo.demo.Person"}, new Object[]{person}));;

        System.in.read();
    }

    /**
     * generic_demo33 应用名不影响查找，写错也没关系
     *
     * 泛型接口
     * 1）同一个应用，不同端口 （generic_demo 应用名，端口 20883、20884）
     * 暴露的泛化接口会合并到一起，都以com.alibaba.dubbo.rpc.service.GenericService暴露出来， 并且请求不确定落在哪个泛化接口的实现类上，
     * 都可能出现，此处既出现MyGenericService的结果，也出先SecondGenericService的结果
     *
     * 2)泛化结果分为传递基本类型和POJO对象类型
     *  2.1） 基本类型以及Date,List,Map等不需要转换，直接调用
     *  2.2） 用Map表示POJO参数，如果返回值为POJO也将自动转成Map
     */
}
