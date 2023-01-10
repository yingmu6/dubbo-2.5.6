package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.demo.CommonService;
import com.alibaba.dubbo.demo.TestLog;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.service.EchoService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 常用功能点
 */
public class ConsumerForCommon { //test3
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();

//        for (int i = 0; i < 10; i++) {
//            CommonService commonService = (CommonService) context.getBean("commonService");
////            System.out.println(commonService.sayHello());
////            System.out.println(commonService.sayHello("你好"));
////        ConsumerForCommon test = new ConsumerForCommon();
////        test.dealRpcContext(context);1
//            String[] arr = new String[2];
//            arr[0] = "aa";
//            arr[1] = "cc";
//            System.out.println("测试：" + commonService.test(1,2.5, arr));
//        }
        CommonService commonService = (CommonService) context.getBean("commonService");

//        TestLog testLog = commonService.getClass().getAnnotation(TestLog.class);
//        System.out.println("注解中内容：" + testLog.log());

        //回声测试
        EchoService echoService = (EchoService) commonService;
        String msg = (String)echoService.$echo("aaa");
        System.out.println("回声测试：" + msg);
        /**
         * 输出：  来自：V1 port:20881 ,hello 你好
         * 其中："V1 port:20881" 是提供者初始化对象时，设置的属性值
         * 就像使用本地对象一样：提供者将对象实例化，消费者取对象
         */
        System.out.println(commonService.sayHello());
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
