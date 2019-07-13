package com.alibaba.dubbo.demo.provider.self.generic;

import com.alibaba.dubbo.demo.Person;
import com.alibaba.dubbo.rpc.service.GenericException;
import com.alibaba.dubbo.rpc.service.GenericService;

import java.util.Map;

/**
 * @author chensy
 * @date 2019-07-09 20:00
 */
public class SecondGenericService implements GenericService {
    @Override
    public Object $invoke(String method, String[] parameterTypes, Object[] args) throws GenericException {
        Object result = " 20884 应用";
//        if ("com.alibaba.dubbo.demo.Person".equals(parameterTypes[0])) { //
//            Map<String, Object> paramMap =  (Map<String, Object> )args[0];
//            Person person = new Person();
//            person.setName("second:" + paramMap.get("name"));
//            person.setSex((Integer) paramMap.get("sex"));
//            result = person;
//        }
        return result;
    }
}
