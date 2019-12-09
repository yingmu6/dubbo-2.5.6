package com.alibaba.dubbo.demo.provider.impl.callback;

import com.alibaba.dubbo.demo.callback.CallbackListener;
import com.alibaba.dubbo.demo.callback.CallbackService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 参数回调 ： 服务端 -》 客户端，即服务端调用客户端逻辑
 * @author chensy
 * @date 2019-09-29 22:43
 */
public class CallbackServiceImpl implements CallbackService {
    private final Map<String, CallbackListener> listeners = new ConcurrentHashMap<>();

    public CallbackServiceImpl() {
        Thread t = new Thread() {
          public void run() {
              while (true) {
                  try {
                      for (Map.Entry<String, CallbackListener> entry : listeners.entrySet()) {
                          try {
                              entry.getValue().changed(getChanged(entry.getKey())); //回调客服端逻辑
                          } catch (Throwable t) {
                              listeners.remove(entry.getKey());
                          }
                      }
                      Thread.sleep(5000); //每个5秒执行一次
                  } catch (Throwable t) {
                      t.printStackTrace();
                  }
              }
          }
        };
        t.setDaemon(true);
        t.start();
    }

    public void addListener(String key, CallbackListener listener) {
        listeners.put(key, listener);
        listener.changed(getChanged(key)); // 发送变更通知
    }

    @Override
    public void removeListener(String key) {
        CallbackListener listener = listeners.get(key);
        listener.changed("移除事件：key = " + key);
        listeners.remove(key);
    }

    private String getChanged(String key) {
        return "回调：Changed: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}
