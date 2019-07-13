package com.alibaba.dubbo.demo.provider.self.generic;

import com.alibaba.dubbo.rpc.service.GenericException;
import com.alibaba.dubbo.rpc.service.GenericService;

/**
 * 实现泛化调用 :
 * 泛接口实现方式主要用于服务器端没有API接口及模型类元的情况，参数及返回值中的所有POJO均用Map表示，通常用于框架集成
 * ，比如：实现一个通用的远程服务Mock框架，可通过实现GenericService接口处理所有服务请求
 *
 * @author chensy
 * @date 2019-07-09 18:06
 */
public class MyGenericService implements GenericService {
    @Override
    public Object $invoke(String method, String[] parameterTypes, Object[] args) throws GenericException {
        System.out.println("method: " + method + ",param:" + parameterTypes + ",args:" + args);

        Object result = new Object();
        if ("sayHi".equals(method)) {
            if ("java.lang.String".equals(parameterTypes[0])) {
                result = "provider str:" + args[0];
            } else if ("java.lang.Integer".equals(parameterTypes[1])) {
                result = "provider int:" + args[1];
            }
        } else if ("welcome".equals(method)) {
            result = "welcome";
        }
        result = result + " 20883 应用";
        return result;

    }
}
