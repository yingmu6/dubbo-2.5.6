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
package com.alibaba.dubbo.config.spring.schema;

import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.ModuleConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.spring.AnnotationBean;
import com.alibaba.dubbo.config.spring.ReferenceBean;
import com.alibaba.dubbo.config.spring.ServiceBean;

import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * DubboNamespaceHandler
 *
 * @author william.liangf
 * @export
 */
public class DubboNamespaceHandler extends NamespaceHandlerSupport { //todo @csy DubboNamespaceHandler什么时候被调用？

    /**
     * 静态块，再类加载时就被执行的
     */
    static {
        Version.checkDuplicate(DubboNamespaceHandler.class);
    }

    /**
     * 子类调用父类的registerBeanDefinitionParser()方法，对DubboBeanDefinitionParser进行注册
     *   将元素名与对应的DubboBeanDefinitionParser映射起来，存入Map<String, BeanDefinitionParser>
     *
     * 重写父类的init()方法
     * Invoked by the {@link DefaultBeanDefinitionDocumentReader} after
     * construction but before any custom（自定义） elements are parsed
     * （在构造DefaultBeanDefinitionDocumentReader以后，自定义元素被解析前，调用初始化方法init()）
     */
    public void init() {
        registerBeanDefinitionParser("application", new DubboBeanDefinitionParser(ApplicationConfig.class, true));
        registerBeanDefinitionParser("module", new DubboBeanDefinitionParser(ModuleConfig.class, true));
        registerBeanDefinitionParser("registry", new DubboBeanDefinitionParser(RegistryConfig.class, true));
        registerBeanDefinitionParser("monitor", new DubboBeanDefinitionParser(MonitorConfig.class, true));
        registerBeanDefinitionParser("provider", new DubboBeanDefinitionParser(ProviderConfig.class, true));
        registerBeanDefinitionParser("consumer", new DubboBeanDefinitionParser(ConsumerConfig.class, true));
        registerBeanDefinitionParser("protocol", new DubboBeanDefinitionParser(ProtocolConfig.class, true));
        registerBeanDefinitionParser("service", new DubboBeanDefinitionParser(ServiceBean.class, true));
        registerBeanDefinitionParser("reference", new DubboBeanDefinitionParser(ReferenceBean.class, false)); //非必须
        registerBeanDefinitionParser("annotation", new DubboBeanDefinitionParser(AnnotationBean.class, true));
    }

    /**
     * BeanDefinitionDocumentReader（Bean）
     *
     * SPI for parsing an XML document that contains Spring bean definitions.
     *  （用于解析含有spring bean的XML文挡）
     * Used by {@link XmlBeanDefinitionReader} for actually parsing a DOM document
     *  （用于解析DOM 文挡）
     *
     * XmlBeanDefinitionReader：（为XML的Bean定义阅读器）
     * Bean definition reader for XML bean definitions.
     * Delegates the actual XML document reading to an implementation
     * of the {@link BeanDefinitionDocumentReader} interface.
     */

}