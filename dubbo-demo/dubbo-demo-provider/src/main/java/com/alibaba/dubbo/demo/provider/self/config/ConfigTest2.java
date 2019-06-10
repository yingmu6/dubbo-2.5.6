package com.alibaba.dubbo.demo.provider.self.config;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.annotation.Service;

/**
 * @author chensy
 * @date 2019-06-09 23:03
 */
public class ConfigTest2 {
    public static void main(String[] args) {
        ApplicationConfig application = new ApplicationConfig();

        // 注解获取
        Service service = AnnotationSelfImpl.class.getAnnotation(Service.class);

        //Service service = new Service();
        application.testAppendAnnotation(Service.class, service);
        System.out.println(application.toString());

        ApplicationConfig application2 = new ApplicationConfig();
        Service service2 = AnnotationSelfImpl.class.getAnnotation(Service.class);
        System.out.println("----override-----");
        application2.testAppendAnnotationSelf(Service.class, service2);
        System.out.println(application2.toString());
    }



}
