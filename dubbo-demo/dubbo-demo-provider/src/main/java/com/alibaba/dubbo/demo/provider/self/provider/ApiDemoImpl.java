package com.alibaba.dubbo.demo.provider.self.provider;

import com.alibaba.dubbo.demo.ApiDemo;

/**
 * @author chensy
 * @date 2019-05-29 08:12
 */
public class ApiDemoImpl implements ApiDemo {

    @Override
    public String sayApi(String str, Integer age, Double price, String name) {
        return "API :" + str + ", age:" + ",price:" + price + ",name：" + name;
    }

    @Override
    public String sayHello(String hello) {
        return "say:" + hello;
    }

}
