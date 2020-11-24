package com.alibaba.dubbo.demo;

/**
 * 常用功能点
 * @author chensy
 * @date 2019-11-07 17:51
 */
@TestLog(log = "test")
public interface CommonService {
    String sayHello();
    String sayHello(String str);
    String sayHello(String str, Integer a);
    String test(Integer a, Double b, String[] c);
}
