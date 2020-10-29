/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.extension;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 扩展点接口的标识。（需要扩展的都要带上SPI注解）
 * <p/>
 * 扩展点声明配置文件，格式修改。<br />
 * 以Protocol示例，配置文件META-INF/dubbo/com.xxx.Protocol内容：<br />
 * 由<br/>
 * <pre><code>com.foo.XxxProtocol
 * com.foo.YyyProtocol</code></pre><br/>
 * 改成使用KV格式<br/>
 * <pre><code>xxx=com.foo.XxxProtocol
 * yyy=com.foo.YyyProtocol
 * </code></pre>
 * <br/>
 * 原因：<br/>
 * 当扩展点的static字段或方法签名上引用了三方库，
 * 如果三方库不存在，会导致类初始化失败，
 * Extension标识Dubbo就拿不到了，异常信息就和配置对应不起来。
 * <br/>
 * 比如:
 * Extension("mina")加载失败，
 * 当用户配置使用mina时，就会报找不到扩展点，
 * 而不是报加载扩展点失败，以及失败原因。
 *
 * @author william.liangf
 * @author ding.lid
 * @export
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE}) //用在类、接口上
public @interface SPI {//表示可以通过SPI的方式，目的是获取接口的实现类（SPI是服务动态发现的机制）
    /**
     * SPI 全称为 (Service Provider Interface) ，是JDK内置的一种服务提供发现机制。SPI是一种动态替换发现的机制，
     * 比如有个接口，想运行时动态的给它添加实现，你只需要添加一个实现
     */

    //在指定目录下配置文件，当实例类时就会在文件中就找
    //为啥用SPI，不可以写个子类继承吗？SPI到底用来做啥？
    //解：SPI 服务提供接口，可以通过配置文件动态指定接口的实现类
    /**
     * 缺省扩展点名。（只能有一个扩展名，比如netty，不能有多个用逗号分隔）
     */
    String value() default "";
    /**
     * @csy-v2 注解了解学习
     * https://juejin.im/post/5b45bd715188251b3a1db54f
     * 注解的本质就是一个继承了Annotation接口的接口,可以去反编译任意一个注解类，就会得到结果的
     * public interface Override extends Annotation{}
     *
     * 随着项目越来越庞大，XML的内容也越来越复杂，维护成本变高
     * 注解可以提供更大的便捷性，易于维护修改，但耦合度高，而XML相对于注解则是相反的
     *
     * 『元注解』是用于修饰注解的注解，通常用在注解的定义上。一般用于指定某个注解生命周期以及作用目标等信息。比如@Target，@Retention
     * 注解与反射：通过反射获取到注解的值
     */
}