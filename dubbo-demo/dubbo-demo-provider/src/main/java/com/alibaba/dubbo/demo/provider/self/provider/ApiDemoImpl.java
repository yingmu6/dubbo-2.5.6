package com.alibaba.dubbo.demo.provider.self.provider;

import com.alibaba.dubbo.demo.ApiDemo;
import com.alibaba.dubbo.rpc.RpcContext;

/**
 * @author chensy
 * @date 2019-05-29 08:12
 */
public class ApiDemoImpl implements ApiDemo {

    @Override
    public String sayApi(String str, Integer age, Double price, String name) {
        //具体调用时，上下文信息有值
        RpcContext rpcContext1 = RpcContext.getContext();
        System.out.println(rpcContext1.getRemoteHost());
        RpcContext rpcContext2 = RpcContext.getContext();
        return "API :" + str + ", age:" + ",price:" + price + ",name：" + name;
    }

    @Override
    public String sayHello(String hello) {
        return "say:" + hello;
    }

}
