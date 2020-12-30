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
import com.alibaba.dubbo.common.bytecode.Proxy;
import com.alibaba.dubbo.common.bytecode.Wrapper;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.ClassHelper;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.support.Parameter;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.cluster.ConfiguratorFactory;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.alibaba.dubbo.rpc.support.ProtocolUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ServiceConfig
 *
 * @author william.liangf
 * @export
 */
/**@ 服务提供者配置 */
public class ServiceConfig<T> extends AbstractServiceConfig {
    /**
     * ServiceConfig数据结构
     * 继承关系
     * ServiceConfig -》AbstractServiceConfig -》 AbstractInterfaceConfig
     * -》AbstractMethodConfig -》AbstractConfig
     *
     * 维护的数据
     * Protocol（协议）、ProxyFactory（代理工厂）、ScheduledExecutorService（延迟线程池 ）
     * List<Exporter<?>> 暴露者列表、interfaceName（暴露的接口名）、interfaceClass（暴露的接口）
     * T（接口的实现类）、path（服务名称）、List<MethodConfig>（方法配置）、ProviderConfig（提供者配置）
     * exported（是否已暴露）、unexported（是否取消暴露）、generic（泛化接口）
     *
     */

    private static final long serialVersionUID = 3033787999037024738L;

    private static final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    private static final ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    private static final Map<String, Integer> RANDOM_PORT_MAP = new HashMap<String, Integer>(); // 随机端口map

    /** 延迟线程池 */
    private static final ScheduledExecutorService delayExportExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("DubboServiceDelayExporter", true));
    private final List<URL> urls = new ArrayList<URL>();
    private final List<Exporter<?>> exporters = new ArrayList<Exporter<?>>();
    // 接口名称（暴露的接口名）
    private String interfaceName;
    // 接口类型（暴露的接口）
    private Class<?> interfaceClass;
    // 接口的实现类
    private T ref;
    // 服务名称
    private String path;
    // 方法配置
    private List<MethodConfig> methods;
    private ProviderConfig provider;
    /**@ volatile 原子安全 是否已暴露 */
    private transient volatile boolean exported;

    // unexported是否已经解除暴露
    private transient volatile boolean unexported; //boolean 类型的成员变量，默认false

    private volatile String generic; //是否是通用接口

    public ServiceConfig() {
    }

    public ServiceConfig(Service service) {
        appendAnnotation(Service.class, service);
    }

    /**
     * 将ProviderConfig列表转换到ProtocolConfig列表（已弃用）
     */
    @Deprecated
    private static final List<ProtocolConfig> convertProviderToProtocol(List<ProviderConfig> providers) {
        if (providers == null || providers.size() == 0) {
            return null;
        }
        List<ProtocolConfig> protocols = new ArrayList<ProtocolConfig>(providers.size());
        for (ProviderConfig provider : providers) {
            protocols.add(convertProviderToProtocol(provider));
        }
        return protocols;
    }

    /**
     * 将ProtocolConfig列表转换到ProviderConfig列表
     */
    @Deprecated
    private static final List<ProviderConfig> convertProtocolToProvider(List<ProtocolConfig> protocols) {
        if (protocols == null || protocols.size() == 0) {
            return null;
        }
        List<ProviderConfig> providers = new ArrayList<ProviderConfig>(protocols.size());
        for (ProtocolConfig provider : protocols) {
            providers.add(convertProtocolToProvider(provider));
        }
        return providers;
    }

    /**@ 将提供者配置ProviderConfig 转换为 ProtocolConfig 协议配置*/
    @Deprecated
    private static final ProtocolConfig convertProviderToProtocol(ProviderConfig provider) {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName(provider.getProtocol().getName());
        protocol.setServer(provider.getServer());
        protocol.setClient(provider.getClient());
        protocol.setCodec(provider.getCodec());
        protocol.setHost(provider.getHost());
        protocol.setPort(provider.getPort());
        protocol.setPath(provider.getPath());
        protocol.setPayload(provider.getPayload());
        protocol.setThreads(provider.getThreads());
        protocol.setParameters(provider.getParameters());
        return protocol;
    }

    /**
     * 将ProtocolConfig转换为ProviderConfig
     */
    @Deprecated
    private static final ProviderConfig convertProtocolToProvider(ProtocolConfig protocol) {
        ProviderConfig provider = new ProviderConfig();
        provider.setProtocol(protocol);
        provider.setServer(protocol.getServer());
        provider.setClient(protocol.getClient());
        provider.setCodec(protocol.getCodec());
        provider.setHost(protocol.getHost());
        provider.setPort(protocol.getPort());
        provider.setPath(protocol.getPath());
        provider.setPayload(protocol.getPayload());
        provider.setThreads(protocol.getThreads());
        provider.setParameters(protocol.getParameters());
        return provider;
    }

    private static Integer getRandomPort(String protocol) {
        protocol = protocol.toLowerCase(); //协议名：比如dubbo
        if (RANDOM_PORT_MAP.containsKey(protocol)) { //若随机端口map中key没包含协议名，则取最小整数值
            return RANDOM_PORT_MAP.get(protocol);
        }
        return Integer.MIN_VALUE;/** 整数的最小值，负2的31次方 -2147... */
    }

    private static void putRandomPort(String protocol, Integer port) {
        protocol = protocol.toLowerCase();
        if (!RANDOM_PORT_MAP.containsKey(protocol)) { //判断缓存中是否存在协议与端口的映射，若没有则添加
            RANDOM_PORT_MAP.put(protocol, port);
        }
    }

    public URL toUrl() {
        return urls == null || urls.size() == 0 ? null : urls.iterator().next();
    }

    public List<URL> toUrls() {
        return urls;
    }

    @Parameter(excluded = true) // 方法需要被排除
    public boolean isExported() {
        return exported;
    }

    @Parameter(excluded = true)
    public boolean isUnexported() {
        return unexported;
    }

    /**@c 接口暴露是在提供方，接口引用是在消费方 */
    public synchronized void export() {
        if (provider != null) {
            if (export == null) {
                export = provider.getExport();
            }
            if (delay == null) {
                delay = provider.getDelay();
            }
        }
        if (export != null && !export) { /**@c 如指定服务不暴露，直接终止返回 */
            return;
        }

        if (delay != null && delay > 0) {// 延迟暴露，在指定的时间后执行任务
            delayExportExecutor.schedule(new Runnable() {
                public void run() {
                    doExport();
                }
            }, delay, TimeUnit.MILLISECONDS);
        } else {
            doExport();
        }
    }

    protected synchronized void doExport() {
        if (unexported) {
            throw new IllegalStateException("Already unexported!");
        }
        if (exported) {
            return;
        }
        exported = true;
        if (interfaceName == null || interfaceName.length() == 0) {/**@c 暴露的接口不能为空 */
            throw new IllegalStateException("<dubbo:service interface=\"\" /> interface not allow null!");
        }
        checkDefault(); //检查ProviderConfig中的配置以及设置
        if (provider != null) {/**@c ServiceConfig中的配置，依次从ProviderConfig、ModuleConfig、ApplicationConfig获取，由小到大，类似局部、全局变量 */
            if (application == null) {
                application = provider.getApplication();
            }
            if (module == null) {
                module = provider.getModule();
            }
            if (registries == null) {
                registries = provider.getRegistries();
            }
            if (monitor == null) {
                monitor = provider.getMonitor();
            }
            if (protocols == null) {
                protocols = provider.getProtocols();
            }
        }
        if (module != null) { //module：可能来自ServiceConfig，也可能来自ProviderConfig
            if (registries == null) {
                registries = module.getRegistries();
            }
            if (monitor == null) {
                monitor = module.getMonitor();
            }
        }
        if (application != null) {
            if (registries == null) {
                registries = application.getRegistries();
            }
            if (monitor == null) {
                MonitorConfig monitor = application.getMonitor();
                this.monitor = monitor;
            }
        }
        if (ref instanceof GenericService) { /**@c 通用接口的判断 */
            interfaceClass = GenericService.class;
            if (StringUtils.isEmpty(generic)) {
                generic = Boolean.TRUE.toString();
            }
        } else {
            try {
                interfaceClass = Class.forName(interfaceName, true, Thread.currentThread() //history-h3 service.setInterface(ApiDemo.class) 已经设置了class，为啥此处还要获取class
                        .getContextClassLoader()); //使用类加载返回类对象
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            checkInterfaceAndMethods(interfaceClass, methods);/**@c 检查设置的方法config是否在暴露的接口 */ // methods是否哪里设置的？并没有显示设置 : 可以调用ServiceConfig的setMethods()
            checkRef();/**@c 检查引用ref */
            generic = Boolean.FALSE.toString();
        }
        if (local != null) {/**@c 本地服务 dubbo:service local机制，已经废弃，被stub属性所替换 */
            if ("true".equals(local)) {
                local = interfaceName + "Local";
            }
            Class<?> localClass;
            try {
                localClass = ClassHelper.forNameWithThreadContextClassLoader(local); //history-h3 用途是啥？创建指定名称的class吗？
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            if (!interfaceClass.isAssignableFrom(localClass)) {
                throw new IllegalStateException("The local implementation class " + localClass.getName() + " not implement interface " + interfaceName);
            }
        }
        //本地存根：对真正调用的对象进行代理
        if (stub != null) { /**@c 本地存根 (远程服务后，客户端通常只剩下接口，而实现全在服务器端，但提供方有些时候想在客户端也执行部分逻辑，比如：做 ThreadLocal 缓存，提前验证参数，调用失败后伪造容错数据等) */
            if ("true".equals(stub)) {/**@c interfaceName接口名如： com.alibaba.dubbo.config.api.DemoService*/
                stub = interfaceName + "Stub";
            }
            Class<?> stubClass;
            try {
                stubClass = ClassHelper.forNameWithThreadContextClassLoader(stub);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            if (!interfaceClass.isAssignableFrom(stubClass)) {
                throw new IllegalStateException("The stub implementation class " + stubClass.getName() + " not implement interface " + interfaceName);
            }
        }
        //检测逻辑细节待调试
        checkApplication();/**@c 暴露服务前检查配置，如果配置满足条件，则创建相应的配置对象，并且添加属性 */
        checkRegistry();
        checkProtocol();
        appendProperties(this);/**@c 为ServiceConfig添加属性 */
        checkStubAndMock(interfaceClass);
        if (path == null || path.length() == 0) {
            path = interfaceName;
        }
        doExportUrls();
    }

    private void checkRef() {
        // 检查引用不为空，并且引用必需实现接口
        if (ref == null) {
            throw new IllegalStateException("ref not allow null!");
        }
        if (!interfaceClass.isInstance(ref)) {/**@c 检查引用是否是声明接口的实现 */
            throw new IllegalStateException("检查引用不为空 The class "
                    + ref.getClass().getName() + " unimplemented interface "
                    + interfaceClass + "!");
        }
    }

    /**
     * 取消服务暴露 history-new 哪种操作能进入取消暴露
     * 1）若标志exported为false，即为未暴露的，则不处理
     * 2）若标志unexported为true，即不需要暴露，已经取消暴露，不处理
     * 3）若暴露的服务列表不为空
     *   3.1）遍历服务列表，依次将服务取消暴露exporter.unexport()
     *   3.2）当处理完后，清空暴露列表
     * 4）将标志unexported置为true
     */
    public synchronized void unexport() {
        if (!exported) {
            return;
        }
        if (unexported) {
            return;
        }
        if (exporters != null && exporters.size() > 0) {
            for (Exporter<?> exporter : exporters) {
                try {
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn("unexpected err when unexport" + exporter, t);
                }
            }
            exporters.clear();
        }
        unexported = true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void doExportUrls() {
        List<URL> registryURLs = loadRegistries(true);
        for (ProtocolConfig protocolConfig : protocols) {
            doExportUrlsFor1Protocol(protocolConfig, registryURLs);
        }
    }

    //暴露URL流程有点复杂，需要仔细分析 ： 组装处理protocalConfig以及注册url，生成invoker，执行暴露export(invoker)
    private void doExportUrlsFor1Protocol(ProtocolConfig protocolConfig, List<URL> registryURLs) {
        String name = protocolConfig.getName();
        if (name == null || name.length() == 0) {
            name = "dubbo"; //默认协议为dubbo
        }

        String host = protocolConfig.getHost();
        if (provider != null && (host == null || host.length() == 0)) {
            host = provider.getHost();
        }
        boolean anyhost = false;
        /**
         * 当本地地址host无效时，尝试多种方式获取host
         * 1）通过InetAddress获取host
         * 2）通过Socket连接注册中心，若能连上注册中心，则取Socket的本地地址host
         * 3）通过遍历网卡NetworkInterface，获取本地地址host
         */
        if (NetUtils.isInvalidLocalHost(host)) {
            anyhost = true; // 设置anyhost
            try {
                host = InetAddress.getLocalHost().getHostAddress(); //获取本地ip地址
            } catch (UnknownHostException e) {
                logger.warn(e.getMessage(), e);
            }
            if (NetUtils.isInvalidLocalHost(host)) {
                if (registryURLs != null && registryURLs.size() > 0) {
                    for (URL registryURL : registryURLs) {
                        try {
                            Socket socket = new Socket();
                            try {
                                /**@c 连接到注册中心 */
                                SocketAddress addr = new InetSocketAddress(registryURL.getHost(), registryURL.getPort()); // socket客户端连接
                                socket.connect(addr, 1000); //此处registry设置的地址为localhost，host取到的值为127.0.0.1
                                host = socket.getLocalAddress().getHostAddress(); // 此处socket连接，只为获取host？ 是的，只为获取host
                                break;
                            } finally {
                                try {
                                    socket.close(); //关闭socket
                                } catch (Throwable e) {
                                }
                            }
                        } catch (Exception e) {
                            logger.warn(e.getMessage(), e);
                        }
                    }
                }
                if (NetUtils.isInvalidLocalHost(host)) {
                    host = NetUtils.getLocalHost(); //从网卡中获取有效的本地地址
                }
            }
        }

        /**
         * 暴露服务的端口处理
         * 1）从配置的config中获取，若有配置port，则直接获取
         * 2）若没有配置port，则产生随机的端口，并用NetUtils工具校验端口是否可用
         * （随机端口从30000开始，随机值的返回是10000，所以随机端口 30000 - 40000）
         */
        Integer port = protocolConfig.getPort();
        if (provider != null && (port == null || port == 0)) {
            port = provider.getPort();
        }
        final int defaultPort = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(name).getDefaultPort();
        if (port == null || port == 0) {
            port = defaultPort;
        }
        if (port == null || port <= 0) {
            port = getRandomPort(name);
            if (port == null || port < 0) {
                port = NetUtils.getAvailablePort(defaultPort); //不断递增端口port，然后使用socket尝试连接，成功连接时的端口为正常端口
                putRandomPort(name, port); //将协议名与端口映射起来并缓存
            }
            logger.warn("Use random available port(" + port + ") for protocol " + name);
        }

        /**
         * 构建传输的参数map
         * 设置基本信息，side、version、timestamp、pid等
         */
        Map<String, String> map = new HashMap<String, String>();
        if (anyhost) { //判断是否是任意host
            map.put(Constants.ANYHOST_KEY, "true");
        }
        map.put(Constants.SIDE_KEY, Constants.PROVIDER_SIDE);/**@c 提供方*/
        map.put(Constants.DUBBO_VERSION_KEY, Version.getVersion());
        map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
        if (ConfigUtils.getPid() > 0) {
            map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
        }
        /**
         * 将config对象的属性值附加到参数map中
         */
        appendParameters(map, application); // application成员变量
        appendParameters(map, module);
        appendParameters(map, provider, Constants.DEFAULT_KEY);
        appendParameters(map, protocolConfig); // protocolConfig传入的参数
        appendParameters(map, this);
        if (methods != null && methods.size() > 0) { // methods是从哪里设置的？方法级参数设置，<dubbo:method name="" ...>
            for (MethodConfig method : methods) { //将设置的MethodConfig与 暴露接口的名称以及参数类型进行比较（反射获取）
                // <dubbo:method> 方法配置时加上方法名作为前缀，便于区分，比如sayApi.timeout=3000
                appendParameters(map, method, method.getName());
                /**
                 * 对重试key进行替换，若存在methodName + ".retry"的键，则进行移除并改为 methodName + ".retries"
                 */
                String retryKey = method.getName() + ".retry";
                if (map.containsKey(retryKey)) {
                    String retryValue = map.remove(retryKey); //将map中的key移除，并且返回key对应的值
                    if ("false".equals(retryValue)) { //不重试 等价 重试次数为0 (retry已被弃用，所以改为reties)
                        map.put(method.getName() + ".retries", "0"); //重试次数处理
                    }
                }
                /**
                 * <dubbo:argument> 设置参数值
                 * 1）获取方法对应的参数config列表，依次遍历
                 * 2）
                 */
                List<ArgumentConfig> arguments = method.getArguments();
                if (arguments != null && arguments.size() > 0) {
                    for (ArgumentConfig argument : arguments) { //
                        //类型自动转换.（在设置了类型type 或者 类型type和下标index都设置的时候进入）
                        if (argument.getType() != null && argument.getType().length() > 0) {
                            Method[] methods = interfaceClass.getMethods(); //获取暴露接口中的所有方法
                            //遍历所有方法
                            if (methods != null && methods.length > 0) {
                                for (int i = 0; i < methods.length; i++) {
                                    String methodName = methods[i].getName();
                                    //匹配方法名称，获取方法签名. 确保方法配置是有效的方法名，排除乱填的方法名
                                    if (methodName.equals(method.getName())) { //MethodConfig中的方法名与暴露接口的方法名比较，直到方法名相同
                                        Class<?>[] argtypes = methods[i].getParameterTypes();
                                        //一个方法中单个callback
                                        if (argument.getIndex() != -1) {/**@c 若设置下标，直接根据下标从接口的参数列表中获取到参数类型，然后与设置的类型进行比较 */
                                            if (argtypes[argument.getIndex()].getName().equals(argument.getType())) { //将MethodConfig中的参数与暴露接口的参数比较
                                                appendParameters(map, argument, method.getName() + "." + argument.getIndex()); //此处prefix 如：sayApi.0
                                            } else { //ArgumentConfig中的index以及type都是会被忽视的，即不加载url中
                                                throw new IllegalArgumentException("argument config error : the index attribute and type attirbute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                            }
                                        } else { //参数回调，服务端可以调用客户端逻辑
                                            //一个方法中多个callback
                                            for (int j = 0; j < argtypes.length; j++) { //循环结束条件：1）遍历完参数列表 2）出现异常
                                                Class<?> argclazz = argtypes[j]; //若只设置了type，同一个方法中的相同参数的callback一样，并且后面只设置type的参数把前面替换
                                                if (argclazz.getName().equals(argument.getType())) { //会把参数列表中的所有相同类型的callback设置为相同
                                                    appendParameters(map, argument, method.getName() + "." + j);
                                                    if (argument.getIndex() != -1 && argument.getIndex() != j) { //history-h3 为啥此处还要判断一次？
                                                        throw new IllegalArgumentException("argument config error : the index attribute and type attirbute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        //在没有设置参数类型type，但设置了参数下标index
                        } else if (argument.getIndex() != -1) { //argument.getIndex() 与 argument.getType()有啥区别？为啥条件不互斥
                            appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                        } else { //参数必须指定下标或类型，若两者都没指定则抛异常(history-h3 为啥此处的异常没有抛出去)
                            throw new IllegalArgumentException("argument config must set index or type attribute.eg: <dubbo:argument index='0' .../> or <dubbo:argument type=xxx .../>");
                        }

                    }
                }
            } // end of methods for
        }

        //泛化接口暴露的url
        //dubbo://10.118.32.93:20885/com.alibaba.dubbo.rpc.service.GenericService?anyhost=true&application=generic_demo&dubbo=2.0.0&export=true
        // &generic=true&interface=com.alibaba.dubbo.rpc.service.GenericService
        // &methods=*&pid=8252&side=provider&timeout=3000×tamp=1562677675014
        if (ProtocolUtils.isGeneric(generic)) { //是否是通用接口
            map.put("generic", generic);
            map.put("methods", Constants.ANY_VALUE);
        } else {
            String revision = Version.getVersion(interfaceClass, version); //获取接口所在包的版本号 如：com.alibaba.dubbo.demo.ApiDemo:1.3.4
            if (revision != null && revision.length() > 0) {
                map.put("revision", revision);
            }

            String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
            if (methods.length == 0) {
                logger.warn("NO method found in service interface " + interfaceClass.getName());
                map.put("methods", Constants.ANY_VALUE);
            } else { //设置暴露接口的方法名，如"methods" -> "sayApi"
                map.put("methods", StringUtils.join(new HashSet<String>(Arrays.asList(methods)), ","));
            }
        }
        if (!ConfigUtils.isEmpty(token)) {/**@c token生成 */
            if (ConfigUtils.isDefault(token)) {
                map.put("token", UUID.randomUUID().toString());
            } else {
                map.put("token", token);
            }
        }
        if ("injvm".equals(protocolConfig.getName())) {/**@c 本地服务 若是本地服务，不设置注册中心以及通知 */
            protocolConfig.setRegister(false);
            map.put("notify", "false");
        }
        // 导出服务
        String contextPath = protocolConfig.getContextpath();
        if ((contextPath == null || contextPath.length() == 0) && provider != null) {
            contextPath = provider.getContextpath();
        }
        URL url = new URL(name, host, port, (contextPath == null || contextPath.length() == 0 ? "" : contextPath + "/") + path, map);

        if (ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                .hasExtension(url.getProtocol())) {
            url = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                    .getExtension(url.getProtocol()).getConfigurator(url).configure(url);
        }

        String scope = url.getParameter(Constants.SCOPE_KEY);
        //配置为none不暴露
        if (!Constants.SCOPE_NONE.toString().equalsIgnoreCase(scope)) {

            //配置不是remote的情况下做本地暴露 (配置为remote，则表示只暴露远程服务)
            if (!Constants.SCOPE_REMOTE.toString().equalsIgnoreCase(scope)) {
                exportLocal(url);
            }
            //如果配置不是local则暴露为远程服务.(配置为local，则表示只暴露本地服务)
            if (!Constants.SCOPE_LOCAL.toString().equalsIgnoreCase(scope)) {
                if (logger.isInfoEnabled()) {
                    logger.info("Export dubbo service " + interfaceClass.getName() + " to url " + url);
                }
                if (registryURLs != null && registryURLs.size() > 0
                        && url.getParameter("register", true)) {
                    for (URL registryURL : registryURLs) {
                        /**@c 静态服务 人工管理服务提供者的上线和下线，此时需将注册中心标识为非动态管理模式 */
                        url = url.addParameterIfAbsent("dynamic", registryURL.getParameter("dynamic"));
                        URL monitorUrl = loadMonitor(registryURL);/**@c 加载监控 */
                        if (monitorUrl != null) {
                            url = url.addParameterAndEncoded(Constants.MONITOR_KEY, monitorUrl.toFullString());
                        }
                        if (logger.isInfoEnabled()) {
                            logger.info("Register dubbo service " + interfaceClass.getName() + " url " + url + " to registry " + registryURL);
                        }

                        //history-h3 组装invoker
                        Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, registryURL.addParameterAndEncoded(Constants.EXPORT_KEY, url.toFullString()));
                        //invoker的值 registry://localhost:2181/com.alibaba.dubbo.registry.RegistryService?application=api_demo&dubbo=2.0.0&export=dubbo%3A%2F%2F10.118.32.182%3A20881%2Fcom.alibaba.dubbo.demo.ApiDemo%3Fanyhost%3Dtrue%26application%3Dapi_demo%26delay%3D5%26dubbo%3D2.0.0%26export%3Dtrue%26generic%3Dfalse%26interface%3Dcom.alibaba.dubbo.demo.ApiDemo%26methods%3DsayApi%26pid%3D43951%26side%3Dprovider%26timeout%3D3000%26timestamp%3D1562072331803&pid=43951&registry=zookeeper&timestamp=1562071838319
                        Exporter<?> exporter = protocol.export(invoker);
                        exporters.add(exporter);
                    }
                } else {
                    Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, url);

                    Exporter<?> exporter = protocol.export(invoker);
                    exporters.add(exporter);
                }
            }
        }
        //dubbo://10.118.32.42:20881/com.alibaba.dubbo.demo.ApiDemo?
        // anyhost=true&application=api_demo&delay=5&dubbo=2.0.0&export=true&generic=false
        // &interface=com.alibaba.dubbo.demo.ApiDemo&methods=sayHello,sayApi&pid=52248
        // &revision=1.3.4&sayApi.0.callback=false&sayApi.2.callback=true&
        // sayApi.3.callback=true&sayApi.retries=0&sayApi.timeout=3000&side=
        // provider&timeout=3000&timestamp=1562997581285&version=1.3.4,
        // dubbo version: 2.0.0, current host: 10.118.32.42
        logger.info("url origin create, ExportUrl for protocol:" + url.toFullString());
        this.urls.add(url);
    }

    //暴露的服务没有host和port，所以暴露不成功
    //Export dubbo service com.alibaba.dubbo.demo.ApiDemo to url dubbo:///com.alibaba.dubbo.demo.ApiDemo?anyhost=true&application=api_demo&delay=5&dubbo=2.0.0&export=true&generic=false&interface=com.alibaba.dubbo.demo.ApiDemo&methods=sayApi&pid=44803&side=provider&timeout=3000&timestamp=1562077212572, dubbo version: 2.0.0, current host: 192.168.0.102

    /**
     *   9.1 暴露本地服务
     * @param url
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void exportLocal(URL url) {
        if (!Constants.LOCAL_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
            URL local = URL.valueOf(url.toFullString())
                    .setProtocol(Constants.LOCAL_PROTOCOL)
                    .setHost(NetUtils.LOCALHOST)
                    .setPort(0);
            Exporter<?> exporter = protocol.export(
                    proxyFactory.getInvoker(ref, (Class) interfaceClass, local));
            exporters.add(exporter);
            logger.info("Export dubbo service " + interfaceClass.getName() + " to local registry");
        }
    }

    private void checkDefault() {
        if (provider == null) {
            provider = new ProviderConfig();
        }
        appendProperties(provider); //从系统属性、配置bean、属性文件中取出值，并设置到ProviderConfig
    }

    private void checkProtocol() {
        if ((protocols == null || protocols.size() == 0) //若ServiceConfig没有设置协议，则取ProviderConfig中的协议配置
                && provider != null) {
            setProtocols(provider.getProtocols());
        }
        // 兼容旧版本
        if (protocols == null || protocols.size() == 0) {
            setProtocol(new ProtocolConfig());
        }
        for (ProtocolConfig protocolConfig : protocols) {
            if (StringUtils.isEmpty(protocolConfig.getName())) {
                protocolConfig.setName("dubbo"); //默认协议：dubbo
            }
            appendProperties(protocolConfig);
        }
    }

    public Class<?> getInterfaceClass() {
        if (interfaceClass != null) {
            return interfaceClass;
        }
        if (ref instanceof GenericService) {
            return GenericService.class;
        }
        try {
            if (interfaceName != null && interfaceName.length() > 0) {
                this.interfaceClass = Class.forName(interfaceName, true, Thread.currentThread()
                        .getContextClassLoader());
            }
        } catch (ClassNotFoundException t) {
            throw new IllegalStateException(t.getMessage(), t);
        }
        return interfaceClass;
    }

    /**
     * @param interfaceClass
     * @see #setInterface(Class)
     * @deprecated
     */
    public void setInterfaceClass(Class<?> interfaceClass) {
        setInterface(interfaceClass);
    }

    public String getInterface() {
        return interfaceName;
    }

    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
        if (id == null || id.length() == 0) {
            id = interfaceName;
        }
    }

    public void setInterface(Class<?> interfaceClass) { //设置接口class同时，也设置接口名称
        if (interfaceClass != null && !interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        this.interfaceClass = interfaceClass;
        setInterface(interfaceClass == null ? (String) null : interfaceClass.getName());
    }

    public T getRef() {
        return ref;
    }

    public void setRef(T ref) {
        this.ref = ref;
    }

    @Parameter(excluded = true)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        checkPathName("path", path);
        this.path = path;
    }

    public List<MethodConfig> getMethods() {
        return methods;
    }

    // ======== Deprecated ========

    @SuppressWarnings("unchecked")
    public void setMethods(List<? extends MethodConfig> methods) {
        this.methods = (List<MethodConfig>) methods;
    }

    public ProviderConfig getProvider() {
        return provider;
    }

    public void setProvider(ProviderConfig provider) {
        this.provider = provider;
    }

    public String getGeneric() {
        return generic;
    }

    public void setGeneric(String generic) {
        if (StringUtils.isEmpty(generic)) {
            return;
        }
        if (ProtocolUtils.isGeneric(generic)) {
            this.generic = generic;
        } else {
            throw new IllegalArgumentException("Unsupported generic type " + generic);
        }
    }

    public List<URL> getExportedUrls() {
        return urls;
    }

    /**
     * @deprecated Replace to getProtocols()
     */
    @Deprecated
    public List<ProviderConfig> getProviders() {
        return convertProtocolToProvider(protocols);
    }

    /**
     * @deprecated Replace to setProtocols()
     */
    @Deprecated
    public void setProviders(List<ProviderConfig> providers) {
        this.protocols = convertProviderToProtocol(providers);
    }


    // ---------overwrite begin-------

    /**
     * export重写
     * 1）判断export（暴露）、delay（延迟属性）
     * 2）在ProviderConfig不为空的情况，若ServiceConfig没设置取ProviderConfig的值，否则取ServiceConfig的值
     * 3）判断是否需要暴露，若不要直接返回
     * 4）判断是否需要延迟，若需要使用定时类延迟指定的时间后暴露
     */
    public synchronized void exportOverride() {
        if (provider != null) {
            if (export == null) {
                export = provider.getExport();
            }
            if (delay == null) {
                delay = provider.getDelay();
            }
        }
        if (export != null && !export) {
            return;
        }
        if (delay != null && delay > 0) { //定时任务，没用类变量 delayExportExecutor，没有指定ThreadFactory
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.schedule(new Runnable() {
                @Override
                public void run() {
                    doExport();
                }
            }, delay, TimeUnit.MILLISECONDS); //延时时间为毫秒
        } else {
            doExport();
        }
    }

    /**
     * doExport重写
     * 1)判断unexported、exported属性值，若已经移除暴露，则抛非法状态异常；若exported暴露过的，则不再暴露
     *  1.1 将exported置为true
     *  1.2 判断接口名是否为空
     *  1.3 检查默认的providerConfig配置
     * 2）取值处理
     *   2.1） 若provider配置不为空，取出application、module、registries、monitor、protocols
     *   2.2） 若module配置不为空,取出monitor、registries
     *   2.3)  若application配置不为空，则取出monitor、registries
     * 3)判断ref是否是通用接口GenericService的实现类,若是将通用接口赋值给interfaceClass，并generic置为"true", 若不是获取interfaceName对应的类
     * 4)判断是否是本地服务local,若是附加 "Local",获取到类localClass
     * 5)判断是否是stub本地存根，若是附加 "Stub"，获取到类stubClass
     * 6）检查配置 checkApplication();checkRegistry();checkRef();
     *    设置当前ServiceConfig的属性值
     *    检查存根和mock checkStubAndMock
     * 7）检查完成后，暴露url， doExportUrlsFor1Protocol();
     */
    protected synchronized void doExportOverride() {
        if (unexported) {
            throw new IllegalStateException("Already unexported！");
        }

        if (exported) {
            return;
        }

        exported = true;
        if (interfaceName == null || interfaceName.length() == 0) {
            throw new IllegalStateException("interface name is null");
        }

        checkDefault(); //检查ProviderConfig，并设置属性值
        if (provider != null) {
            if (application == null) {
                application = provider.getApplication();
            }
            if (module == null) {
                module = provider.getModule();
            }
            if (registries == null) {
                registries = provider.getRegistries();
            }
            if (monitor == null) {
                monitor = provider.getMonitor();
            }
            if (protocols == null) {
                protocols = provider.getProtocols();
            }
        }

        if (module != null) {
            if (monitor == null) {
                monitor = module.getMonitor();
            }
            if (registries == null) {
                registries = module.getRegistries();
            }
        }
        if (application != null) {
            if (monitor == null) {
                MonitorConfig monitor = application.getMonitor();
                this.monitor = monitor;
            }
            if (registries == null) {
                registries = application.getRegistries();
            }
        }

        if (ref instanceof GenericService) {
            interfaceClass = GenericService.class;
            if (StringUtils.isEmpty(generic)) {
                generic = Boolean.TRUE.toString();
            }
        } else {
            try {
                interfaceClass = Class.forName(interfaceName, true, Thread.currentThread()
                        .getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }

            checkInterfaceAndMethods(interfaceClass, methods); //检查接口与设置的方法是否一致
            checkRef();
            generic = Boolean.FALSE.toString();
        }

        if (local != null) {
            if (local.equals("true")) {
                local = interfaceName + "Local"; //拼接接口名
            }
            Class<?> localClass;
            try {
                localClass = ClassHelper.forNameWithThreadContextClassLoader(local); //从线程中获取类加载器
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            if (!interfaceClass.isAssignableFrom(localClass)) { //判断loadClass是否是接口interfaceClass的实现类
                throw new IllegalStateException(localClass + "localClass not implement interface " + interfaceClass);
            }
        }

        if (stub != null) {
            if (stub.equals("true")) {
                stub = interfaceName + "Stub";
            }
            Class<?> stubClass;
            try {
                stubClass = ClassHelper.forNameWithThreadContextClassLoader(stub);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            if (!interfaceClass.isAssignableFrom(stubClass)) {
                throw new IllegalStateException(stubClass + "stubClass not implement interface " + interfaceClass);
            }
        }

        checkApplication();
        checkRegistry();
        checkProtocol();
        appendProperties(this);
        checkStubAndMock(interfaceClass);
        if (path == null || path.length() == 0) {
            path = interfaceName;
        }
        doExportUrls();
    }

    /**
     * 构建暴露协议Protocol的url，并生成invoker，用与暴露服务 export(invoker)
     *
     * 1)检查协议名是否为空 ProtocolConfig.getName, 若为空协议名 name="dubbo"
     * 2)服务暴露的主机ip host
     *  2.1）从ProtocolConfig获取host，在provider不为空，且host为空时，取provider的主机ip host
     *  2.2）NetUtils.isInvalidLocalHost(host) 判读是否是无效的host，若是anyhost = true;
     *      2.2.1) 从InetAddress获取getLocalHost，然后获取主机地址getHostAddress
     *      2.2.2）再次判断是否为无效host，若是有效host，并且注册中心的url不为空。使用Socket尝试连接，若能正常连接，取socket.getLocalAddress().getHostAddress()。
     *      2.2.3）继续判断是否是无效host，若是，通过网络工具NetUtils获取本地host（遍历网卡，找出有效的host）
     * 3）服务端口处理port
     *  3.1) 从protocolConfig.getPort()获取端口，若provider不为空，且port端口为空，则从provider获取端口
     *  3.2）获取Protocol指定名字name扩展的默认端口defaultPort，若port为空或为0，则取默认端口的值
     *  3.3）若port还为空，
     *       3.3.1) 从RANDOM_PORT_MAP中获取getRandomPort（若map中没有key，则返回最小整数）
     *       3.3.2) 若port还为空或小于等于0，使用工具获取端口NetUtils.getAvailablePort （以一个端口为起始值，在最大
     *       端口范围内MAX_PORT 之前递增，然后使用ServerSocket尝试连接，若能连接成功，则返回端口，并设置到 RANDOM_PORT_MAP中）
     * 4)处理协议中的参数SIDE_KEY（提供方）、DUBBO_VERSION_KEY（dubbo版本）、TIMESTAMP_KEY（时间戳）、PID_KEY（进程id）
     * 5）将配置中的属性添加到url参数map中，加入的配置有application、module、provider(带前缀DEFAULT_KEY)、protocolConfig（入参协议配置）、this（当前对象ServiceConfig）
     * 6）对方法配置MethodConfig、参数配置ArgumentConfig进行分析
     *   6.1）methods非空判断
     *   6.2）循环遍历methods （方法配置处理）
     *     6.2.1）将方法配置添加到url参数map中，并且带上前缀名 method.getName()
     *     6.2.2) 对重试的参数进行变更，因为MethodConfig中的retry是布尔值，转换为重试次数retries，如果map包含键method.getName() + ".retry"，
     *            将键移除，并且判断对应的值，若置为false，增加键method.getName() + ".retries"，并设为0。会重试的方法不做判断
     *     6.2.3）若方法配置中设置的method.getArguments() 参数列表不为空，则循环处理参数列表（参数配置处理）
     *         6.2.3.1）参数类型argument.getType() 不为空（设置了type或同时设置了type、index）
     *            6.2.3.1.1）获取暴露接口的所有方法，在这些方法不为空的时候，循环遍历方法列表，
     *                 找到methodName.equals(method.getName())，即MethodConfig与暴露接口中相同名称的接口
     *               6.2.3.1.1.1） 获取参数类型列表methods[i].getParameterTypes()
     *                  6.2.3.1.1.1.1) 若设置了下标index，argument.getIndex() != -1，判断参数列表中指定位置index对应的类型是否与argumentConfig设置类型名称是否一致，否则抛出非法参数异常
     *                  6.2.3.1.1.1.2）若没有设置下标，循环参数列表，按类型比较，只要是类型相同的，callback都设置为一样
     *         6.2.3.2）argument.getIndex() != -1 参数没有设置type，设置了index，则为指定类型添加参数config
     *         6.2.3.3）既没有设置type、又没设置index，则报异常
     *
     * 7）判断是否是泛化接口 generic判断（返回接口不需要引用API接口）
     *    7.1) 是泛化接口：设置generic,methods置为* ANY_VALUE
     *    7.2）不是泛化接口
     *       7.2.1）获取接口所在包的版本号 Version.getVersion，若不为空，设置值revision
     *       7.2.2) 获取暴露接口interfaceClass的封装类中的方法名
     *         7.2.2.1）是否存在方法，若没存在 methods置为* （ANY_VALUE)
     *         7.2.2.2）若存在方法，则按分隔符拼接方法名
     * 8）若token不为空
     *    8.1）判断token是否是默认值（为"true"或"default"）
     *      8.1.1) 若是：生成uuid，并且更新token的
     *      8.1.2) 若不是: map中设置token，值为token
     * 9）判断协议名是否为injvm，本地暴露
     *      9.1）若是：protocolConfig 注册属性register置为false，并将需暴露的map的notify置为false
     * 10）从protocolConfig获取上下文路径contextPath，
     *     若contextPath为null或者length==0，且provider不为null，若provider中获取contextPath
     * 11) 构建url ：name（协议名）、host、port、contextPath（上下文路径，判断是否为空，若不为空，与path拼接 ）、map（上文设置的参数map）
     *   11.1) ExtensionLoader判断 ConfiguratorFactory是否有扩展，url.getProtocol()
     *       若有获取ConfiguratorFactory的扩展，
     *       并且或取getConfigurator，获取Configurator，调用configure配置url
     * 12）从url获取暴露范围scope
     *    12.1）判断暴露返回是否为none，若不为none则继续判断
     *       12.1.1）判断是本地范围 !remote，若是暴露本地服务 exportLocal(url);
     *       12.1.2) 判断是否是远程暴露 !local，若是，继续进行
     *          12.1.2.1）打出暴露的日志
     *          12.1.2.2）判断注册url是否为空，并且url中的register为true
     *             12.1.2.2.1) 注册url不为空，遍历registryURLs 注册url列表
     *               12.1.2.2.1.1) url添加参数dynamic，值中registerUrl中获取（url.addParameterIfAbsent）
     *               12.1.2.2.1.2）加载监控，并返回监控url；若monitorUrl不为空时，加到url参数中 (url.addParameterAndEncoded 带上编码)
     *               12.1.2.2.1.3）打印加载日志
     *               12.1.2.2.1.4）通过代理创建invoker对象，ref（暴露接口的实例）、(Class)interfaceClass(暴露接口)、url（把暴露的url.toFullString作为registryURL键的值）
     *               12.1.2.2.1.5）传入invoker进行服务暴露，protocol.export(invoker);
     *               12.1.2.2.1.6）将产生的export记录exporters，exporters.add(exporter);
     *             12.1.2.2.2) 注册url为空
     *               12.1.2.2.2.1) 创建invoker，参数有ref、interfaceClass、url，（proxyFactory.getInvoker(ref, (Class) interfaceClass, url);）
     *               12.1.2.2.2.2) 暴露服务，protocol.export(invoker)，并且加到exporters
     * 13）加url添加到urls列表中 this.urls.add(url)
     *
     */
    private void doExportUrlsFor1ProtocolOverride(ProtocolConfig protocolConfig, List<URL> registryURLs) {
        String protocolName = protocolConfig.getName();
        if (protocolName == null || protocolName.length() == 0) {
            protocolName = "dubbo";
        }
        String host = protocolConfig.getHost();
        if (provider != null) {
            if(host == null || host.length() == 0) {
                host = provider.getHost();
            }
        }
        boolean anyhost = false;
        if (NetUtils.isInvalidLocalHost(host)) { //若是无效的host，继续查找
            anyhost = true;
            try {
                host = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                logger.warn(e.getMessage(), e); //异常信息提醒，并不中断流程
            }
            if (NetUtils.isInvalidLocalHost(host)) {
                if (registryURLs != null && registryURLs.size() > 0) {
                    for (URL url : registryURLs) { //尝试用socket连接注册中心
                        Socket socket = new Socket();
                        try {
                            SocketAddress address = new InetSocketAddress(url.getHost(), url.getPort()); //history-h3 registry在哪里循环
                            socket.connect(address, 1000);
                            host = socket.getLocalAddress().getHostAddress();
                            break;
                        } catch (Exception e) {

                        } finally {
                            try {
                                socket.close();
                            } catch (IOException e) {

                            }
                        }
                    }
                }
                if (NetUtils.isInvalidLocalHost(host)) {
                    host = NetUtils.getLocalHost();
                }
            }
        }

        Integer port = protocolConfig.getPort();
        if (provider != null) {
            if (port == null || port == 0) {
                port = provider.getPort();
            }
        }
        final int defaultPort = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(protocolName).getDefaultPort();
        if (port == null || port == 0) {
            port = defaultPort;
        }
        if (port == null || port <= 0) {
            port = getRandomPort(protocolName);
            if (port == null || port <= 0) {
                port = NetUtils.getAvailablePort(defaultPort); //从defaultPort开始递增尝试连接
                putRandomPort(protocolName, port);
            }
            logger.warn("use random port :" + port + ", protocolName :" + protocolName);
        }

        Map<String, String> map = new HashMap<>();
        if (anyhost) {
            map.put(Constants.ANYHOST_KEY, "true");
        }
        map.put(Constants.SIDE_KEY, Constants.PROVIDER_SIDE);
        map.put(Constants.DUBBO_VERSION_KEY, Version.getVersion());
        map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
        int pid = ConfigUtils.getPid();
        if (pid > 0) {
            map.put(Constants.PID_KEY, String.valueOf(pid));
        }
        appendParameters(map, application);
        appendParameters(map, module);
        appendParameters(map, provider, Constants.DEFAULT_KEY);
        appendParameters(map, protocolConfig);
        appendParameters(map, this);
        if (methods != null && methods.size() > 0) { //方法配置
            for (MethodConfig methodConfig : methods) {
                appendParameters(map, methodConfig, methodConfig.getName());
                String retryKey = methodConfig.getName() + ".retry";
                if (map.containsKey(retryKey)) {
                    String retryValue = map.remove(retryKey);
                    if ("false".equals(retryValue)) { //若设置了重试，retries必然会设置此处，所以为"true"时，不用管
                        map.put(methodConfig.getName() + ".retries", "0");
                    }
                }

                List<ArgumentConfig> argumentConfigList = methodConfig.getArguments();
                if (argumentConfigList != null && argumentConfigList.size() > 0) {
                    for (ArgumentConfig argumentConfig : argumentConfigList) {
                        if (argumentConfig.getType() != null && argumentConfig.getType().length() > 0) { //设置了type类型
                            String methodName = methodConfig.getName();
                            Method[] methods = interfaceClass.getMethods();
                            if (methods != null && methods.length > 0) {
                                for (Method method : methods) {
                                    if (methodName.equals(method.getName())) { //找到配置的方法
                                        Class[] paramClazzArr = method.getParameterTypes();
                                        if (argumentConfig.getIndex() != -1) {
                                            //比较指定index中参数列表中的参数类型
                                            if (paramClazzArr[argumentConfig.getIndex()].getName().equals(argumentConfig.getType())) {
                                                appendParameters(map, argumentConfig, methodName + "." + argumentConfig.getIndex());
                                            } else {
                                                throw new IllegalArgumentException("argument index and type can not match ,type = " + argumentConfig.getType() + ",index = " + argumentConfig.getIndex());
                                            }
                                        } else {
                                            for (int i = 0; i < paramClazzArr.length; i++) {
                                                if (argumentConfig.getType().equals(paramClazzArr[i].getName())) { //相同类型的callback值设为一样
                                                    appendParameters(map, argumentConfig, methodName + "." + i );
                                                    if (argumentConfig.getIndex() != -1 && argumentConfig.getIndex() != i) {
                                                        throw new IllegalArgumentException("argument index and type can not match ,type = " + argumentConfig.getType() + ",index = " + argumentConfig.getIndex());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (argumentConfig.getIndex() != -1) {
                            appendParameters(map, argumentConfig, methodConfig.getName() + "." + argumentConfig.getIndex());
                        } else {
                            throw new IllegalArgumentException("argument config must set type or index");
                        }
                    }
                }
            }
        }

       if (ProtocolUtils.isGeneric(generic)) {
           map.put("generic", generic);
           map.put("methods", Constants.ANY_VALUE);
       } else {
           String revision = Version.getVersion(interfaceClass, version);
           if (revision != null && revision.length() > 0) {
               map.put("revision", revision);
           }
           String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
           if (methods == null || methods.length == 0) {
               logger.info("interfaceClass no methods :" + interfaceClass.getName());
               map.put("methods", Constants.ANY_VALUE);
           } else {
               map.put("methods", StringUtils.join(methods, ",")); //将方法名分隔拼接
           }
       }
       if (!StringUtils.isEmpty(token)) {
           if (ConfigUtils.isDefault(token)) {
               map.put("token", UUID.randomUUID().toString());
           } else {
               map.put("token", token);
           }
       }
       if ("injvm".equals(protocolName)) {
           protocolConfig.setRegister(false);
           map.put("notify", "false");
       }
       String contextPath = protocolConfig.getContextpath(); //上线文路径
       if ((contextPath == null || contextPath.length() == 0) && provider != null) {
          contextPath = provider.getContextpath();
       }

       //通过获取上文条件，构造url
       URL url = new URL(protocolName, host, port, (((contextPath == null || contextPath.length() == 0) ? "" : contextPath + "/") + path), map);
       if (ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
               .hasExtension(url.getProtocol())) { //协议名：如dubbo
           url = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class).getExtension(url.getProtocol()).getConfigurator(url).configure(url);
       }
       String scope = url.getParameter(Constants.SCOPE_KEY); //常量值放前面，避免scope为null时，报空指针
       if (!Constants.SCOPE_NONE.equalsIgnoreCase(scope)) { //若scope没设置为null，则本地服务、远程服务都要暴露

           if (!Constants.SCOPE_REMOTE.equalsIgnoreCase(scope)) {
               exportLocal(url);
           }
           if (!Constants.SCOPE_LOCAL.equals(scope)) {
               //logger.warn("暴露远程服务：interface:" + interfaceClass.getName() + ", to url :" + url.toFullString());
               if (logger.isInfoEnabled()) {
                   logger.info("Export dubbo service " + interfaceClass.getName() + " to url " + url);
               }

               if (registryURLs != null && registryURLs.size() > 0
                     && url.getParameter("register", true)) {
                   for (URL registerUrl : registryURLs) {
                       url.addParameterIfAbsent("dynamic", registerUrl.getParameter("dynamic"));
                       URL monitorUrl = loadMonitor(registerUrl); //判断是否配置监控中心
                       if (monitorUrl != null) {
                           url.addParameterAndEncoded(Constants.MONITOR_KEY, monitorUrl.toFullString());
                       }
                       // 存在注册url，根据注册url创建invoker，把暴露的url做用EXPORT_KEY的值，附加到register的url中
                       Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class)interfaceClass, registerUrl.addParameterAndEncoded(Constants.EXPORT_KEY, url.toFullString()));
                       Exporter<?> exporter = protocol.export(invoker); //因为协议是以registry开头的，所以先暴露RegistryProtocol协议
                       exporters.add(exporter);
                   }
               } else {
                   Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, url);
                   Exporter<?> exporter = protocol.export(invoker);
                   exporters.add(exporter);
               }
           }
       }

       logger.info("url self create, ExportUrl for protocol:" + url.toFullString());
       this.urls.add(url);

    }


    // ---------end----------
}
