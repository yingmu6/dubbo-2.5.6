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
package com.alibaba.dubbo.registry.integration;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryFactory;
import com.alibaba.dubbo.registry.RegistryService;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.cluster.Configurator;
import com.alibaba.dubbo.rpc.protocol.InvokerWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RegistryProtocol
 *
 * @author william.liangf
 * @author chao.liuc
 */
public class RegistryProtocol implements Protocol {

    private final static Logger logger = LoggerFactory.getLogger(RegistryProtocol.class);
    private static RegistryProtocol INSTANCE;
    private final Map<URL, NotifyListener> overrideListeners = new ConcurrentHashMap<URL, NotifyListener>(); //URL与通知监听器的映射
    //用于解决rmi重复暴露端口冲突的问题，已经暴露过的服务不再重新暴露
    //providerurl <--> exporter
    private final Map<String, ExporterChangeableWrapper<?>> bounds = new ConcurrentHashMap<String, ExporterChangeableWrapper<?>>(); //需要暴露协议的url与ExporterChangeableWrapper的映射，url的值如：dubbo://192.168.1.102:20881/com.alibaba.dubbo.demo.ApiDemo....
    private Cluster cluster;
    private Protocol protocol;
    private RegistryFactory registryFactory; //history-h1 注册工厂何处被实例的？
    private ProxyFactory proxyFactory;

    public RegistryProtocol() {
        INSTANCE = this;
    }

    public static RegistryProtocol getRegistryProtocol() { //初始化实例 RegistryProtocol
        if (INSTANCE == null) {
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(Constants.REGISTRY_PROTOCOL); // load
        }
        return INSTANCE;
    }

    //过滤URL中不需要输出的参数(以点号开头的)
    private static String[] getFilteredKeys(URL url) {
        Map<String, String> params = url.getParameters();
        if (params != null && !params.isEmpty()) {
            List<String> filteredKeys = new ArrayList<String>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry != null && entry.getKey() != null && entry.getKey().startsWith(Constants.HIDE_KEY_PREFIX)) {
                    filteredKeys.add(entry.getKey());
                }
            }
            return filteredKeys.toArray(new String[filteredKeys.size()]);
        } else {
            return new String[]{};
        }
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setRegistryFactory(RegistryFactory registryFactory) {
        this.registryFactory = registryFactory;
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    public int getDefaultPort() {
        return 9090;
    }

    public Map<URL, NotifyListener> getOverrideListeners() {
        return overrideListeners;
    }

    /**
     * 暴露注册协议 是怎么先到RegistryProtocol，然后再到Dubbo的？在什么地方选择的？
     * 解：若配置了注册中心时，组装暴露的url时，会先把协议名置为registry，并把要暴露的协议放在registry的url中，键位export
     * 根据自适应扩展时加载选择的实例，一开始时registry协议名
     *
     * 注册中心注册、并订阅节点
     */

    /**
     * 注册协议暴露（将invoker转换位exporter的过程） -- 代码流程
     * originInvoker中url的值
     * registry://localhost:2181/com.alibaba.dubbo.registry.RegistryService?application=api_demo&dubbo=2.0.0&
     * export=dubbo%3A%2F%2F192.163.103.104%3A20881%2Fcom.alibaba.dubbo.demo.ApiDemo%3Fanyhost%3Dtrue%26application%3Dapi_demo
     * %26delay%3D5%26dubbo%3D2.0.0%26export%3Dtrue%26generic%3Dfalse%26interface%3Dcom.alibaba.dubbo.demo.ApiDemo%26methods
     * %3DsayHello%2CsayApi%26pid%3D56627%26sayApi.0.callback%3Dfalse%26sayApi.3.callback%3Dfalse%26sayApi.retries%3D0%26
     * sayApi.timeout%3D3000%26service.filter%3DselfFilter%26side%3Dprovider%26timeout%3D3000%26timestamp%3D1591272337789&
     * pid=56627&registry=zookeeper&timestamp=1591272337777
     *
     * 1）暴露指定invoker对应的export值的服务，获取到暴露者 ExporterChangeableWrapper
     * 2）获取invoker对应的注册实例
     */
    public <T> Exporter<T> export(final Invoker<T> originInvoker) throws RpcException { //originInvoker的值注册协议：registry://localhost:2181/com.alibaba.dubbo.registry.RegistryService?application=api_demo&dubbo=2.0.0&export=dubbo%3A%2F%2F10.118.32.69%3A20881%2Fcom.alibaba.dubbo.demo....
        //export invoker(使用dubbo协议暴露)
        final ExporterChangeableWrapper<T> exporter = doLocalExport(originInvoker); //本地服务暴露，比如DubboProtocol中export
        //registry provider（获取注册中心） ，若注册中心是zookeeper，则此处的实例为ZookeeperRegistry
        final Registry registry = getRegistry(originInvoker); //从缓存中获取注册实例，若没有则创建
        // 通过invoker的url 获取 providerUrl的地址
        final URL registedProviderUrl = getRegistedProviderUrl(originInvoker);
        // 通知注册中心发布服务(写到指定目录下) : 创建节点(持久节点 + 临时节点) 比如 /dubbo/xxx.service/providers/dubbo://....
        registry.register(registedProviderUrl); //过滤掉部分参数的url ： dubbo://192.168.1.103:20881/com.alibaba.dubbo.demo.ApiDemo?anyhost=true&application=....
        // 订阅override数据（由dubbo协议转换为provider协议）
        // System-t0d0（需要修复的代码） 提供者订阅时，会影响同一JVM即暴露服务，又引用同一服务的的场景，因为subscribed以服务名为缓存的key，导致订阅信息覆盖。
        final URL overrideSubscribeUrl = getSubscribedOverrideUrl(registedProviderUrl);
        // 监听器的使用, overrideSubscribeUrl如：provider://192.168.1.103:20881/com.alibaba.dubbo.demo.ApiDemo?anyhost=true&application=api_demo&category=configurators&check=false...
        final OverrideListener overrideSubscribeListener = new OverrideListener(overrideSubscribeUrl, originInvoker);
        overrideListeners.put(overrideSubscribeUrl, overrideSubscribeListener); //设置映射关系
        //订阅节点
        registry.subscribe(overrideSubscribeUrl, overrideSubscribeListener); //订阅/dubbo/*/configrators节点
        //保证每次export都返回一个新的exporter实例
        return new Exporter<T>() {
            public Invoker<T> getInvoker() {
                return exporter.getInvoker();
            }

            public void unexport() {
                try {
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
                try {
                    registry.unregister(registedProviderUrl);
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
                try {
                    overrideListeners.remove(overrideSubscribeUrl);
                    registry.unsubscribe(overrideSubscribeUrl, overrideSubscribeListener);
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        };
    }

    /**
     * 暴露本地服务（暴露注册url中export对应值的服务，registry://localhost:2181/com.alibaba.dubbo.registry.RegistryService?export=dubbo%3A%2F%2F192.163.103.104....）
     * 1）根据invoker获取缓存中的key（即为export暴露服务的url去掉指定参数对应的字符串， 约定Map中的key，以哪种形式存，就用哪种形式取）
     * 2）从Map<String, ExporterChangeableWrapper<?>> bounds 集合中获取指定key的暴露者ExporterChangeableWrapper
     * 3）对获取的exporter进行判断
     *   3.1）若exporter为空，对bounds进行同步资源锁定
     *       双重判断exporter是否为空，若为空，做相关构建
     *      3.1.1）通过传入的invoker，以及invoker中url对应export提供者url，构建静态内部类InvokerDelegete（invoker的代理类）
     *      3.1.2）通过InvokerDelegete进行协议暴露，获取到export
     *      3.1.3）通过Exporter, Invoker构建ExporterChangeableWrapper（export的代理类），并赋值给exporter
     *      3.1.3）将创建好的exporter写入export 本地绑定的map中 bounds
     *   3.2）若exporter不为空，直接返回exporter
     */
    @SuppressWarnings("unchecked")
    private <T> ExporterChangeableWrapper<T> doLocalExport(final Invoker<T> originInvoker) {
        String key = getCacheKey(originInvoker); // 获取要暴露的协议url（移除了dynamic、enabled参数），如 dubbo://...
        ExporterChangeableWrapper<T> exporter = (ExporterChangeableWrapper<T>) bounds.get(key); //从缓存中获取export的实现类ExporterChangeableWrapper
        if (exporter == null) {
            synchronized (bounds) {
                exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
                if (exporter == null) { //使用接口回调，接口调用具体实现类的方法
                    final Invoker<?> invokerDelegete = new InvokerDelegete<T>(originInvoker, getProviderUrl(originInvoker));
                    exporter = new ExporterChangeableWrapper<T>((Exporter<T>) protocol.export(invokerDelegete), originInvoker); //本地暴露
                    bounds.put(key, exporter);
                }
            }
        }
        return exporter;
    }

    /**
     * 对修改了url的invoker重新export
     *
     * @param originInvoker
     * @param newInvokerUrl
     */
    @SuppressWarnings("unchecked")
    private <T> void doChangeLocalExport(final Invoker<T> originInvoker, URL newInvokerUrl) {
        String key = getCacheKey(originInvoker);
        final ExporterChangeableWrapper<T> exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
        if (exporter == null) {
            logger.warn(new IllegalStateException("error state, exporter should not be null"));
        } else {
            final Invoker<T> invokerDelegete = new InvokerDelegete<T>(originInvoker, newInvokerUrl);
            exporter.setExporter(protocol.export(invokerDelegete));
        }
    }

    /**
     * 根据invoker的地址获取registry实例（获取具体的注册实例，如：由registry -> zookeeper等）
     * 1）从invoker中获取到URL
     * 2）若URL中的协议名是registry
     *   2.1）从URL中获取参数registry对应的注册协议，一般为zookeeper，默认dubbo（注册协议有多种，redis、multicase、zookeeper，dubbo）
     *   2.2）设置registryUrl的protocol，实现URL中的协议转换，并且移除registryUrl的registry参数
     *   2.3）通过注册工厂获取注册实例
     */
    private Registry getRegistry(final Invoker<?> originInvoker) {
        URL registryUrl = originInvoker.getUrl();
        if (Constants.REGISTRY_PROTOCOL.equals(registryUrl.getProtocol())) {
            String protocol = registryUrl.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_DIRECTORY); //注册协议,zookeeper
            registryUrl = registryUrl.setProtocol(protocol).removeParameter(Constants.REGISTRY_KEY); //将注册协议 由registry -> zookeeper，并且移除registry实例
        }
        return registryFactory.getRegistry(registryUrl); //扩展名url.getProtocol()取到zookeeper，所以RegistryFactory的实例为ZookeeperRegistryFactory
    }

    /**
     * 返回注册到注册中心的URL，对URL参数进行一次过滤
     *
     * @param originInvoker
     * @return
     */
    private URL getRegistedProviderUrl(final Invoker<?> originInvoker) {
        URL providerUrl = getProviderUrl(originInvoker);
        //注册中心看到的地址
        final URL registedProviderUrl = providerUrl.removeParameters(getFilteredKeys(providerUrl)).removeParameter(Constants.MONITOR_KEY);
        return registedProviderUrl;
    }

    /**
     * 将协议名由dubbo -> provider
     * 并添加参数 category、check参数
     */
    private URL getSubscribedOverrideUrl(URL registedProviderUrl) { //入参如：dubbo://192.168.1.103:20881/com.alibaba.dubbo.demo.ApiDemo?anyhost=true...
        return registedProviderUrl.setProtocol(Constants.PROVIDER_PROTOCOL) //设置协议
                .addParameters(Constants.CATEGORY_KEY, Constants.CONFIGURATORS_CATEGORY,
                        Constants.CHECK_KEY, String.valueOf(false));  //添加两个参数CATEGORY_KEY、CHECK_KEY
    }

    /**
     * 通过invoker的url 获取 providerUrl的地址（即键export对应的值）
     * 1）获取invoker的url（invoker是Node的子接口，可以通过getUrl()获取到URL）
     * 2）获取url中export的值，并且解码。因为export的值被编码过的，需要对应解码
     * 3）从url解析出来的export值为空，表明是非法的url，抛出非法参数异常
     * 4）通过export的值构建url，这个值是完整的url
     */
    private URL getProviderUrl(final Invoker<?> origininvoker) { //获取提供者url registry://localhost:2181/.../export=dubbo%3A%2F%2F10.118.32.69%3A20881%2
        String export = origininvoker.getUrl().getParameterAndDecoded(Constants.EXPORT_KEY); //url中键对应的值被编码过，所以需要解码
        if (export == null || export.length() == 0) { //解码后的url dubbo://10.118.32.69:20881/com.alibaba.dubbo.demo.ApiDemo?anyhost=true&application=api_demo&delay=5&dubbo=2.0.0&export=true...
            throw new IllegalArgumentException("The registry export url is null! registry: " + origininvoker.getUrl());
        }

        URL providerUrl = URL.valueOf(export); //注册协议中export对应的协议url，如：dubbo://10.118.32.69:20881/com.alibaba.dubbo.demo.ApiDemo?anyhost=true&application=api_demo&delay=5&dubbo=2.0.0&export=true....
        return providerUrl;
    }

    /**
     * 获取invoker在bounds中缓存的key
     * 1）获取invoker对应的url中export的对应的URL值，即需要暴露的服务的URL值
     * 2）从URL的参数集合中移除"dynamic", "enabled"键，并返回url对应的字符串
     */
    private String getCacheKey(final Invoker<?> originInvoker) {
        URL providerUrl = getProviderUrl(originInvoker);
        String key = providerUrl.removeParameters("dynamic", "enabled").toFullString(); //移除dynamic、enabled参数
        return key;
    }

    @SuppressWarnings("unchecked")
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        url = url.setProtocol(url.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_REGISTRY)).removeParameter(Constants.REGISTRY_KEY); //协议替换为,并且移除键registry zookeeper://localhost:2181/com.alibaba.dubbo.registry.RegistryService?application=...
        Registry registry = registryFactory.getRegistry(url); //通过工厂方法创建注册实例Registry
        if (RegistryService.class.equals(type)) { //history-h1 此处怎么会有相等的可能？RegistryServicer.class为com.alibaba.dubbo.registry.RegistryService，而暴露的接口为com.alibaba.dubbo.demo.ApiDemo
            return proxyFactory.getInvoker((T) registry, type, url);
        }

        // group="a,b" or group="*"
        Map<String, String> qs = StringUtils.parseQueryString(url.getParameterAndDecoded(Constants.REFER_KEY)); //将refer引用部分解析到map中
        String group = qs.get(Constants.GROUP_KEY);
        if (group != null && group.length() > 0) { //history-h1 接口分组
            if ((Constants.COMMA_SPLIT_PATTERN.split(group)).length > 1
                    || "*".equals(group)) {
                return doRefer(getMergeableCluster(), registry, type, url);
            }
        }
        return doRefer(cluster, registry, type, url);
    }

    private Cluster getMergeableCluster() {
        return ExtensionLoader.getExtensionLoader(Cluster.class).getExtension("mergeable");
    }

    private <T> Invoker<T> doRefer(Cluster cluster, Registry registry, Class<T> type, URL url) {
        RegistryDirectory<T> directory = new RegistryDirectory<T>(type, url);
        directory.setRegistry(registry);
        directory.setProtocol(protocol);
        URL subscribeUrl = new URL(Constants.CONSUMER_PROTOCOL, NetUtils.getLocalHost(), 0, type.getName(), directory.getUrl().getParameters());
        if (!Constants.ANY_VALUE.equals(url.getServiceInterface())
                && url.getParameter(Constants.REGISTER_KEY, true)) {
            registry.register(subscribeUrl.addParameters(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY,
                    Constants.CHECK_KEY, String.valueOf(false))); //subscribeUrl内容为 consumer://192.168.1.102/com.alibaba.dubbo.demo.ApiDemo?application=api_demo&dubbo=2.0.0&interface=com.alibaba.dubbo.demo.ApiDemo&methods=sayApi,sayHello&pid=32927&side=consumer&timestamp=1564673239256
        }
        directory.subscribe(subscribeUrl.addParameter(Constants.CATEGORY_KEY,
                Constants.PROVIDERS_CATEGORY
                        + "," + Constants.CONFIGURATORS_CATEGORY
                        + "," + Constants.ROUTERS_CATEGORY)); //第二个参数，"providers,configurators,routers"
        return cluster.join(directory);
    }

    public void destroy() {
        List<Exporter<?>> exporters = new ArrayList<Exporter<?>>(bounds.values());
        for (Exporter<?> exporter : exporters) {
            exporter.unexport();
        }
        bounds.clear();
    }

    public static class InvokerDelegete<T> extends InvokerWrapper<T> {
        private final Invoker<T> invoker;

        /**
         * @param invoker
         * @param url     invoker.getUrl返回此值
         */
        public InvokerDelegete(Invoker<T> invoker, URL url) {
            // 调用父类的构造函数，初始化Invoker、URL
            super(invoker, url);
            this.invoker = invoker;
        }

        /**
         * 获取invoker
         * 判断是否是InvokerDelegete的实例
         *   若是：强制转换到InvokerDelegete，并调用getInvoker() history 此处调用InvokerDelegete的getInvoker，又是invoker instanceof InvokerDelegete，递归调用，会不会死循环
         *   若不是：直接返回invoker
         */
        public Invoker<T> getInvoker() {
            if (invoker instanceof InvokerDelegete) {
                return ((InvokerDelegete<T>) invoker).getInvoker();
            } else {
                return invoker;
            }
        }
    }

    /*重新export 1.protocol中的exporter destory问题
     *1.要求registryprotocol返回的exporter可以正常destroy
     *2.notify后不需要重新向注册中心注册
     *3.export 方法传入的invoker最好能一直作为exporter的invoker.
     */
    private class OverrideListener implements NotifyListener {

        private final URL subscribeUrl;
        private final Invoker originInvoker;

        public OverrideListener(URL subscribeUrl, Invoker originalInvoker) {
            this.subscribeUrl = subscribeUrl;
            this.originInvoker = originalInvoker;
        }

        /**
         * @param urls 已注册信息列表，总不为空，含义同{@link com.alibaba.dubbo.registry.RegistryService#lookup(URL)}的返回值。
         */
        public synchronized void notify(List<URL> urls) { //订阅/dubbo/*/configrators节点，当有变化时，触发OverrideListener监听器，重新执行OverrideListener#notify方法
            logger.debug("original override urls: " + urls);
            List<URL> matchedUrls = getMatchedUrls(urls, subscribeUrl);
            logger.debug("subscribe url: " + subscribeUrl + ", override urls: " + matchedUrls);
            //没有匹配的
            if (matchedUrls.isEmpty()) {
                return;
            }

            List<Configurator> configurators = RegistryDirectory.toConfigurators(matchedUrls);

            final Invoker<?> invoker;
            if (originInvoker instanceof InvokerDelegete) {
                invoker = ((InvokerDelegete<?>) originInvoker).getInvoker();
            } else {
                invoker = originInvoker;
            }
            //最原始的invoker
            URL originUrl = RegistryProtocol.this.getProviderUrl(invoker);
            String key = getCacheKey(originInvoker);
            ExporterChangeableWrapper<?> exporter = bounds.get(key);
            if (exporter == null) {
                logger.warn(new IllegalStateException("error state, exporter should not be null"));
                return;
            }
            //当前的，可能经过了多次merge
            URL currentUrl = exporter.getInvoker().getUrl();
            //与本次配置merge的
            URL newUrl = getConfigedInvokerUrl(configurators, originUrl);
            if (!currentUrl.equals(newUrl)) {
                RegistryProtocol.this.doChangeLocalExport(originInvoker, newUrl);
                logger.info("exported provider url changed, origin url: " + originUrl + ", old export url: " + currentUrl + ", new export url: " + newUrl);
            }
        }

        private List<URL> getMatchedUrls(List<URL> configuratorUrls, URL currentSubscribe) {
            List<URL> result = new ArrayList<URL>();
            for (URL url : configuratorUrls) {
                URL overrideUrl = url;
                // 兼容旧版本
                if (url.getParameter(Constants.CATEGORY_KEY) == null
                        && Constants.OVERRIDE_PROTOCOL.equals(url.getProtocol())) {
                    overrideUrl = url.addParameter(Constants.CATEGORY_KEY, Constants.CONFIGURATORS_CATEGORY);
                }

                //检查是不是要应用到当前服务上
                if (UrlUtils.isMatch(currentSubscribe, overrideUrl)) {
                    result.add(url);
                }
            }
            return result;
        }

        //合并配置的url
        private URL getConfigedInvokerUrl(List<Configurator> configurators, URL url) {
            for (Configurator configurator : configurators) {
                url = configurator.configure(url);
            }
            return url;
        }
    }

    /**
     * exporter代理,建立返回的exporter与protocol export出的exporter的对应关系，在override时可以进行关系修改.
     * @param <T>
     * @author chao.liuc
     */
    //私有内部类
    private class ExporterChangeableWrapper<T> implements Exporter<T> {

        private final Invoker<T> originInvoker;
        private Exporter<T> exporter;

        public ExporterChangeableWrapper(Exporter<T> exporter, Invoker<T> originInvoker) {
            this.exporter = exporter;
            this.originInvoker = originInvoker;
        }

        public Invoker<T> getOriginInvoker() {
            return originInvoker;
        }

        public Invoker<T> getInvoker() {
            return exporter.getInvoker();
        }

        public void setExporter(Exporter<T> exporter) {
            this.exporter = exporter;
        }

        public void unexport() {
            String key = getCacheKey(this.originInvoker);
            bounds.remove(key);
            exporter.unexport();
        }
    }
}