package com.alibaba.dubbo.demo.callback;

/**
 * @author chensy
 * @date 2019-09-29 22:41
 */
//回调监听器：发生变更时触发
public interface CallbackListener {
    void changed(String msg);
}
