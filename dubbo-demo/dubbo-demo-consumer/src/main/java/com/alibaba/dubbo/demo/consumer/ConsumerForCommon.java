package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.demo.CommonService;
import com.alibaba.dubbo.demo.DemoService;
import com.alibaba.dubbo.rpc.RpcContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 常用功能点
 */
public class ConsumerForCommon {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();

        CommonService commonService = (CommonService) context.getBean("commonService");
        commonService.sayHello();
//        ConsumerForCommon test = new ConsumerForCommon();
//        test.dealRpcContext(context);1
        System.in.read();
    }

    /**
     * 上下文 RpcContext
     */
    public void dealRpcContext(ClassPathXmlApplicationContext context) {
        CommonService commonService = (CommonService) context.getBean("commonService"); // 获取远程服务代理
        RpcContext rpcContext = RpcContext.getContext();

        commonService.sayHello();

        RpcContext rpcContext2 = RpcContext.getContext();
        logger.info("是否提供方：" + rpcContext2.isProviderSide());
        logger.info("是否消费方：" + rpcContext2.isConsumerSide());
        System.out.println("路径URL:" + rpcContext2.getUrl());
    }
}
