package com.alibaba.dubbo.demo.provider.impl;

import com.alibaba.dubbo.demo.DemoService;

public class DemoServicePortImpl implements DemoService {

    public String sayHello(String name) {
        return "端口Port " + name;
    }

    @Override
    public String sayLang(String who, String lang, Integer time) {
        return who + ", response form provider: " + lang + ":" + time;
    }

}