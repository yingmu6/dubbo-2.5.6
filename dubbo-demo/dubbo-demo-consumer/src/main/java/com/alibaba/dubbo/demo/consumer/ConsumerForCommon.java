package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.CommonService;
import com.alibaba.dubbo.demo.DemoService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 常用功能点
 */
public class ConsumerForCommon {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer-common.xml"});
        context.start();

        CommonService commonService = (CommonService) context.getBean("commonService"); // 获取远程服务代理
        commonService.dealRpcContext();
        System.in.read();
    }
}
