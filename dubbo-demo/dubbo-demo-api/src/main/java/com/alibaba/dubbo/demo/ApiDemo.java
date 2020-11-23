package com.alibaba.dubbo.demo;

/**
 * @author chensy
 * @date 2019-05-29 08:06
 */
public interface ApiDemo { //todo 11/23 加上注解，看是否能解析到
    String sayHello(String hello);
    String sayApi(String str, Integer age, Double price, String name);
}
