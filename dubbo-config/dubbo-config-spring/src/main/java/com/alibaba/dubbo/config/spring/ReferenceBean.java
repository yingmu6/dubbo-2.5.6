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
package com.alibaba.dubbo.config.spring;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.ModuleConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.extension.SpringExtensionFactory;
import com.alibaba.dubbo.config.support.Parameter;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ReferenceFactoryBean
 *
 * @author william.liangf
 * @export
 */
public class ReferenceBean<T> extends ReferenceConfig<T> implements FactoryBean, ApplicationContextAware, InitializingBean, DisposableBean {

    private static final long serialVersionUID = 213195494150089726L;

    private transient ApplicationContext applicationContext;

    public ReferenceBean() {
        super();
    }

    public ReferenceBean(Reference reference) {
        super(reference);
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        SpringExtensionFactory.addApplicationContext(applicationContext);
    }

    /**
     * 引用对象
     */
    public Object getObject() throws Exception {
        return get();
    }

    /**
     * 获取对象类型
     */
    public Class<?> getObjectType() {
        return getInterfaceClass();
    }

    @Parameter(excluded = true)
    public boolean isSingleton() {
        return true;
    }

    /**
     * ReferenceBean 属性设置后处理（若立即初始化，会调用ReferenceConfig的get()获取引用对象）
     * 1）若ConsumerConfig为空
     *  1.1）判断applicationContext是否为空，若不为空获取ConsumerConfig对应的Map
     *  1.2）遍历consumerConfigMap的值
     *    1.2.1）若ConsumerConfig中isDefault为空或为true
     *           若consumerConfig不为空，则抛出"重复的consumer配置"，否则赋值给consumerConfig
     *    1.2.2）若consumerConfig不为空，则设置ReferenceConfig到ConsumerConfig属性
     * 2）若ApplicationConfig为空，且从ConsumerConfig获取的ApplicationConfig也为空
     *  2.1）判断applicationContext是否为空，若不为空获取ApplicationConfig对应的Map
     *  2.2）遍历applicationConfigMap
     *    2.2.1）若ApplicationConfig中isDefault为空或为true
     *           若ApplicationConfig不为空，则抛出"重复的application配置"，否则赋值给applicationConfig
     *    2.2.2）若applicationConfig不为空，则设置到AbstractInterfaceConfig的application
     * 3）若ModuleConfig为空，且从ConsumerConfig获取的ModuleConfig也为空
     *   3.1）判断applicationContext是否为空，若不为空获取ModuleConfig对应的Map
     *   3.2）遍历moduleConfigMap
     *     3.2.1）若ModuleConfig中isDefault为空或为true
     *            若ModuleConfig不为空，则抛出"重复的module配置"，否则赋值给moduleConfig
     *     3.2.2）若moduleConfig不为空，则设置到AbstractInterfaceConfig的module
     * 4）若List<RegistryConfig>为空，且从ConsumerConfig和ApplicationConfig中都没查到注册列表
     *   4.1）判断applicationContext是否为空，若不为空获取RegistryConfig对应的Map
     *   4.2）遍历registryConfigMap
     *     4.2.1）若RegistryConfig中isDefault为空或为true，则将RegistryConfig添加到注册列表中
     *     4.2.2）若注册列表不为空，则设置到AbstractInterfaceConfig中的注册列表
     * 5）若MonitorConfig为空，且从从ConsumerConfig和ApplicationConfig中都没查到MonitorConfig
     *   5.1）判断applicationContext是否为空，若不为空获取MonitorConfig对应的Map
     *   5.2）遍历monitorConfigMap
     *     5.2.1）若MonitorConfig中isDefault为空或为true
     *           若MonitorConfig不为空，则抛出"重复的monitor配置"，否则赋值给monitorConfig
     *     5.2.2）若moduleConfig不为空，则设置到AbstractInterfaceConfig的monitor
     * 6）获取加载初始化的值
     *   6.1）若置为空，ConsumerConfig不为空时，从ConsumerConfig拿到值
     *   6.2）若是加载时即刻初始化，则直接调用getObject()方法，引用对象
     */
    @SuppressWarnings({"unchecked"})
    public void afterPropertiesSet() throws Exception {
        if (getConsumer() == null) {/**@c 判断消费者是否为空，若为空则构建消费者 */
            Map<String, ConsumerConfig> consumerConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ConsumerConfig.class, false, false);
            if (consumerConfigMap != null && consumerConfigMap.size() > 0) {
                ConsumerConfig consumerConfig = null;
                for (ConsumerConfig config : consumerConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault().booleanValue()) {
                        if (consumerConfig != null) {
                            throw new IllegalStateException("Duplicate consumer configs: " + consumerConfig + " and " + config);
                        }
                        consumerConfig = config;
                    }
                }
                if (consumerConfig != null) {
                    setConsumer(consumerConfig);
                }
            }
        }
        if (getApplication() == null
                && (getConsumer() == null || getConsumer().getApplication() == null)) {
            Map<String, ApplicationConfig> applicationConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ApplicationConfig.class, false, false);
            if (applicationConfigMap != null && applicationConfigMap.size() > 0) {
                ApplicationConfig applicationConfig = null;
                for (ApplicationConfig config : applicationConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault().booleanValue()) {
                        if (applicationConfig != null) {
                            throw new IllegalStateException("Duplicate application configs: " + applicationConfig + " and " + config);
                        }
                        applicationConfig = config;
                    }
                }
                if (applicationConfig != null) {
                    setApplication(applicationConfig);
                }
            }
        }
        if (getModule() == null
                && (getConsumer() == null || getConsumer().getModule() == null)) {
            Map<String, ModuleConfig> moduleConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ModuleConfig.class, false, false);
            if (moduleConfigMap != null && moduleConfigMap.size() > 0) {
                ModuleConfig moduleConfig = null;
                for (ModuleConfig config : moduleConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault().booleanValue()) {
                        if (moduleConfig != null) {
                            throw new IllegalStateException("Duplicate module configs: " + moduleConfig + " and " + config);
                        }
                        moduleConfig = config;
                    }
                }
                if (moduleConfig != null) {
                    setModule(moduleConfig);
                }
            }
        }
        if ((getRegistries() == null || getRegistries().size() == 0)
                && (getConsumer() == null || getConsumer().getRegistries() == null || getConsumer().getRegistries().size() == 0)
                && (getApplication() == null || getApplication().getRegistries() == null || getApplication().getRegistries().size() == 0)) {
            Map<String, RegistryConfig> registryConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, RegistryConfig.class, false, false);
            if (registryConfigMap != null && registryConfigMap.size() > 0) {
                List<RegistryConfig> registryConfigs = new ArrayList<RegistryConfig>();
                for (RegistryConfig config : registryConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault().booleanValue()) {
                        registryConfigs.add(config);
                    }
                }
                if (registryConfigs != null && registryConfigs.size() > 0) {
                    super.setRegistries(registryConfigs);
                }
            }
        }
        if (getMonitor() == null
                && (getConsumer() == null || getConsumer().getMonitor() == null)
                && (getApplication() == null || getApplication().getMonitor() == null)) {
            Map<String, MonitorConfig> monitorConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, MonitorConfig.class, false, false);
            if (monitorConfigMap != null && monitorConfigMap.size() > 0) {
                MonitorConfig monitorConfig = null;
                for (MonitorConfig config : monitorConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault().booleanValue()) {
                        if (monitorConfig != null) {
                            throw new IllegalStateException("Duplicate monitor configs: " + monitorConfig + " and " + config);
                        }
                        monitorConfig = config;
                    }
                }
                if (monitorConfig != null) {
                    setMonitor(monitorConfig);
                }
            }
        }
        Boolean b = isInit();
        if (b == null && getConsumer() != null) {
            b = getConsumer().isInit();
        }
        if (b != null && b.booleanValue()) {
            getObject();
        }
    }

}