package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.DemoService;

/**
 * @author chensy
 * @date 2019-11-07 13:15
 */
public class DemoChineseImpl implements DemoService {
    @Override
    public String sayHello(String name) {
        return "你好 " + name;
    }

    @Override
    public String sayLang(String who, String lang, Integer time) {
        return who + ", 从服务端响应: " + lang + ":" + time;
    }
}
