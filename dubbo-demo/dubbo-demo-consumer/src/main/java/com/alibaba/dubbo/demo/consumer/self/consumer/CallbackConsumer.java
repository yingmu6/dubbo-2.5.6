package com.alibaba.dubbo.demo.consumer.self.consumer;

import com.alibaba.dubbo.demo.CallbackListener;
import com.alibaba.dubbo.demo.CallbackService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 参数回调
 * @author chensy
 * @date 2019-09-29 22:59
 */
public class CallbackConsumer {
    public static void main(String[] args) throws Exception{
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-callback-consumer.xml"});
        context.start();

        CallbackService callbackService = (CallbackService) context.getBean("callbackService");

        callbackService.addListener("foo.bar", new CallbackListener() {
            public void changed(String msg) {
                System.out.println("callback1:" + msg);
            }
        });

        /**
         * 为啥此处加上removeListener方法 报错
         * com.alibaba.dubbo.common.bytecode.NoSuchMethodException: Not found method "removeListener" in class com.alibaba.dubbo.demo.provider.CallbackServiceImpl.
         * 解答：需要编译一下  mvn clean install
         */
        callbackService.removeListener("foo.bar");
        System.in.read();
    }
}
