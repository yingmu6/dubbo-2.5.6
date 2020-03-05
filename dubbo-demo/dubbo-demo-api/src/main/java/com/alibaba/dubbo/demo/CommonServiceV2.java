package com.alibaba.dubbo.demo;

import java.util.List;
import java.util.Map;

/**
 * 常用功能点
 * @author chensy
 * @date 2019-11-07 17:51
 */
public interface CommonServiceV2 {
    String sayMulHello(String lang, String desc);
    String sayMulHelloV2(String lang, Map<String, String> map, List<String> list);
    String sayMulHelloV3(String lang, Map<String, Person> map);

}
