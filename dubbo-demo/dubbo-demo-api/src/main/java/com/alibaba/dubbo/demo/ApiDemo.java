package com.alibaba.dubbo.demo;

/**
 * @author chensy
 * @date 2019-05-29 08:06
 */
public interface ApiDemo {
    String sayHello(String hello);
    String sayApi(String str, Integer age, Double price, String name);
}
