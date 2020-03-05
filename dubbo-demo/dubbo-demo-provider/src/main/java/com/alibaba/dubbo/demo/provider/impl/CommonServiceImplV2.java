package com.alibaba.dubbo.demo.provider.impl;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.demo.CommonServiceV2;
import com.alibaba.dubbo.demo.Person;

import java.util.List;
import java.util.Map;

/**
 * @author chensy
 * @date 2019-11-07 17:55
 */
public class CommonServiceImplV2 implements CommonServiceV2 {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    // 上下文信息处理
    @Override
    public String sayMulHello(String lang, String desc) {
        if ("zh".equals(lang)) {
            return "V中文：12你好！" + desc;
        } else {
            return "V英文：12hello！" + desc;
        }
    }

    @Override
    public String sayMulHelloV2(String lang, Map<String, String> map, List<String> list) {
        if ("zh".equals(lang)) {
            return "V2中文：12你好！" + map + ";" + list;
        } else {
            return "V2英文：12hello！" + map + ";" + list;
        }
    }

    @Override
    public String sayMulHelloV3(String lang, Map<String, Person> map) {
        if ("zh".equals(lang)) {
            return "V3中文：12你好！" + map + ";";
        } else {
            return "V3英文：12hello！" + map + ";";
        }
    }
}
