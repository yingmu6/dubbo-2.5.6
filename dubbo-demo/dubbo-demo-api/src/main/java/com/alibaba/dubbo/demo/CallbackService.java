package com.alibaba.dubbo.demo;

/**
 * 回调接口服务
 * @author chensy
 * @date 2019-09-29 22:39
 */
public interface CallbackService {
    void addListener(String key, CallbackListener listener);

    void removeListener(String key);
}
