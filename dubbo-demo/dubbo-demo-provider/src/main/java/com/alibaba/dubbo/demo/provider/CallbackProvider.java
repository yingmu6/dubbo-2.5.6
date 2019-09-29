package com.alibaba.dubbo.demo.provider;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author chensy
 * @date 2019-09-29 22:54
 */
public class CallbackProvider {
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-callback-provider.xml"});
        context.start();

        System.in.read(); // 按任意键退出
    }
}
