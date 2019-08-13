package com.alibaba.dubbo.demo;

/**
 * @author chensy
 * @date 2019-08-13 14:30
 */
public class DemoServiceMock implements DemoService {
    @Override
    public String sayHello(String name) {
        return "使用mock数据";
    }
}

/**
 * mock使用实现接口形式
 * 1）和接口在同一个包中
 * 2）命名方式：接口名+Mock后缀
 */
