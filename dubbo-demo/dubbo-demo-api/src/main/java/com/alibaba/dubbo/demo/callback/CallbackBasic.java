package com.alibaba.dubbo.demo.callback;

/**
 * @author chensy
 * @date 2019-11-08 23:27
 */
public interface CallbackBasic {
    String sayHello(String from, CallbackBasicListener basicListener);
}

/**
 * 基本回调使用：
 * 问题：
 * 1）服务端回调客户端，能否拿到返回结果？
 * 2）回调的参数，必须是接口吗？
 */
