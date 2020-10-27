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
package com.alibaba.dubbo.registry.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AbstractRegistry. (SPI, Prototype, ThreadSafe)
 *
 * @author chao.liuc
 * @author william.liangf
 */
public abstract class AbstractRegistry implements Registry { //将公共信息放到抽象类，供子类调用

    // URL地址分隔符，用于文件缓存中，服务提供者URL分隔
    private static final char URL_SEPARATOR = ' ';
    // URL地址分隔正则表达式，用于解析文件缓存中服务提供者URL列表
    private static final String URL_SPLIT = "\\s+";
    // 日志输出
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    // 本地磁盘缓存，其中特殊的key值.registies记录注册中心列表，其它均为notified服务提供者列表 (从缓存文件中，读取属性写到Properties)
    private final Properties properties = new Properties();
    // 文件缓存定时写入（线程为SaveProperties）
    private final ExecutorService registryCacheExecutor = Executors.newFixedThreadPool(1, new NamedThreadFactory("DubboSaveRegistryCache", true));
    //是否同步保存文件（若是异步，则用线程池）
    private final boolean syncSaveFile;
    private final AtomicLong lastCacheChanged = new AtomicLong();
    private final Set<URL> registered = new ConcurrentHashSet<URL>(); /**@c 需要注册的数据 */
    private final ConcurrentMap<URL, Set<NotifyListener>> subscribed = new ConcurrentHashMap<URL, Set<NotifyListener>>(); /**@c 订阅、取消订阅，一个主题URL被多个监听者NotifyListener监听 */
    private final ConcurrentMap<URL, Map<String, List<URL>>> notified = new ConcurrentHashMap<URL, Map<String, List<URL>>>(); /**@c 通知的集合 */
    private URL registryUrl;
    // 本地磁盘缓存文件 history 此处的值需要调试下
    private File file;

    private AtomicBoolean destroyed = new AtomicBoolean(false);

    /**
     * dubbo本地缓存文件地址
     * 加载时从本地文件中读取配置，加载到properties中，并通知notify 相关的回路url
     */
    public AbstractRegistry(URL url) {
        setUrl(url);
        // 启动文件保存定时器（同步或异步保存）
        syncSaveFile = url.getParameter(Constants.REGISTRY_FILESAVE_SYNC_KEY, false); //此处的属性 save.file在哪里设置？ 解：url中设置
        /**
         * 1) 本地文件存储路径如： /Users/chenshengyong/.dubbo/dubbo-registry-localhost.cache
         * 2) 文件中的内容格式：com.csy.dubbo.provider.api.test.ApiDemo\:1.0.0=empty\://192.168.0.101/com.csy.dubbo.provider.api.test.ApiDemo?application......
         *    接口名=暴露的url
         */
        String filename = url.getParameter(Constants.FILE_KEY, System.getProperty("user.home") + "/.dubbo/dubbo-registry-" + url.getHost() + ".cache");
        File file = null;
        if (ConfigUtils.isNotEmpty(filename)) { //文件名：/Users/chenshengyong/.dubbo/dubbo-registry-localhost.cache
            file = new File(filename);
            if (!file.exists() && file.getParentFile() != null && !file.getParentFile().exists()) { //文件不存在，并且父目录不存在
                if (!file.getParentFile().mkdirs()) { //创建父目录：如 /Users/chenshengyong/.dubbo
                    throw new IllegalArgumentException("Invalid registry store file " + file + ", cause: Failed to create directory " + file.getParentFile() + "!");
                }
            }
        }
        this.file = file; //文件是在哪里写入的？ 解：上文通过new File创建的
        loadProperties(); //加载文件中的值，写到proprties，做本地缓存
        notify(url.getBackupUrls());
    }

    protected static List<URL> filterEmpty(URL url, List<URL> urls) {
        if (urls == null || urls.size() == 0) {
            List<URL> result = new ArrayList<URL>(1);
            result.add(url.setProtocol(Constants.EMPTY_PROTOCOL));
            return result;
        }
        return urls;
    }

    public URL getUrl() {
        return registryUrl;
    }

    protected void setUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("registry url == null");
        }
        this.registryUrl = url;
    }

    public Set<URL> getRegistered() {
        return registered;
    }

    public Map<URL, Set<NotifyListener>> getSubscribed() {
        return subscribed;
    }

    public Map<URL, Map<String, List<URL>>> getNotified() {
        return notified;
    }

    public File getCacheFile() {
        return file;
    }

    public Properties getCacheProperties() {
        return properties;
    }

    public AtomicLong getLastCacheChanged() {
        return lastCacheChanged;
    }

    /**
     * 将属性列表保存到文件中 (读取.cache中的属性，并附加上本地缓存的Properties，写到到.lock文件中)
     * 1）若当前版本最近变更的版本，则不处理（乐观锁处理）
     * 2）若本地缓存文件为空，则不处理
     * 3）构建文件输入流，从输入流中读取属性列表，写入到Properties中
     *   3.1）若失败则抛异常，"从本地存储文件中加载属性失败"
     *   3.2）在try/catch执行完后finally，文件输入流
     * 4）将本地缓存的Properties也putAll全部写到新建的Properties中
     * 5）在本地缓存文件的路径下创建".lock"文件（若文件不存在则创建）
     * 6）创建随机访问文件RandomAccessFile，从文件中获取文件通道FileChannel，
     *    并进行加锁处理channel.tryLock()，若不能获取到锁，则抛异常
     * 7）构建文件输出流，将新键的Properties存储到文件中
     *    7.1）关闭文件输出流FileOutputStream、释放锁lock、关闭通道channel、关闭随机访问文件等
     *    7.2）若失败异常
     *     7.2.1）版本号落后与最近版本号，则不处理
     *     7.2.2）若版本号高于最近版本号，则创建保存属性的线程SaveProperties，异步执行
     */
    public void doSaveProperties(long version) {
        if (version < lastCacheChanged.get()) { /**@c 版本号比较，乐观锁处理 */
            return;
        }
        if (file == null) {
            return;
        }
        Properties newProperties = new Properties();
        // 保存之前先读取一遍，防止多个注册中心之间冲突
        InputStream in = null;
        try {
            if (file.exists()) { //若文件存在，将之前的配置加载到属性配置中，保留老的配置，在老的配置上增加配置
                in = new FileInputStream(file);
                newProperties.load(in);
            }
        } catch (Throwable e) {
            logger.warn("Failed to load registry store file, cause: " + e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
        // 保存
        try {
            newProperties.putAll(properties);
            File lockfile = new File(file.getAbsolutePath() + ".lock");
            if (!lockfile.exists()) {
                lockfile.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(lockfile, "rw");
            try {
                FileChannel channel = raf.getChannel();
                try {
                    FileLock lock = channel.tryLock();
                    if (lock == null) {
                        throw new IOException("Can not lock the registry cache file " + file.getAbsolutePath() + ", ignore and retry later, maybe multi java process use the file, please config: dubbo.registry.file=xxx.properties");
                    }
                    // 保存
                    try {
                        if (!file.exists()) { //创建本地缓存文件,如../dubbo-registry-localhost.cache
                            file.createNewFile();
                        }
                        FileOutputStream outputFile = new FileOutputStream(file);
                        try {
                            newProperties.store(outputFile, "Dubbo Registry Cache");
                        } finally {
                            outputFile.close();
                        }
                    } finally {
                        lock.release();
                    }
                } finally {
                    channel.close();
                }
            } finally {
                raf.close();
            }
        } catch (Throwable e) {
            if (version < lastCacheChanged.get()) {
                return;
            } else {
                registryCacheExecutor.execute(new SaveProperties(lastCacheChanged.incrementAndGet()));
            }
            logger.warn("Failed to save registry store file, cause: " + e.getMessage(), e);
        }
    }

    private void loadProperties() {
        if (file != null && file.exists()) { //若存在文件，如：/Users/chenshengyong/.dubbo/dubbo-registry-localhost.cache，则从文件中读取属性值
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                properties.load(in);/**@c 从本地文件中中加载属性 */
                if (logger.isInfoEnabled()) {
                    logger.info("Load registry store file " + file + ", data: " + properties);
                }
            } catch (Throwable e) {
                logger.warn("Failed to load registry store file " + file, e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        }
    }

    /**
     * 从本地缓存Properties中获取到url中serviceKey对应的服务url列表List<URL>
     * 1）遍历缓存Properties的所有键key
     * 2）获取到key、value
     * 3）若key不为空，并且key的值与服务serviceKey相等，并且首字母是字符或是下划线，并且值不等于空
     *   3.1）将符合条件的value按给定的分隔符分隔
     *   3.2）遍历分隔后得到的数组，加到url列表中，并返回url列表
     */
    public List<URL> getCacheUrls(URL url) {/**@c 缓存在内容的值*/
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();/**@c 存储URL字符串 */
            if (key != null && key.length() > 0 && key.equals(url.getServiceKey())
                    && (Character.isLetter(key.charAt(0)) || key.charAt(0) == '_')
                    && value != null && value.length() > 0) {
                String[] arr = value.trim().split(URL_SPLIT);
                List<URL> urls = new ArrayList<URL>();
                for (String u : arr) {
                    urls.add(URL.valueOf(u));
                }
                return urls;
            }
        }
        return null;
    }

    /**
     * 查询符合条件的已注册数据
     * 1）从ConcurrentMap<URL, Map<String, List<URL>>>集合中根据url获取对应的Map
     * 2）如果映射的notifiedUrls不为空，做循环处理
     *   2.1）遍历通知的url列表notifiedUrls.values()
     *     2.1.1）遍历urls列表
     *       2.1.1.1）若url的协议不为空，则加入到结果url列表
     * 3）若映射的notifiedUrls为空
     *   3.1）映射的notifiedUrls为空
     *     3.1.1）创建原则引用类AtomicReference，
     *            创建通知监听器NotifyListener
     *     3.1.2）做订阅操作subscribe(url, listener)
     *        3.1.2.1）从引用中获取url列表reference.get()
     *         3.1.2.1.1）循环遍历url列表，将空协议的url去掉
     */
    public List<URL> lookup(URL url) {
        List<URL> result = new ArrayList<URL>();
        Map<String, List<URL>> notifiedUrls = getNotified().get(url);
        if (notifiedUrls != null && notifiedUrls.size() > 0) {
            for (List<URL> urls : notifiedUrls.values()) {
                for (URL u : urls) {
                    if (!Constants.EMPTY_PROTOCOL.equals(u.getProtocol())) {
                        result.add(u);
                    }
                }
            }
        } else {
            final AtomicReference<List<URL>> reference = new AtomicReference<List<URL>>();
            NotifyListener listener = new NotifyListener() {
                public void notify(List<URL> urls) {
                    reference.set(urls);
                }
            };
            //history-h1 此处订阅的用途？
            subscribe(url, listener); // 订阅逻辑保证第一次notify后再返回
            List<URL> urls = reference.get();
            if (urls != null && urls.size() > 0) {
                for (URL u : urls) {
                    if (!Constants.EMPTY_PROTOCOL.equals(u.getProtocol())) {
                        result.add(u);
                    }
                }
            }
        }
        return result;
    }

    public void register(URL url) {/**@c 注册数据，添加订阅者 */
        if (url == null) {
            throw new IllegalArgumentException("register url == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Register: " + url);
        }
        registered.add(url);
    }

    public void unregister(URL url) {/**@c 取消注册，移除订阅者 */
        if (url == null) {
            throw new IllegalArgumentException("unregister url == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Unregister: " + url);
        }
        registered.remove(url);
    }

    /**
     * 订阅： 将监听器加到url对应的集合中
     * @param url      订阅条件，不允许为空，如：consumer://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     * @param listener 变更事件监听器，不允许为空
     */

    /**
     * 将监听者添加到url对应的监听者集合中
     * 1）对URL、NotifyListener进行非空判断
     * 2）从订阅缓存ConcurrentMap<URL, Set<NotifyListener>>中获取到url对应的监听者列表
     *  2.1）若监听列表为空，则初始化监听者列表，并设置对url对应的缓存中
     */
    public void subscribe(URL url, NotifyListener listener) {/**@c 一个URL对应多个NotifyListener */
        if (url == null) {
            throw new IllegalArgumentException("subscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("subscribe listener == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Subscribe: " + url);
        }
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners == null) {
            subscribed.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
            listeners = subscribed.get(url);
        }

        // 将监听者添加到url对应的监听者集合中
        listeners.add(listener); //此处添加后，会影响ConcurrentMap<URL, Set<NotifyListener>>中url对应的集合
    }

    /**
     * 取消订阅，将监听器从集合中移除
     */
    public void unsubscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("unsubscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("unsubscribe listener == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Unsubscribe: " + url);
        }
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    protected void recover() throws Exception {/**@c history-h1 恢复什么*/
        // register
        Set<URL> recoverRegistered = new HashSet<URL>(getRegistered());
        if (!recoverRegistered.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover register url " + recoverRegistered);
            }
            for (URL url : recoverRegistered) {
                register(url);
            }
        }
        // subscribe
        Map<URL, Set<NotifyListener>> recoverSubscribed = new HashMap<URL, Set<NotifyListener>>(getSubscribed());
        if (!recoverSubscribed.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover subscribe url " + recoverSubscribed.keySet());
            }
            for (Map.Entry<URL, Set<NotifyListener>> entry : recoverSubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    subscribe(url, listener);
                }
            }
        }
    }

    protected void notify(List<URL> urls) {
        if (urls == null || urls.isEmpty()) return;

        /**
         * 遍历订阅的集合subscribed
         */
        for (Map.Entry<URL, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            URL url = entry.getKey();

            if (!UrlUtils.isMatch(url, urls.get(0))) { // history-v1 为啥url只和列表的第一个比较？
                continue;
            }

            Set<NotifyListener> listeners = entry.getValue();
            if (listeners != null) {
                for (NotifyListener listener : listeners) {/**@c 依次通知监听者 */
                    try {
                        notify(url, listener, filterEmpty(url, urls));
                    } catch (Throwable t) {
                        logger.error("Failed to notify registry event, urls: " + urls + ", cause: " + t.getMessage(), t);
                    }
                }
            }
        }
    }

    /**
     * NotifyListener通知已注册URL列表，并且对url中的服务做本地缓存以及缓存文件的写入
     * 1）判断URL、NotifyListener参数是否为空，若为空则抛出非法参数异常
     *    判断通知的url列表是否为空，若为空则终止后续的操作，因为没有通知的url列表
     * 2）若普通日志开启，则打印出日志
     * 3）遍历需要通知的provider的url列表
     *   3.1）判断消者的consumer Url, 与提供者的provider Url是否匹配
     *       3.1.1）若匹配UrlUtils.isMatch为true，获取提供者provider url中"category"的值
     *         3.1.1.1）从Map<String, List<URL>> 获取category对应的url列表
     *         3.1.1.2）从分类列表categoryList为空，创建分类列表，并设置到Map<String, List<URL>>中
     * 4）若处理的Map<String, List<URL>>为空，则直接返回
     * 5）ConcurrentMap<URL, Map<String, List<URL>>> 获取url对应的分类通知Map中Map<String, List<URL>> categoryNotified
     *    若分类通知categoryNotified 中Map为空，则初始化Map
     * 6）遍历分类对应的Map  result.entrySet()
     *    6.1）获取分类category、以及分类对应的url列表categoryList，放入到ConcurrentMap<URL, Map<String, List<URL>>> 已通知过的列表notified
     *    6.2）将url中的通知列表写到本地缓存Property、并写到本地缓存文件.caces， .lock中
     *    6.3）listener通知分类的url列表
     */
    protected void notify(URL url, NotifyListener listener, List<URL> urls) {
        if (url == null) {
            throw new IllegalArgumentException("notify url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }
        if ((urls == null || urls.size() == 0)
                && !Constants.ANY_VALUE.equals(url.getServiceInterface())) {
            logger.warn("Ignore empty notify urls for subscribe url " + url);
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Notify urls for subscribe url " + url + ", urls: " + urls);
        }
        Map<String, List<URL>> result = new HashMap<String, List<URL>>();
        for (URL u : urls) { //构建需要通知的URL列表
            if (UrlUtils.isMatch(url, u)) {
                String category = u.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
                List<URL> categoryList = result.get(category);
                if (categoryList == null) {
                    categoryList = new ArrayList<URL>();
                    result.put(category, categoryList);
                }
                categoryList.add(u);
            }
        }
        if (result.size() == 0) {
            return;
        }
        Map<String, List<URL>> categoryNotified = notified.get(url);
        if (categoryNotified == null) {
            notified.putIfAbsent(url, new ConcurrentHashMap<String, List<URL>>());
            categoryNotified = notified.get(url);
        }
        for (Map.Entry<String, List<URL>> entry : result.entrySet()) {
            String category = entry.getKey();
            List<URL> categoryList = entry.getValue();
            categoryNotified.put(category, categoryList); //通知的分类以及分类下的URL列表
            saveProperties(url);
            listener.notify(categoryList); //通知
        }
    }

    /**
     * 将url对应的通知列表写入本地缓存Properties中，并且也写到本地缓存文件中
     * 1）若本地磁盘缓存文件为空，则不处理
     * 2）从ConcurrentMap<URL, Map<String, List<URL>>> 获取url对应的分类通知列表
     *    若通知的分类map不为空，遍历通知的url列表
     *    将多个url按分隔符进行分隔
     * 3）将服务serviceKey与对应的拼接字符串buf，存入Properties
     * 4）将版本号lastCacheChanged递增
     * 5）是否同步保存文件
     *  5.1）同步保存文件 doSaveProperties(version)
     *  5.2）异步保存文件 ExecutorService registryCacheExecutor
     */
    private void saveProperties(URL url) {
        if (file == null) {
            return;
        }

        try {
            StringBuilder buf = new StringBuilder();
            Map<String, List<URL>> categoryNotified = notified.get(url);
            // 将按分类通知的URL，进行拼接，并按分隔符分隔
            if (categoryNotified != null) {
                for (List<URL> us : categoryNotified.values()) {
                    for (URL u : us) {
                        if (buf.length() > 0) {
                            buf.append(URL_SEPARATOR);
                        }
                        buf.append(u.toFullString());
                    }
                }
            }
            // 将服务key作为属性键key，将需要通知的url字符串作为属性值value
            properties.setProperty(url.getServiceKey(), buf.toString()); //本地缓存文件中存储的内容，dubbo-registry-localhost.cache，键值对的
            // 每次变更都要将版本号加一，不管内容是否有变更
            long version = lastCacheChanged.incrementAndGet();
            // 同步保存或异步保存（异步使用线程池）
            if (syncSaveFile) {
                doSaveProperties(version);
            } else {
                registryCacheExecutor.execute(new SaveProperties(version));
            }
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    public void destroy() {/**@c 取消注册及取消订阅 */
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }

        if (logger.isInfoEnabled()) {
            logger.info("Destroy registry:" + getUrl());
        }
        Set<URL> destroyRegistered = new HashSet<URL>(getRegistered());
        if (!destroyRegistered.isEmpty()) {
            for (URL url : new HashSet<URL>(getRegistered())) {
                if (url.getParameter(Constants.DYNAMIC_KEY, true)) {
                    try {
                        unregister(url);
                        if (logger.isInfoEnabled()) {
                            logger.info("Destroy unregister url " + url);
                        }
                    } catch (Throwable t) {
                        logger.warn("Failed to unregister url " + url + " to registry " + getUrl() + " on destroy, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
        Map<URL, Set<NotifyListener>> destroySubscribed = new HashMap<URL, Set<NotifyListener>>(getSubscribed());
        if (!destroySubscribed.isEmpty()) {
            for (Map.Entry<URL, Set<NotifyListener>> entry : destroySubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    try {
                        unsubscribe(url, listener);
                        if (logger.isInfoEnabled()) {
                            logger.info("Destroy unsubscribe url " + url);
                        }
                    } catch (Throwable t) {
                        logger.warn("Failed to unsubscribe url " + url + " to registry " + getUrl() + " on destroy, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
    }

    public String toString() {
        return getUrl().toString();
    }

    /**
     * 将属性保存到文件的线程
     */
    private class SaveProperties implements Runnable { //保存文件线程
        private long version;

        private SaveProperties(long version) {
            this.version = version;
        }

        public void run() {
            doSaveProperties(version);
        }
    }

}