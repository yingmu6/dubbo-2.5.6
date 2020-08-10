package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.demo.CommonService;
import com.alibaba.dubbo.rpc.RpcContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 常用功能点
 */
public class ConsumerForCommonV2 {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer-v2.xml"});
        context.start();

        for (int i = 0; i < 10; i++) {
            CommonService commonService = (CommonService) context.getBean("commonService");
            System.out.println(commonService.sayHello("aa"));
//        ConsumerForCommon test = new ConsumerForCommon();
//        test.dealRpcContext(context);
        }
        System.in.read();
    }
}
