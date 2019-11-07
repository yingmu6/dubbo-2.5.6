package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.DemoService;

import com.alibaba.dubbo.rpc.RpcContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Map;

/**
 * Created by ken.lj on 2017/7/31.
 */
public class Consumer {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();

        Consumer test = new Consumer();
        //test.showRpcContext();
        //test.serviceGroup(context);
        test.serviceVersion(context);
        System.in.read();
    }

    /**
     * 上下文中存放的是当前调用过程中所需的环境信息 http://dubbo.apache.org/zh-cn/docs/user/demos/context.html
     * RpcContext 是一个 ThreadLocal 的临时状态记录器
     * RpcContext上下文信息问题：
     * 1）RpcContext是在哪里设置的？
     */
    public void showRpcContext() {
        // 获取上下文信息
        RpcContext rpcContext = RpcContext.getContext();
        Map<String, Object> map =  rpcContext.get();

        System.out.println("RpcContext内容=" + rpcContext.getUrl());
    }

    /**
     * 服务分组: 当一个接口有多种实现时，可以用 group 区分。 http://dubbo.apache.org/zh-cn/docs/user/demos/multi-versions.html
     * 同一个接口有不同的实现，需要分组管理，若不分组管理，获取的实例会随机
     * 1）接口调用A=Hello aaa ...  , 接口调用B=Hello bbb
     * 2) 接口调用A=Hello aaa... , 接口调用B=你好
     */
    public void serviceGroup(ClassPathXmlApplicationContext context) {
        DemoService demoService = (DemoService) context.getBean("demoChinese"); // 获取远程服务代理
        String hello = demoService.sayHello("aaa！"); // 执行远程方法
        System.out.println("接口调用A=" + hello); // 显示调用结果

        DemoService demoService2 = (DemoService) context.getBean("demoService"); // 获取远程服务代理
        System.out.println("接口调用B=" + demoService2.sayHello("bbb"));

        /**
         * group="*" 配置任意分组
         * 1) 任意调用=Hello any
         * 2) 任意调用=Hello any
         * 调用多次都是同一个结果，没有任意选择？
         * 解：若选择*，则由负载均衡算法计算使用哪个服务，如果计算条件不变，就总是同一个接口。测试中将不同服务的权重变更，发现group=*的值会变为具体的分组
         */
        DemoService anyService = (DemoService) context.getBean("demoAny");
        System.out.println("任意调用=" + anyService.sayHello("any"));
    }

    /**
     * 当一个接口实现，出现不兼容升级时，可以用版本号过渡，版本号不同的服务相互间不引用。 http://dubbo.apache.org/zh-cn/docs/user/demos/multi-versions.html
     * 可以按照以下的步骤进行版本迁移：
     * 1) 在低压力时间段，先升级一半提供者为新版本
     * 2) 再将所有消费者升级为新版本
     * 3) 然后将剩下的一半提供者升级为新版本
     *
     * 一个接口多个实现时，zk中的接口会加上序号com.alibaba.dubbo.demo.DemoService2, ***.DemoService3, ***.DemoService4
     */
    public void serviceVersion(ClassPathXmlApplicationContext context) {
        DemoService demoServiceV1 = (DemoService) context.getBean("demoServiceV1");
        System.out.println("版本V1=" + demoServiceV1.sayHello("张三"));

        DemoService demoServiceV2 = (DemoService) context.getBean("demoServiceV2");
        System.out.println("版本V2=" + demoServiceV2.sayHello("李四"));
    }

}
