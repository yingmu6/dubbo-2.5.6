package com.alibaba.dubbo.demo.provider.self.config;

import com.alibaba.dubbo.config.annotation.Service;

/**
 * @author chensy
 * @date 2019-06-10 18:53
 */
@Service(testAppConfigOut = "we22")
public class AnnotationSelfImpl {
    private String name;
    private Integer age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
