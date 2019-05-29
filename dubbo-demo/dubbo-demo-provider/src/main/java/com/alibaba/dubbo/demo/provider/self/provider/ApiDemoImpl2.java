package com.alibaba.dubbo.demo.provider.self.provider;

import com.alibaba.dubbo.demo.ApiDemo2;

/**
 * @author chensy
 * @date 2019-05-29 13:47
 */
public class ApiDemoImpl2 implements ApiDemo2 {
    @Override
    public String sayApi2(String str) {
       return "API2 " + str;
    }
}
