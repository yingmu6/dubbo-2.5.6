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
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.support.Parameter;
import com.alibaba.dubbo.monitor.MonitorFactory;
import com.alibaba.dubbo.monitor.MonitorService;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryFactory;
import com.alibaba.dubbo.registry.RegistryService;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.InvokerListener;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.support.MockInvoker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AbstractDefaultConfig
 *
 * @author william.liangf
 * @export
 */
public abstract class AbstractInterfaceConfig extends AbstractMethodConfig {

    private static final long serialVersionUID = -1559314110797223229L;

    // 服务接口的本地实现类名
    protected String local;

    // 服务接口的本地实现类名
    protected String stub;

    // 服务监控
    protected MonitorConfig monitor;

    // 代理类型
    protected String proxy;

    // 集群方式
    protected String cluster;

    // 过滤器
    protected String filter;

    // 监听器
    protected String listener;

    // 负责人
    protected String owner;

    // 连接数限制,0表示共享连接，否则为该服务独享连接数
    protected Integer connections;

    // 连接数限制
    protected String layer;

    // 应用信息
    protected ApplicationConfig application;

    // 模块信息
    protected ModuleConfig module;

    // 注册中心
    protected List<RegistryConfig> registries;
    // 连接事件
    protected String onconnect;
    // 断开事件
    protected String ondisconnect;
    // callback实例个数限制
    private Integer callbacks;
    // 服务暴露或引用的scope,如果为local，则表示只在当前JVM内查找.
    private String scope;

    /**
     * 检测注册中心
     * 1）若注册地址列表为空，则尝试从系统属性或属性文件中查找
     *    获取到地址，用竖线分隔地址，构建RegistryConfig，设置地址，并加到注册实例列表
     * 2）若注册实例还是为空，则抛出异常
     * 3）
     */
    protected void checkRegistry() { //检测注册中心的配置，并设置属性值
        // 兼容旧版本
        if (registries == null || registries.size() == 0) { //注册配置列表为空时，从系统属性中查找
            String address = ConfigUtils.getProperty("dubbo.registry.address");
            if (address != null && address.length() > 0) {
                registries = new ArrayList<RegistryConfig>();
                String[] as = address.split("\\s*[|]+\\s*");/**@c 以竖线分隔，前后都可以有空格 */
                for (String a : as) {
                    RegistryConfig registryConfig = new RegistryConfig();
                    registryConfig.setAddress(a);
                    registries.add(registryConfig);
                }
            }
        }
        if ((registries == null || registries.size() == 0)) {
            throw new IllegalStateException((getClass().getSimpleName().startsWith("Reference")
                    ? "No such any registry to refer service in consumer "
                    : "No such any registry to export service in provider ")
                    + NetUtils.getLocalHost()
                    + " use dubbo version "
                    + Version.getVersion()
                    + ", Please add <dubbo:registry address=\"...\" /> to your spring config. If you want unregister, please set <dubbo:service registry=\"N/A\" />");
        }
        for (RegistryConfig registryConfig : registries) {
            appendProperties(registryConfig);
        }
    }

    /**
     * 检查应用信息
     * 1）检查应用名，应用名不为空
     * 2）对应ApplicationConfig配置对象设置属性
     * 3）从系统中或配置文件中获取优雅停机时间，并设置到系统属性中
     */
    @SuppressWarnings("deprecation")
    protected void checkApplication() {
        // 兼容旧版本
        if (application == null) {
            String applicationName = ConfigUtils.getProperty("dubbo.application.name");
            if (applicationName != null && applicationName.length() > 0) {
                application = new ApplicationConfig(); //todo @csy-h3 此处为啥不把应用名设置进入
            }
        }
        if (application == null) {/**@c Application 应用配置不能为空 */
            throw new IllegalStateException( //todo @csy-h3 为啥此处的异常没有抛出来？
                    "No such application config! Please add <dubbo:application name=\"...\" /> to your spring config.");
        }
        appendProperties(application);

        //从配置文件中获取优雅停机的时间，并且设置到系统属性中
        String wait = ConfigUtils.getProperty(Constants.SHUTDOWN_WAIT_KEY);/**@ 优雅停机的等待时间 */
        if (wait != null && wait.trim().length() > 0) {
            System.setProperty(Constants.SHUTDOWN_WAIT_KEY, wait.trim());
        } else {
            wait = ConfigUtils.getProperty(Constants.SHUTDOWN_WAIT_SECONDS_KEY);
            if (wait != null && wait.trim().length() > 0) {
                System.setProperty(Constants.SHUTDOWN_WAIT_SECONDS_KEY, wait.trim());
            }
        }
    }

    /**@c 加载注册配置map，并把参数附加到URL中*/
    protected List<URL> loadRegistries(boolean provider) { //service export 步骤04  provider是否是提供者
        checkRegistry();
        List<URL> registryList = new ArrayList<URL>();
        if (registries != null && registries.size() > 0) {
            for (RegistryConfig config : registries) {
                String address = config.getAddress();
                if (address == null || address.length() == 0) {
                    address = Constants.ANYHOST_VALUE; //取服务器端任意IPV4地址
                }
                String sysaddress = System.getProperty("dubbo.registry.address");
                if (sysaddress != null && sysaddress.length() > 0) { //若系统属性中存在注册地址，优先使用系统属性的值
                    address = sysaddress;
                }
                if (address != null && address.length() > 0
                        && !RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(address)) {
                    Map<String, String> map = new HashMap<String, String>();
                    appendParameters(map, application); //将配置对象的属性设置的url参数中
                    appendParameters(map, config);
                    map.put("path", RegistryService.class.getName());
                    map.put("dubbo", Version.getVersion());
                    map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
                    if (ConfigUtils.getPid() > 0) { //获取进程的pid
                        map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
                    }
                    if (!map.containsKey("protocol")) {
                        if (ExtensionLoader.getExtensionLoader(RegistryFactory.class).hasExtension("remote")) {
                            map.put("protocol", "remote");
                        } else {
                            map.put("protocol", "dubbo");
                        }
                    }
                    List<URL> urls = UrlUtils.parseURLs(address, map);
                    //此处生成的url： zookeeper://localhost:2181/com.alibaba.dubbo.registry.RegistryService?application=api_demo&dubbo=2.0.0&pid=36916&timestamp=1562032607782
                    for (URL url : urls) {
                        url = url.addParameter(Constants.REGISTRY_KEY, url.getProtocol());
                        url = url.setProtocol(Constants.REGISTRY_PROTOCOL);
                        if ((provider && url.getParameter(Constants.REGISTER_KEY, true)) //提供者
                                || (!provider && url.getParameter(Constants.SUBSCRIBE_KEY, true))) { //消费者
                            registryList.add(url);
                        }
                    }
                }
            }
        }
        //此处Registry的url： registry://localhost:2181/com.alibaba.dubbo.registry.RegistryService?application=api_demo&dubbo=2.0.0&pid=36972&registry=zookeeper&timestamp=1562032865696
        return registryList;
    }

    protected URL loadMonitor(URL registryURL) { //service export 步骤06 todo @csy-h3 dubbo监控界面怎么搭建使用？
        if (monitor == null) {
            String monitorAddress = ConfigUtils.getProperty("dubbo.monitor.address");
            String monitorProtocol = ConfigUtils.getProperty("dubbo.monitor.protocol");
            if (monitorAddress != null && monitorAddress.length() > 0
                    || monitorProtocol != null && monitorProtocol.length() > 0) {
                monitor = new MonitorConfig();
            } else {
                return null;
            }
        }
        appendProperties(monitor);
        Map<String, String> map = new HashMap<String, String>();
        map.put(Constants.INTERFACE_KEY, MonitorService.class.getName());
        map.put("dubbo", Version.getVersion());
        map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
        if (ConfigUtils.getPid() > 0) {
            map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
        }
        appendParameters(map, monitor);
        String address = monitor.getAddress();
        String sysaddress = System.getProperty("dubbo.monitor.address");
        if (sysaddress != null && sysaddress.length() > 0) {
            address = sysaddress;
        }
        if (ConfigUtils.isNotEmpty(address)) {
            if (!map.containsKey(Constants.PROTOCOL_KEY)) {
                if (ExtensionLoader.getExtensionLoader(MonitorFactory.class).hasExtension("logstat")) {
                    map.put(Constants.PROTOCOL_KEY, "logstat");
                } else {
                    map.put(Constants.PROTOCOL_KEY, "dubbo");
                }
            }
            return UrlUtils.parseURL(address, map);
        } else if (Constants.REGISTRY_PROTOCOL.equals(monitor.getProtocol()) && registryURL != null) {
            return registryURL.setProtocol("dubbo").addParameter(Constants.PROTOCOL_KEY, "registry").addParameterAndEncoded(Constants.REFER_KEY, StringUtils.toQueryString(map));
        }
        return null;
    }

    //判断接口中是否存在方法列表，若包含接口中没有的方法，则抛出异常
    protected void checkInterfaceAndMethods(Class<?> interfaceClass, List<MethodConfig> methods) {
        // 接口不能为空
        if (interfaceClass == null) {
            throw new IllegalStateException("interface not allow null!");
        }
        // 检查接口类型必需为接口
        if (!interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        // 检查方法是否在接口中存在（若没设置就不比较）
        if (methods != null && methods.size() > 0) {/**@c 将ServiceConfig中声明的方法与接口中实际的方法比较  */
            for (MethodConfig methodBean : methods) {
                String methodName = methodBean.getName();
                if (methodName == null || methodName.length() == 0) {
                    throw new IllegalStateException("<dubbo:method> name attribute is required! Please check: <dubbo:service interface=\"" + interfaceClass.getName() + "\" ... ><dubbo:method name=\"\" ... /></<dubbo:reference>");
                }
                boolean hasMethod = false;
                for (java.lang.reflect.Method method : interfaceClass.getMethods()) {
                    if (method.getName().equals(methodName)) {
                        hasMethod = true;
                        break;
                    }
                }
                if (!hasMethod) {
                    throw new IllegalStateException("The interface " + interfaceClass.getName()
                            + " not found method " + methodName);
                }
            }
        }
    }

    /**
     * 检查stub和mock
     * 1）若本地实现类名local不为空
     *  1.1）若local是默认值（"true"或"default"），则加载类名为interfaceClass.getName() + "Local"
     *       否则为ReflectUtils.forName(local)
     *  1.2）判断类interfaceClass是否是localClass的超类
     *  1.3）查找本地实现类的构造方法
     * 2）若本地存根stub不为空
     *  2.1）若local是默认值（"true"或"default"），则加载类名为interfaceClass.getName() + "Stub"
     *       否则为ReflectUtils.forName(local)
     *  1.2）判断类interfaceClass是否是localClass的超类
     *  1.3）查找stub的构造方法
     * 3）若测试mock不为空
     *  3.1）若mock值以return为前缀，截取"return "后的值
     *  3.2）解析Mock的值
     */
    protected void checkStubAndMock(Class<?> interfaceClass) {
        if (ConfigUtils.isNotEmpty(local)) {
            Class<?> localClass = ConfigUtils.isDefault(local) ? ReflectUtils.forName(interfaceClass.getName() + "Local") : ReflectUtils.forName(local);
            if (!interfaceClass.isAssignableFrom(localClass)) {/**@c 判断一个类Class1和另一个类Class2是否相同或是另一个类的超类或接口 */
                throw new IllegalStateException("The local implementation class " + localClass.getName() + " not implement interface " + interfaceClass.getName());
            }
            try {
                ReflectUtils.findConstructor(localClass, interfaceClass);/**@c 判断是否存在构造方法 */
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("No such constructor \"public " + localClass.getSimpleName() + "(" + interfaceClass.getName() + ")\" in local implementation class " + localClass.getName());
            }
        }
        if (ConfigUtils.isNotEmpty(stub)) { //在客户端实现部分逻辑
            Class<?> localClass = ConfigUtils.isDefault(stub) ? ReflectUtils.forName(interfaceClass.getName() + "Stub") : ReflectUtils.forName(stub);
            if (!interfaceClass.isAssignableFrom(localClass)) {
                throw new IllegalStateException("The local implementation class " + localClass.getName() + " not implement interface " + interfaceClass.getName());
            }
            try {
                ReflectUtils.findConstructor(localClass, interfaceClass);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("No such constructor \"public " + localClass.getSimpleName() + "(" + interfaceClass.getName() + ")\" in local implementation class " + localClass.getName());
            }
        }
        if (ConfigUtils.isNotEmpty(mock)) { //开发自测，联调过程中，经常碰到一些下游服务调用不通的场景，这个时候我们如何不依赖于下游系统，就业务系统独立完成自测。服务调不通时，调用mock类
            if (mock.startsWith(Constants.RETURN_PREFIX)) {
                String value = mock.substring(Constants.RETURN_PREFIX.length());
                try {
                    MockInvoker.parseMockValue(value);
                } catch (Exception e) {
                    throw new IllegalStateException("Illegal mock json value in <dubbo:service ... mock=\"" + mock + "\" />");
                }
            } else {
                Class<?> mockClass = ConfigUtils.isDefault(mock) ? ReflectUtils.forName(interfaceClass.getName() + "Mock") : ReflectUtils.forName(mock);
                if (!interfaceClass.isAssignableFrom(mockClass)) {
                    throw new IllegalStateException("The mock implementation class " + mockClass.getName() + " not implement interface " + interfaceClass.getName());
                }
                try {
                    mockClass.getConstructor(new Class<?>[0]);
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException("No such empty constructor \"public " + mockClass.getSimpleName() + "()\" in mock implementation class " + mockClass.getName());
                }
            }
        }
        /**
         * 问题集：todo @csy-new
         * 1）实践：本地服务功能使用
         * 2) 实践：本地stub功能使用
         * 3）实践：Mock功能使用
         */
    }

    /**
     * @return local
     * @deprecated Replace to <code>getStub()</code>
     */
    @Deprecated
    public String getLocal() {
        return local;
    }

    /**
     * @param local
     * @deprecated Replace to <code>setStub(Boolean)</code>
     */
    @Deprecated
    public void setLocal(Boolean local) {
        if (local == null) {
            setLocal((String) null);
        } else {
            setLocal(String.valueOf(local));
        }
    }

    /**
     * @param local
     * @deprecated Replace to <code>setStub(String)</code>
     */
    @Deprecated
    public void setLocal(String local) {
        checkName("local", local);
        this.local = local;
    }

    public String getStub() {
        return stub;
    }

    public void setStub(Boolean stub) {
        if (local == null) {
            setStub((String) null);
        } else {
            setStub(String.valueOf(stub));
        }
    }

    public void setStub(String stub) {
        checkName("stub", stub);
        this.stub = stub;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        checkExtension(Cluster.class, "cluster", cluster);
        this.cluster = cluster;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        checkExtension(ProxyFactory.class, "proxy", proxy);
        this.proxy = proxy;
    }

    public Integer getConnections() {
        return connections;
    }

    public void setConnections(Integer connections) {
        this.connections = connections;
    }

    @Parameter(key = Constants.REFERENCE_FILTER_KEY, append = true)
    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        checkMultiExtension(Filter.class, "filter", filter);
        this.filter = filter;
    }

    @Parameter(key = Constants.INVOKER_LISTENER_KEY, append = true)
    public String getListener() {
        checkMultiExtension(InvokerListener.class, "listener", listener);
        return listener;
    }

    public void setListener(String listener) {
        this.listener = listener;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        checkNameHasSymbol("layer", layer);
        this.layer = layer;
    }

    public ApplicationConfig getApplication() {
        return application;
    }

    public void setApplication(ApplicationConfig application) {
        this.application = application;
    }

    public ModuleConfig getModule() {
        return module;
    }

    public void setModule(ModuleConfig module) {
        this.module = module;
    }

    public RegistryConfig getRegistry() {
        return registries == null || registries.size() == 0 ? null : registries.get(0);
    }

    public void setRegistry(RegistryConfig registry) { //设置一个注册配置到注册列表
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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        checkMultiName("owner", owner);
        this.owner = owner;
    }

    public Integer getCallbacks() {
        return callbacks;
    }

    public void setCallbacks(Integer callbacks) {
        this.callbacks = callbacks;
    }

    public String getOnconnect() {
        return onconnect;
    }

    public void setOnconnect(String onconnect) {
        this.onconnect = onconnect;
    }

    public String getOndisconnect() {
        return ondisconnect;
    }

    public void setOndisconnect(String ondisconnect) {
        this.ondisconnect = ondisconnect;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    // -------overwrite begin

    /**
     * overwrite 加载注册中心配置，并且生成url
     * 1）检查注册中心，并设置到注册中心config中checkRegistry
     *    1.1) 对registries列表进行非空判断
     * 2）组装注册中心url
     *    2.0) 循环遍历注册中心config列表
     *    2.1）从注册config获取address，若address为空赋值任意地址ANYHOST_VALUE。从系统属性中dubbo.registry.address获取address，若存在sysaddress，则覆盖之前的地址
     *    2.2）若address不为空，且可用（!NO_AVAILABLE）时继续。将应用配置application、config添加到url参数map中
     *    2.3) 分析path（注册中心接口名称）、dubbo（版本号version）、pid（进程id，ConfigUtils.getPid()）、timestamp（时间戳）等键
     *    2.4）在map中不存在protocol键时，判断RegistryFactory中是否有remote扩展。若有则把协议protocal置为remote，否则置为dubbo
     *    2.5）将address以及url的参数map，生成UrlUtils.parseURLs(address, map);
     *    2.6）对每个url处理，url增加registry键，替换protocol协议名称
     *    2.7）若是满足条件的提供者或消费者 （provider && REGISTRY） || (!provider && SUBSCRIBE) ，将组装的url添加到url列表中 registryList.add(url);
     */
    //加载配置此处生成的url： zookeeper://localhost:2181/com.alibaba.dubbo.registry.RegistryService?application=api_demo&dubbo=2.0.0&pid=36916&timestamp=1562032607782
    //返回的Registry的url： registry://localhost:2181/com.alibaba.dubbo.registry.RegistryService?application=api_demo&dubbo=2.0.0&pid=36972&registry=zookeeper&timestamp=1562032865696

    protected List<URL> loadRegistriesOverride(boolean provider) {
        checkRegistry();
        List<URL> registryUrls = new ArrayList<>();
        if (registries != null && registries.size() > 0) {
            for (RegistryConfig config : registries) {
                String address = config.getAddress();
                if (address == null || address.length() == 0) {
                    address = Constants.ANYHOST_VALUE;
                }
                String sysaddress = System.getProperty("dubbo.registry.address");
                if (sysaddress != null && sysaddress.length() > 0) {
                    address = sysaddress;
                }
                if (address != null && address.length() > 0 &&
                        !RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(address)) {
                    Map<String, String> map = new HashMap<>();
                    appendParameters(map, application);
                    appendParameters(map, config);
                    map.put("path", RegistryService.class.getName());
                    map.put("dubbo", Version.getVersion());
                    if (ConfigUtils.getPid() > 0) { // pid值可用常量Constants.PID_KEY
                        map.put("pid", String.valueOf(ConfigUtils.getPid()));
                    }
                    //timestamp可用常量Constants.TIMESTAMP_KEY
                    map.put("timestamp", String.valueOf(System.currentTimeMillis()));

                    if (!map.keySet().contains("protocol")) {
                        boolean isHasExtend = ExtensionLoader.getExtensionLoader(RegistryFactory.class).hasExtension("remote");
                        if (isHasExtend) {
                            map.put("protocol", "remote");
                        } else {
                            map.put("protocol", "dubbo");
                        }
                    }

                    List<URL> urlList = UrlUtils.parseURLs(address, map);
                    for (URL url : urlList) {
                        url = url.addParameter(Constants.REGISTRY_KEY, url.getProtocol()); //是从url中获取protocol，不是从config获取
                        url = url.setProtocol(Constants.REGISTRY_PROTOCOL);
                        if ((provider && url.getParameter(Constants.REGISTER_KEY, true)) ||
                                (!provider && url.getParameter(Constants.SUBSCRIBE_KEY, true))) {
                            registryUrls.add(url);
                        }
                    }
                }
            }
        }
        return registryUrls;
    }




    // ------overwrite end
}