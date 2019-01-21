package com.csy.test.basic;

/**
 * @author chensy
 * @date 2019-01-16 12:01
 */
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String language) {
        String hello;
        if (language.equals("zh")) {
            hello = "你好";
        } else {
            hello = "how are you";
        }
        return hello;
    }
}
