package com.alibaba.dubbo.demo.provider;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 常用知识点
 */
public class ProviderForCommon {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-provider-common.xml"});
        context.start();

        System.in.read(); // 按任意键退出
    }

    /**
     * 服务提供者启动日志：
     * 第一步：启动Spring容器 [22/02/20 05:44:54:054 CST] main  INFO support.ClassPathXmlApplicationContext: Refreshing org.springframework.context.support.ClassPathXmlApplicationContext@2833cc44: startup date [Sat Feb 22 17:44:54 CST 2020]; root of context hierarchy
     * 第二步：加载dubbo的xml配置文件[22/02/20 05:44:54:054 CST] main  INFO xml.XmlBeanDefinitionReader: Loading XML bean definitions from class path resource [META-INF/spring/dubbo-demo-provider-common.xml]
     * 第三步：指定日志 [22/02/20 05:44:55:055 CST] main  INFO logger.LoggerFactory: using logger: com.alibaba.dubbo.common.logger.log4j.Log4jLoggerAdapter（从哪里打印出来的？）
     * 第四部：Spring容器创建实例 [22/02/20 05:44:55:055 CST] main  INFO config.AbstractConfig:  [DUBBO] The service ready on spring started. service: com.alibaba.dubbo.demo.CommonService, dubbo version: 2.0.0, current host: 127.0.0.1
     * 第五部：暴露本地服务： [22/02/20 05:44:56:056 CST] main  INFO config.AbstractConfig:  [DUBBO] Export dubbo service com.alibaba.dubbo.demo.CommonService to local registry, dubbo version: 2.0.0, current host: 127.0.0.1
     * 第六部：暴露远程服务[22/02/20 05:44:56:056 CST] main  WARN config.AbstractConfig:  [DUBBO] 暴露远程服务：interface:com.alibaba.dubbo.demo.CommonService, to url :dubbo://192.168.0.102:20885/com.alibaba.dubbo.demo.CommonService?anyhost=true&application=demo-provider&dubbo=2.0.0&generic=false&interface=com.alibaba.dubbo.demo.CommonService&methods=sayHello&pid=44899&side=provider&timestamp=1582364695883, dubbo version: 2.0.0, current host: 127.0.0.1
     */

}
