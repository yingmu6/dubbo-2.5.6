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
import com.alibaba.dubbo.config.ModuleConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.spring.extension.SpringExtensionFactory;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ServiceFactoryBean
 *
 * history-new InitializingBean, DisposableBean,
 * ApplicationContextAware, ApplicationListener, BeanNameAware 待了解
 *
 * @author william.liangf
 * @export
 */
public class ServiceBean<T> extends ServiceConfig<T> implements InitializingBean, DisposableBean, ApplicationContextAware, ApplicationListener, BeanNameAware {

    private static final long serialVersionUID = 213195494150089726L;

    private static transient ApplicationContext SPRING_CONTEXT;

    private transient ApplicationContext applicationContext;

    private transient String beanName;

    private transient boolean supportedApplicationListener;

    public ServiceBean() {
        super();
    }

    public ServiceBean(Service service) {
        super(service);
    }

    public static ApplicationContext getSpringContext() {
        return SPRING_CONTEXT;
    }

    /**
     * 设置ApplicationContext
     * 1）将传入的值applicationContext赋值给当前成员变量
     * 2）将应用上下文添加到集合中
     * 3）若applicationContext不为空
     *  3.1）赋值给上下文SPRING_CONTEXT
     *  3.2）兼容Spring2.0.1（添加监听器）
     *   3.2.1）获取"addApplicationListener"对应的方法Method
     *   3.2.2）调用applicationContext的"addApplicationListener"方法将当前对象加入
     *   3.2.3）supportedApplicationListener置为true
     *   3.2.4）若出现异常，且applicationContext是AbstractApplicationContext的实例
     *     3.2.4.1）获取"addListener"对应的Method，若不能访问，则变更访问权限
     *     3.2.4.2）调用applicationContext的"addListener"方法将当前对象加入
     *     3.2.4.3）supportedApplicationListener置为true
     *
     */
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        SpringExtensionFactory.addApplicationContext(applicationContext);
        if (applicationContext != null) {
            SPRING_CONTEXT = applicationContext;
            try {
                Method method = applicationContext.getClass().getMethod("addApplicationListener", new Class<?>[]{ApplicationListener.class}); // 兼容Spring2.0.1
                method.invoke(applicationContext, new Object[]{this});
                supportedApplicationListener = true;
            } catch (Throwable t) {
                if (applicationContext instanceof AbstractApplicationContext) {
                    try {
                        Method method = AbstractApplicationContext.class.getDeclaredMethod("addListener", new Class<?>[]{ApplicationListener.class}); // 兼容Spring2.0.1
                        if (!method.isAccessible()) {
                            method.setAccessible(true);
                        }
                        method.invoke(applicationContext, new Object[]{this});
                        supportedApplicationListener = true;
                    } catch (Throwable t2) {
                    }
                }
            }
        }
        /**
         * 问题点：history-new
         * 1）spring 2.0.1中的addApplicationListener、addListener了解
         */
    }

    public void setBeanName(String name) {
        this.beanName = name;
    }

    /**
     * 启动spring时，注册URL  核心流程
     * @csy-finish 此处Spring实践回调待调试理解
     *
     * 若ContextRefreshedEvent、ApplicationEvent的类名相同
     * 则判断事件的类名是否相同
     * 若未延迟、未暴露、未取消暴露 则暴露服务
     */
    public void onApplicationEvent(ApplicationEvent event) {
        if (ContextRefreshedEvent.class.getName().equals(event.getClass().getName())) {
            if (isDelay() && !isExported() && !isUnexported()) {
                if (logger.isInfoEnabled()) {
                    logger.info("The service ready on spring started. service: " + getInterface());
                }
                export();
            }
        }
        /**
         * 问题点：history-new
         * 1）ContextRefreshedEvent与ApplicationEvent有何不同？
         * 2）此处待调试
         */
    }

    /**
     * 判断是否延迟
     * 1）从父类中获取延迟时间以及提供者配置
     * 2）若延迟时间为空，提供者配置不为空，则从ProviderConfig获取delay时间
     * 3）根据是否支持以及delay的值判断是否延迟
     */
    private boolean isDelay() {
        Integer delay = getDelay();
        ProviderConfig provider = getProvider();
        if (delay == null && provider != null) {
            delay = provider.getDelay();
        }
        return supportedApplicationListener && (delay == null || delay == -1);
    }

    /**
     * ServiceBean 属性设置后处理（若不需要延迟，会调用ServiceConfig的export()暴露服务）
     * 1）若ProviderConfig配置为空（设置成员属性List<ProtocolConfig>或ProviderConfig的值）
     *  1.1）判断applicationContext是否为空，若不为空获取ProviderConfig对应的Map
     *  1.2）判断protocolConfigMap的值
     *    1.2.1）若protocolConfigMap为空且providerConfigMap元素数目大于1
     *      1.2.1.1）遍历providerConfigMap的值列表
     *      1.2.1.2）若ProviderConfig中isDefault不为空且为true，则加到ProviderConfig列表中
     *      1.2.1.3）若ProviderConfig列表不为空，将provider列表转换为Protocol列表，并设置到当前成员变量中
     *    1.2.2）若protocolConfigMap不为空或providerConfigMap元素数目小于等于1
     *      1.2.2.1）遍历providerConfigMap的值列表
     *      1.2.2.2）若ProviderConfig中isDefault为空或值为true
     *               若providerConfig不为空，则抛出异常"重复的provider配置"
     *      1.2.2.3）单个设置ProviderConfig
     * 2）若ApplicationConfig配置为空 且（ProviderConfig为空或从ProviderConfig获取的应用配置为空）
     *   2.1）判断applicationContext是否为空，若不为空获取ModuleConfig对应的Map
     *   2.2）遍历moduleConfigMap的值
     *     2.2.1）若ModuleConfig中的isDefault为空或值为true
     *            若moduleConfig不为空，则抛出异常"重复的module配置"，将遍历的config进行赋值
     *     2.2.2）若ModuleConfig不为空，则设置模块信息ModuleConfig
     * 3）若注册中心List<RegistryConfig>为空 且ProviderConfig中的注册列表 且ApplicationConfig的注册列表都为空
     *   3.1）判断applicationContext是否为空，若不为空获取RegistryConfig对应的Map
     *   3.2）遍历registryConfigMap的值
     *     3.2.1）若RegistryConfig中的isDefault为空或值为true
     *            将RegistryConfig添加到注册列表中（允许多注册中心）
     *     3.2.2）若注册列表不为空，则赋值给父类AbstractInterfaceConfig的registries
     * 4）若MonitorConfig配置为空 且ProviderConfig、ApplicationConfig中的MonitorConfig都为空
     *   4.1）判断applicationContext是否为空，若不为空获取MonitorConfig对应的Map
     *   4.2）遍历monitorConfigMap的值
     *     4.2.1）若MonitorConfig中的isDefault为空或值为true
     *            若monitorConfig不为空，则抛出异常"重复的monitor配置"
     *     4.2.2）若MonitorConfig不为空，赋值给父类AbstractInterfaceConfig的MonitorConfig
     * 5）若协议列表List<ProtocolConfig>为空
     *   5.1）判断applicationContext是否为空，若不为空获取ProtocolConfig对应的Map
     *   5.2）遍历protocolConfigMap的值
     *     5.2.1）若ProtocolConfig中的isDefault为空或值为true
     *            将ProtocolConfig添加到协议列表protocolConfigs
     *     5.2.2）若protocolConfigs不为空，赋值给父类AbstractInterfaceConfig的protocolConfigs
     * 6）若path服务名称为空
     *   6.1）若beanName不为空且interfaceName（接口名称）不为空，并且bean的名称以接口名称开发
     *        则将beanName作为服务名称
     * 7）若服务不需要延迟，则直接暴露服务
     *
     */
    @SuppressWarnings({"unchecked", "deprecation"})
    public void afterPropertiesSet() throws Exception {
        if (getProvider() == null) {
            Map<String, ProviderConfig> providerConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ProviderConfig.class, false, false);
            if (providerConfigMap != null && providerConfigMap.size() > 0) {
                Map<String, ProtocolConfig> protocolConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ProtocolConfig.class, false, false);
                if ((protocolConfigMap == null || protocolConfigMap.size() == 0)
                        && providerConfigMap.size() > 1) { // 兼容旧版本
                    List<ProviderConfig> providerConfigs = new ArrayList<ProviderConfig>();
                    for (ProviderConfig config : providerConfigMap.values()) {
                        if (config.isDefault() != null && config.isDefault().booleanValue()) {
                            providerConfigs.add(config);
                        }
                    }
                    if (providerConfigs.size() > 0) {
                        setProviders(providerConfigs);
                    }
                } else {
                    ProviderConfig providerConfig = null;
                    for (ProviderConfig config : providerConfigMap.values()) {
                        if (config.isDefault() == null || config.isDefault().booleanValue()) {
                            if (providerConfig != null) {
                                throw new IllegalStateException("Duplicate provider configs: " + providerConfig + " and " + config);
                            }
                            providerConfig = config;
                        }
                    }
                    if (providerConfig != null) {
                        setProvider(providerConfig);
                    }
                }
            }
        }
        if (getApplication() == null
                && (getProvider() == null || getProvider().getApplication() == null)) {
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
                && (getProvider() == null || getProvider().getModule() == null)) {
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
                && (getProvider() == null || getProvider().getRegistries() == null || getProvider().getRegistries().size() == 0)
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
                && (getProvider() == null || getProvider().getMonitor() == null)
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
        if ((getProtocols() == null || getProtocols().size() == 0)
                && (getProvider() == null || getProvider().getProtocols() == null || getProvider().getProtocols().size() == 0)) {
            Map<String, ProtocolConfig> protocolConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ProtocolConfig.class, false, false);
            if (protocolConfigMap != null && protocolConfigMap.size() > 0) {
                List<ProtocolConfig> protocolConfigs = new ArrayList<ProtocolConfig>();
                for (ProtocolConfig config : protocolConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault().booleanValue()) {
                        protocolConfigs.add(config);
                    }
                }
                if (protocolConfigs != null && protocolConfigs.size() > 0) {
                    super.setProtocols(protocolConfigs);
                }
            }
        }
        if (getPath() == null || getPath().length() == 0) {
            if (beanName != null && beanName.length() > 0
                    && getInterface() != null && getInterface().length() > 0
                    && beanName.startsWith(getInterface())) {
                setPath(beanName);
            }
        }
        if (!isDelay()) {
            export();
        }

        /**
         * 问题集 history-new
         * 1）ProtocolConfig、ProviderConfig的区别？
         * 2）config.isDefault() == null || config.isDefault().booleanValue()此处判断逻辑比较绕，待调试理解
         *
         */
    }

    public void destroy() throws Exception {
        unexport();
    }

}