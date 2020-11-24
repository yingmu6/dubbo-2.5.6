package com.alibaba.dubbo.demo;

import java.lang.annotation.*;

/**
 * 测试注解
 * @author
 * @date
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface TestLog {
    String log() default "log";
}
