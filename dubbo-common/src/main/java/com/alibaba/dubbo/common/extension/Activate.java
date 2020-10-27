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
 * 用户通过group和value配置激活条件,被activate注解的扩展点在满足某种条件时会被激活（被创建）
 * https://www.jianshu.com/p/7e116f480165
 *
 * Activate
 * <p/>
 * 对于可以被框架中自动激活加载扩展，此Annotation用于配置扩展被自动激活加载条件。
 * 比如，过滤扩展，有多个实现，使用Activate Annotation的扩展可以根据条件被自动加载。
 * <ol>
 * <li>{@link Activate#group()}生效的Group。具体的有哪些Group值由框架SPI给出。
 * <li>{@link Activate#value()}在{@link com.alibaba.dubbo.common.URL}中Key集合中有，则生效。
 * </ol>
 * <p>
 * <p/>
 * 底层框架SPI提供者通过{@link com.alibaba.dubbo.common.extension.ExtensionLoader}的{@link ExtensionLoader#getActivateExtension}方法
 * 获得条件的扩展。
 *
 * @author william.liangf
 * @author ding.lid
 * @export
 * @see SPI
 * @see ExtensionLoader
 * @see ExtensionLoader#getActivateExtension(com.alibaba.dubbo.common.URL, String[], String)
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Activate { //以group、value作为过滤条件，查找实例类，不需要用配置文件的key  @pause 4.1 自动激活流程待深入了解？SPI机制待深入
    /**
     * Group过滤条件。
     * <br />
     * 包含{@link ExtensionLoader#getActivateExtension}的group参数给的值，则返回扩展。
     * <br />
     * 如没有Group设置，则不过滤。
     */
    String[] group() default {};

    /**
     * Key过滤条件。包含{@link ExtensionLoader#getActivateExtension}的URL的参数Key中有，则返回扩展。
     * <p/>
     * 示例：<br/>
     * 注解的值 <code>@Activate("cache,validatioin")</code>，
     * 则{@link ExtensionLoader#getActivateExtension}的URL的参数有<code>cache</code>Key，或是<code>validatioin</code>则返回扩展。
     * <br/>
     * 如没有设置，则不过滤。
     */
    String[] value() default {};

    /**
     * 排序信息，可以不提供。
     */
    String[] before() default {};

    /**
     * 排序信息，可以不提供。
     */
    String[] after() default {};

    /**
     * 排序信息，可以不提供。
     */
    int order() default 0;
}