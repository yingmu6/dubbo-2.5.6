/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.config.spring.extension;

import com.alibaba.dubbo.common.extension.ExtensionFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;

import org.springframework.context.ApplicationContext;

import java.util.Set;

/**
 * SpringExtensionFactory
 *
 * @author william.liangf
 */
public class SpringExtensionFactory implements ExtensionFactory { //从 Spring 的 IOC 容器中获取所需的拓展

    private static final Set<ApplicationContext> contexts = new ConcurrentHashSet<ApplicationContext>();

    /**
     * 添加应用上下文到集合中
     */
    public static void addApplicationContext(ApplicationContext context) {
        contexts.add(context);
    }

    /**
     * 移除应用上下文
     */
    public static void removeApplicationContext(ApplicationContext context) {
        contexts.remove(context);
    }

    /**
     * 获取指定类型type、指定name的bean实例
     * 1）遍历应用上下文集合，若包含指定的name，获取name对应的实例
     * 2）若实例的类型是指定的type，则进行强转并返回
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtension(Class<T> type, String name) {
        for (ApplicationContext context : contexts) {/**@c 从spring bean中获取实例 */
            if (context.containsBean(name)) { //判断是否包含指定名称的bean
                Object bean = context.getBean(name);
                if (type.isInstance(bean)) { //若包含，再看下bean的类型
                    return (T) bean;
                }
            }
        }
        return null;
    }

}
