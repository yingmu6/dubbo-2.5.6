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
package com.alibaba.dubbo.common.utils;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ding.lid
 * @author william.liangf
 */
public class ConfigUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);
    private static Pattern VARIABLE_PATTERN = Pattern.compile(
            "\\$\\s*\\{?\\s*([\\._0-9a-zA-Z]+)\\s*\\}?");  /**@c \s匹配空白，\S匹配非空白 */
    private static volatile Properties PROPERTIES;
    private static int PID = -1;

    private ConfigUtils() {
    }

    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    public static boolean isEmpty(String value) {
        return value == null || value.length() == 0
                || "false".equalsIgnoreCase(value)
                || "0".equalsIgnoreCase(value)
                || "null".equalsIgnoreCase(value)
                || "N/A".equalsIgnoreCase(value);
    }

    public static boolean isDefault(String value) {
        return "true".equalsIgnoreCase(value)
                || "default".equalsIgnoreCase(value);
    }

    /**
     * 扩展点列表中插入缺省扩展点。
     * <p>
     * 扩展点列表支持<ul>
     * <li>特殊值<code><strong>default</strong></code>，表示缺省扩展点插入的位置
     * <li>特殊符号<code><strong>-</strong></code>，表示剔除。 <code>-foo1</code>，剔除添加缺省扩展点foo1。<code>-default</code>，剔除添加所有缺省扩展点。
     * </ul>
     *
     * @param type 扩展点类型
     * @param cfg  扩展点名列表
     * @param def  缺省的扩展点的列表
     * @return 完成缺省的扩展点列表插入后的列表
     */
    public static List<String> mergeValues(Class<?> type, String cfg, List<String> def) {
        List<String> defaults = new ArrayList<String>();
        if (def != null) {
            for (String name : def) {
                if (ExtensionLoader.getExtensionLoader(type).hasExtension(name)) {
                    defaults.add(name);
                }
            }
        }

        List<String> names = new ArrayList<String>();

        // 加入初始值
        String[] configs = (cfg == null || cfg.trim().length() == 0) ? new String[0] : Constants.COMMA_SPLIT_PATTERN.split(cfg);
        for (String config : configs) {
            if (config != null && config.trim().length() > 0) {
                names.add(config);
            }
        }

        // 不包含 -default
        if (!names.contains(Constants.REMOVE_VALUE_PREFIX + Constants.DEFAULT_KEY)) {
            // 加入 插入缺省扩展点
            int i = names.indexOf(Constants.DEFAULT_KEY);
            if (i > 0) {
                names.addAll(i, defaults);
            } else {
                names.addAll(0, defaults);
            }
            names.remove(Constants.DEFAULT_KEY);
        } else {
            names.remove(Constants.DEFAULT_KEY);
        }

        // 合并-的配置项
        for (String name : new ArrayList<String>(names)) {
            if (name.startsWith(Constants.REMOVE_VALUE_PREFIX)) {
                names.remove(name);
                names.remove(name.substring(1));
            }
        }
        return names;
    }

    /**@ 替换属性值 */
    public static String replaceProperty(String expression, Map<String, String> params) {
        if (expression == null || expression.length() == 0 || expression.indexOf('$') < 0) {
            return expression;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) { // 逐个匹配,app.na$me.proper$ties7788 会取值me.proper
            String key = matcher.group(1);
            String value = System.getProperty(key);
            if (value == null && params != null) {
                value = params.get(key);
            }
            if (value == null) {
                value = "";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 读取属性文件中的值，并写入到Properties中
     * 1）双重判定，若Properties为空
     *   1.1）通过System.getProperty获取文件名，dubbo.properties.file
     *     1.1.1）若文件名为空，通过System.getenv获取文件名"dubbo.properties.file"
     *       1.1.1.1）若文件名为空，返回默认文件名"dubbo.properties"
     *   1.2）通过ConfigUtils.loadProperties从指定的文件中获取到Properties属性值
     * 2）返回Properties属性值
     */
    public static Properties getProperties() { //读取dubbo的属性文件，并把键值对写到Properties类的属性中
        if (PROPERTIES == null) {
            synchronized (ConfigUtils.class) {
                if (PROPERTIES == null) {
                    String path = System.getProperty(Constants.DUBBO_PROPERTIES_KEY); //查找属性文件的路径，依次查找指定文件名的路径
                    if (path == null || path.length() == 0) {
                        path = System.getenv(Constants.DUBBO_PROPERTIES_KEY);
                        if (path == null || path.length() == 0) {
                            path = Constants.DEFAULT_DUBBO_PROPERTIES;
                        }
                    }
                    PROPERTIES = ConfigUtils.loadProperties(path, false, true);
                }
            }
        }
        return PROPERTIES;
    }

    public static void setProperties(Properties properties) {
        if (properties != null) {
            PROPERTIES = properties;
        }
    }

    public static void addProperties(Properties properties) {
        if (properties != null) {
            getProperties().putAll(properties);
        }
    }

    /**
     * 获取属性key对应的值
     *   默认值为null
     */
    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"}) /**@c 从System获取指定的值，如果不存在使用默认值 */
    /**
     * 获取属性key对应的值，若没有值，默认值为defaultValue
     * 1）从System系统中获取key对应的值
     *   1.1）若值不为空，则直接返回
     * 2）若从System系统中获取的属性值为空，则从属性文件中获取
     *   2.1）从dubbo.properties.file或dubbo.propertie获取
     *   2.2）先尝试本地文件中查找，若本地文件没有查到，则通过网络流从远程文件中获取
     *     （若允许出现多个文件，则将所有文件对应的属性值都进行加载）
     */
    public static String getProperty(String key, String defaultValue) {
        String value = System.getProperty(key); //此处为啥再从系统属性中查一次，都是系统属性中没值才去加载配置文件的？ 1）因为开发时查没有，但到此处时，可能其它的线程把值设置进入了。起再次确认的作用 2）还有单独调用的地方
        if (value != null && value.length() > 0) {
            return value;
        }
        /**@c Properties继承了HashTable 加了synchronized锁，线程安全的 */
        Properties properties = getProperties();
        return replaceProperty(properties.getProperty(key, defaultValue), (Map) properties);
    }

    public static Properties loadProperties(String fileName) {
        return loadProperties(fileName, false, false);
    }

    public static Properties loadProperties(String fileName, boolean allowMultiFile) {
        return loadProperties(fileName, allowMultiFile, false);
    }

    /**
     * Load properties file to {@link Properties} from class path.
     *
     * @param fileName       properties file name. for example: <code>dubbo.properties</code>, <code>METE-INF/conf/foo.properties</code>
     * @param allowMultiFile if <code>false</code>, throw {@link IllegalStateException} when found multi file on the class path.
     * @param optional       is optional. if <code>false</code>, log warn when properties config file not found!s
     * @return loaded {@link Properties} content. <ul>
     * <li>return empty Properties if no file found.
     * <li>merge multi properties file if found multi file
     * </ul>
     * @throws IllegalStateException not allow multi-file, but multi-file exsit on class path.
     */

    /**
     * 加载指定文件名的属性文件，并且写到Properties对象中  todo @csy_new 待调试
     * 1）若文件名以"/"开头，则按本地文件处理
     *   1.1）通过文件名，获取到文件输入流
     *   1.2）从输入流中读取到属性列表
     *   1.3）关闭流，并且返回属性Properties
     * 2）若文件名不是以"/"开头，分两种情况
     * （a：尝试使用类加载器加载文件，b：若不是本地文件，则使用网络流读取文件）
     *   2.1）获取类加载器，并且获取文件名对应资源
     *   2.2）判断url枚举，是否存在元素，若存在则加到url列表中
     * 3）若url列表为空并且是非可选optional的，则打印提醒warn日志
     * 4）若不允许多文件，文件超过1个时，则抛出提醒warn日志
     *   4.1）获取类加载器，并且从输入流中读取属性加载的到属性Properties中
     * 5）若允许多文件
     *   5.1）遍历url列表
     *     5.1.1）打开连接，并返回输入流
     *     5.1.2）从输入流中读取属性加载到Properties，并设置到返回的属性properties中
     *     5.1.3）关闭输入流
     * 6）返回属性properties的值
     */
    public static Properties loadProperties(String fileName, boolean allowMultiFile, boolean optional) {
        Properties properties = new Properties();
        if (fileName.startsWith("/")) {/**@c 读取本地文件，写到Properties，绝对路径 */
            try {
                FileInputStream input = new FileInputStream(fileName);
                try {
                    properties.load(input);
                } finally {
                    input.close();
                }
            } catch (Throwable e) {
                logger.warn("Failed to load " + fileName + " file from " + fileName + "(ingore this file): " + e.getMessage(), e);
            }
            return properties;
        }

        List<java.net.URL> list = new ArrayList<java.net.URL>();/**@c 获取指定文件名对应的URL列表 */
        try {
            Enumeration<java.net.URL> urls = ClassHelper.getClassLoader().getResources(fileName);
            list = new ArrayList<java.net.URL>();
            while (urls.hasMoreElements()) {
                list.add(urls.nextElement());
            }
        } catch (Throwable t) {
            logger.warn("Fail to load " + fileName + " file: " + t.getMessage(), t);
        }

        if (list.size() == 0) {
            if (!optional) {
                logger.warn("No " + fileName + " found on the class path.");
            }
            return properties;
        }

        if (!allowMultiFile) { //不允许多文件
            if (list.size() > 1) {/**@c 在不允许多文件时，不能超过一个问题 */
                String errMsg = String.format("only 1 %s file is expected, but %d dubbo.properties files found on class path: %s",
                        fileName, list.size(), list.toString());
                logger.warn(errMsg);
                // throw new IllegalStateException(errMsg); // see http://code.alibabatech.com/jira/browse/DUBBO-133
            }

            // fall back to use method getResourceAsStream
            try {
                properties.load(ClassHelper.getClassLoader().getResourceAsStream(fileName));
            } catch (Throwable e) {
                logger.warn("Failed to load " + fileName + " file from " + fileName + "(ingore this file): " + e.getMessage(), e);
            }
            return properties;
        }

        logger.info("load " + fileName + " properties file from " + list);

        for (java.net.URL url : list) { //有多个url时，加每个url中对应的属性值设置到Properties
            try {
                Properties p = new Properties();
                InputStream input = url.openStream();
                if (input != null) {
                    try {
                        p.load(input);
                        properties.putAll(p);
                    } finally {
                        try {
                            input.close();
                        } catch (Throwable t) {
                        }
                    }
                }
            } catch (Throwable e) {
                logger.warn("Fail to load " + fileName + " file from " + url + "(ingore this file): " + e.getMessage(), e);
            }
        }

        return properties;
    }

    public static int getPid() {/**@获取进程pid */
        if (PID < 0) {
            try {
                RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
                String name = runtime.getName(); // format: "pid@hostname"
                PID = Integer.parseInt(name.substring(0, name.indexOf('@')));
            } catch (Throwable e) {
                PID = 0;
            }
        }
        return PID;
    }

}