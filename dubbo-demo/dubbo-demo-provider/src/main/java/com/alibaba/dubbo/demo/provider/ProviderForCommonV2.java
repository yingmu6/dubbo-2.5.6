package com.alibaba.dubbo.demo.provider;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 常用知识点
 */
public class ProviderForCommonV2 {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-provider-common-v2.xml"});
        context.start();

        System.in.read(); // 按任意键退出
    }

}
