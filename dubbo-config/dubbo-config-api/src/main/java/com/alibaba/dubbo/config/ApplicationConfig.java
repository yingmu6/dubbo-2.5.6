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
package com.alibaba.dubbo.config;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.compiler.support.AdaptiveCompiler;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.config.support.Parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * ApplicationConfig 继承了AbstractConfig出构造方法外的非private的变量和方法
 * Debug时，this对象会显示toString()的内容，AbstractConfig对toString进行了重写，会输出xml标签的配置信息
 * @author william.liangf
 * @export
 */
public class ApplicationConfig extends AbstractConfig {

    private static final long serialVersionUID = 5508512956753757169L;

    // 应用名称
    private String name;

    // 模块版本
    private String version; //todo @csy 10/01 若提供者指定了版本等约束条件，消费者是怎么校验处理的

    // 应用负责人
    private String owner;

    // 组织名(BU或部门)
    private String organization;

    // 分层
    private String architecture;

    // 环境，如：dev/test/run
    private String environment; //todo @csy 10/01 探究下项目环境是否用这个字段标识的？

    // Java代码编译器
    private String compiler;

    // 日志输出方式
    private String logger;

    // 注册中心（从数据结构可以看出，一个应用对应多个注册中心，一个监控中心）
    private List<RegistryConfig> registries;

    // 服务监控
    private MonitorConfig monitor;

    // 是否为缺省
    private Boolean isDefault;

    // test
    private String testAppConfigOut;

    public String getTestAppConfigOut() {
        return testAppConfigOut;
    }

    public void setTestAppConfigOut(String testAppConfigOut) {
        this.testAppConfigOut = testAppConfigOut;
    }

    public ApplicationConfig() {
    }

    public ApplicationConfig(String name) {
        setName(name);
    }

    //origin  @Parameter(key = Constants.APPLICATION_KEY, required = true)
    @Parameter(key = Constants.APPLICATION_KEY, escaped = true, append = true, required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        checkName("name", name); //检查命名格式是否正确
        this.name = name;
        if (id == null || id.length() == 0) {
            id = name;
        }
    }

    @Parameter(key = "application.version")
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        checkMultiName("owner", owner);
        this.owner = owner;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        checkName("organization", organization);
        this.organization = organization;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        checkName("architecture", architecture);
        this.architecture = architecture;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        checkName("environment", environment);
        if (environment != null) {
            if (!("develop".equals(environment) || "test".equals(environment) || "product".equals(environment))) {
                throw new IllegalStateException("Unsupported environment: " + environment + ", only support develop/test/product, default is product.");
            }
        }
        this.environment = environment;
    }

    public RegistryConfig getRegistry() {
        return registries == null || registries.size() == 0 ? null : registries.get(0);
    }

    public void setRegistry(RegistryConfig registry) {
        List<RegistryConfig> registries = new ArrayList<RegistryConfig>(1);
        registries.add(registry);
        this.registries = registries;
    }

    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    @SuppressWarnings({"unchecked"})
    public void setRegistries(List<? extends RegistryConfig> registries) {
        this.registries = (List<RegistryConfig>) registries;
    }

    public MonitorConfig getMonitor() {
        return monitor;
    }

    public void setMonitor(String monitor) {
        this.monitor = new MonitorConfig(monitor);
    }

    public void setMonitor(MonitorConfig monitor) {
        this.monitor = monitor;
    }

    public String getCompiler() {
        return compiler;
    }

    public void setCompiler(String compiler) {
        this.compiler = compiler;
        AdaptiveCompiler.setDefaultCompiler(compiler);
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
        LoggerFactory.setLoggerAdapter(logger);
    }

    public Boolean isDefault() {
        return isDefault;
    }

    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    /**@c 测试appendProperties 该方法是protected 访问权限只能在同一个包进行*/
    public void testAppendProperties(AbstractConfig abstractConfig) {
        appendProperties(abstractConfig);
    }

    public void testAppendParameter(Map<String, String> parameters, Object config, String prefix) {
        appendParameters(parameters, config, prefix);
    }

    public void testAppendAnnotation(Class<?> annotationClass, Object annotation) {
        appendAnnotationOrigin(annotationClass, annotation);
    }

    /**@c 重新方法 */
    public void testAppendAnnotationSelf(Class<?> annotationClass, Object annotation) {
        appendAnnotation(annotationClass, annotation);
    }
}