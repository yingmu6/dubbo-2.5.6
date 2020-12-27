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
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.support.Parameter;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 配置解析的工具方法、公共方法
 * 抽象类，但该类没有抽象方法
 *
 * @author william.liangf
 * @export
 */
public abstract class AbstractConfig implements Serializable {/**@c API配置方式 抽象出公有的配置操作 */

    protected static final Logger logger = LoggerFactory.getLogger(AbstractConfig.class);
    private static final long serialVersionUID = 4267533505537413570L;
    private static final int MAX_LENGTH = 200;

    private static final int MAX_PATH_LENGTH = 200;
    /**@c
     * 正则表达式语法
     * 加号+ 相当于 {1,} ,匹配一次或者多次前面的字符或子字符串
     * 问好? 相当于 {0,1} 出现0次或1次
     * 星号* 相当于 {0,} 出现0次或多次
     */

    //模式名称
    private static final Pattern PATTERN_NAME = Pattern.compile("[\\-._0-9a-zA-Z]+");

    private static final Pattern PATTERN_MULTI_NAME = Pattern.compile("[,\\-._0-9a-zA-Z]+");

    private static final Pattern PATTERN_METHOD_NAME = Pattern.compile("[a-zA-Z][0-9a-zA-Z]*");

    private static final Pattern PATTERN_PATH = Pattern.compile("[/\\-$._0-9a-zA-Z]+");

    private static final Pattern PATTERN_NAME_HAS_SYMBOL = Pattern.compile("[:*,/\\-._0-9a-zA-Z]+");

    private static final Pattern PATTERN_KEY = Pattern.compile("[*,\\-._0-9a-zA-Z]+");
    private static final Map<String, String> legacyProperties = new HashMap<String, String>();
    private static final String[] SUFFIXS = new String[]{"Config", "Bean"};

    /**
     * 预置的属性映射集合legacyProperties
     * 包含name、host、port、threads、timeout、
     * retries、check、url等参数
     */
    static {//legacy:遗赠，遗产， 这些参数的用途？解：替换特定的属性值（协议protocol、消费端配置consumer从服务端去获取）
        legacyProperties.put("dubbo.protocol.name", "dubbo.service.protocol");
        legacyProperties.put("dubbo.protocol.host", "dubbo.service.server.host");
        legacyProperties.put("dubbo.protocol.port", "dubbo.service.server.port");
        legacyProperties.put("dubbo.protocol.threads", "dubbo.service.max.thread.pool.size");
        legacyProperties.put("dubbo.consumer.timeout", "dubbo.service.invoke.timeout");
        legacyProperties.put("dubbo.consumer.retries", "dubbo.service.max.retry.providers");
        legacyProperties.put("dubbo.consumer.check", "dubbo.service.allow.no.provider");
        legacyProperties.put("dubbo.service.url", "dubbo.service.address");
        // self
        legacyProperties.put("dubbo.application.name", "dubbo.application.service.name");
    }

    //此处的用途？优雅停机

    /**
     * 在虚拟机停止前，调用钩子函数
     * 1）停止前，答应方法被执行的日志
     * 2）销毁协议配置
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                if (logger.isInfoEnabled()) {
                    logger.info("Run shutdown hook now.");
                }
                ProtocolConfig.destroyAll(); //停机时，销毁协议配置（移除节点）
            }
        }, "DubboShutdownHook"));
    }

    protected String id;

    /**
     * 对特殊属性处理
     * 1）若key的值为dubbo.service.max.retry.providers，即提供者最大重试次数
     *         将给定的值减1，如value为3，处理后的值为2
     * 2）若key的值为dubbo.service.allow.no.provider，即为是否允许没有提供者
     *         对传入的值去反
     */
    private static String convertLegacyValue(String key, String value) {
        if (value != null && value.length() > 0) {
            if ("dubbo.service.max.retry.providers".equals(key)) {
                return String.valueOf(Integer.parseInt(value) - 1);
            } else if ("dubbo.service.allow.no.provider".equals(key)) {
                return String.valueOf(!Boolean.parseBoolean(value));
            }
        }
        return value;
    }

    /**
     * DUBBO配置项的优先级: java -D优先于 Spring配置，Spring配置优先于 properties文件的配置，这也符合一般项目的规则.
     * https://www.cnblogs.com/ydxblog/p/5714476.html
     * java -D 效果等同 System.setProperty()
     *
     * 等同：系统配置（启动配置）> xml配置（API配置）> properties文件配置
     * 此方法用于是将dubbo的属性配置过滤处理
     */

    /**
     * 将系统属性或属性文件的内容加载到配置对象中AbstractConfig（按优先级更新config属性值：系统属性配置 > XML配置 > 属性文件配置）
     * 思路整理：
     * 1）遍历config对象的符合要求的set方法（能访问、参数是基本类型），从方法名中解析出属性名称
     * 2）构建属性前缀key，先尝试拼接configId，从系统属性中查找，若没有按不拼接的从系统属性中查找，
     *    若查找到值，则更新config对象中对应属性的值
     * 3）若系统属性中没有查到，则通过get方法获取属性的值，若能取到则不处理
     * 4）若系统属性、xml配置中都没有值，则尝试加载配置文件读取值
     *    若存在值，则将属性文件中的值更新到config文件中
     *    若不存在值：则从预定义的legacyProperties集合中变更key，再尝试获取值
     */
    protected static void appendProperties(AbstractConfig config) {/**@c 向上转型，依次设置属性的值*/
        if (config == null) {
            return;
        }
        String prefix = "dubbo." + getTagName(config.getClass()) + "."; /**@c 如：dubbo.provider. 或dubbo.application. */
        Method[] methods = config.getClass().getMethods();
        for (Method method : methods) {
            try {
                /**@c 处理方法名，获取配置值 找到set方法进行设值 */
                String name = method.getName();
                if (name.length() > 3 && name.startsWith("set") && Modifier.isPublic(method.getModifiers())
                        && method.getParameterTypes().length == 1 && isPrimitive(method.getParameterTypes()[0])) {
                    /**@c 将方法名set后的字母小写，然后后面的由大写字母的地方用"-"分隔，并转换为小写
                     * 将驼峰式的写法改为用分隔符分开的形式
                     * 去掉set，获取到属性名，如setDefault变为default
                     */
                    String property = StringUtils.camelToSplitName(name.substring(3, 4).toLowerCase() + name.substring(4), "-");
                    /**@c 先从系统中获取属性值，若没有，则调用get或is方法获取值 */
                    String value = null;
                    if (config.getId() != null && config.getId().length() > 0) {
                        String pn = prefix + config.getId() + "." + property;
                        value = System.getProperty(pn);
                        if (!StringUtils.isBlank(value)) {
                            logger.info("Use System Property " + pn + " to config dubbo");
                        }
                    }
                    // System.setProperty("dubbo.application.name", "reress"); 用系统中的值
                    if (value == null || value.length() == 0) {
                        String pn = prefix + property;/**@c 如：dubbo.provider.default */
                        value = System.getProperty(pn);
                        if (!StringUtils.isBlank(value)) {
                            logger.info("Use System Property " + pn + " to config dubbo");
                        }
                    }
                    if (value == null || value.length() == 0) {
                        Method getter;
                        try {
                            /**@c 判断是否有get+属性名的方法，如果没有，寻找是否有is+属性名的方法 */
                            getter = config.getClass().getMethod("get" + name.substring(3), new Class<?>[0]);
                        } catch (NoSuchMethodException e) {
                            try {
                                getter = config.getClass().getMethod("is" + name.substring(3), new Class<?>[0]);
                            } catch (NoSuchMethodException e2) {
                                getter = null;
                            }
                        }
                        if (getter != null) {/**@c 执行method中方法invoke */
                            if (getter.invoke(config, new Object[0]) == null) {/**@c invoke(方法所属对象，参数列表) */ //此处为啥没有else，若没有else，怎么设置值的？ 若get方法有值，表明通过xml有设置，则不处理
                                if (config.getId() != null && config.getId().length() > 0) { //例如：dubbo.application.config_app.name
                                    value = ConfigUtils.getProperty(prefix + config.getId() + "." + property);
                                }
                                if (value == null || value.length() == 0) {
                                    value = ConfigUtils.getProperty(prefix + property); //例如：dubbo.application.name
                                }
                                if (value == null || value.length() == 0) {/**@c 替换特定的属性 */
                                    String legacyKey = legacyProperties.get(prefix + property);
                                    if (legacyKey != null && legacyKey.length() > 0) {
                                        value = convertLegacyValue(legacyKey, ConfigUtils.getProperty(legacyKey));
                                    }
                                }

                            }
                        }
                    }
                    if (value != null && value.length() > 0) {/**@c 执行方法调用 */
                        method.invoke(config, new Object[]{convertPrimitive(method.getParameterTypes()[0], value)});
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 获取标签名，如ProviderConfig处理后为Provider
     * 1）获取config类的类名，如ProviderConfig
     * 2）判断类名是否是否以Config、Bean为后缀
     *    若做字符串截取，去掉Config、Bean
     * 3）将字符串变为小写字符串
     */
    private static String getTagName(Class<?> cls) {
        String tag = cls.getSimpleName();/**@c 如com.alibaba.dubbo.config.ProviderConfig的simpleName为ProviderConfig */
        for (String suffix : SUFFIXS) {/**@c suffix后缀，将后缀名Config、Bean去掉，就是标签的名称 */
            if (tag.endsWith(suffix)) {/**@c 去掉config或bean，如ProviderConfig处理后为Provider */
                tag = tag.substring(0, tag.length() - suffix.length());
                break;
            }
        }
        tag = tag.toLowerCase(); //将标签名变为小写
        return tag;
    }

    /**@c 附加参数 */
    protected static void appendParameters(Map<String, String> parameters, Object config) {
        appendParameters(parameters, config, null);
    }

    @SuppressWarnings("unchecked")   /**@c 过滤处理URL中的参数值 原始方法*/
    protected static void appendParameters(Map<String, String> parameters, Object config, String prefix) {
        if (config == null) {
            return;
        }
        Method[] methods = config.getClass().getMethods();
        for (Method method : methods) {
            try {
                String name = method.getName();
                if ((name.startsWith("get") || name.startsWith("is"))
                        && !"getClass".equals(name)
                        && Modifier.isPublic(method.getModifiers())
                        && method.getParameterTypes().length == 0
                        && isPrimitive(method.getReturnType())) {
                    /**@ 通过反射机制获取注解 获取方法上的注解，注解的值若没有设置，则取默认值 */
                    Parameter parameter = method.getAnnotation(Parameter.class);
                    if (method.getReturnType() == Object.class || parameter != null && parameter.excluded()) { //excluded表示的方法对应的字段，不会出现在url中
                        continue;
                    }
                    int i = name.startsWith("get") ? 3 : 2;
                    String prop = StringUtils.camelToSplitName(name.substring(i, i + 1).toLowerCase() + name.substring(i + 1), ".");
                    String key;
                    if (parameter != null && parameter.key() != null && parameter.key().length() > 0) {
                        key = parameter.key(); //取方法上注解key的值
                    } else {
                        key = prop;
                    }
                    Object value = method.invoke(config, new Object[0]); // 执行方法调用，并返回执行的结果
                    String str = String.valueOf(value).trim();
                    if (value != null && str.length() > 0) {
                        if (parameter != null && parameter.escaped()) {
                            str = URL.encode(str); //将参数对应的值进行url编码
                        }
                        if (parameter != null && parameter.append()) {
                            String pre = (String) parameters.get(Constants.DEFAULT_KEY + "." + key);
                            if (pre != null && pre.length() > 0) {
                                str = pre + "," + str;
                            }
                            pre = (String) parameters.get(key);
                            if (pre != null && pre.length() > 0) {
                                str = pre + "," + str;
                            }
                        }
                        if (prefix != null && prefix.length() > 0) {
                            key = prefix + "." + key;
                        }
                        parameters.put(key, str);/**@c 存入键值*/
                    } else if (parameter != null && parameter.required()) { //
                        throw new IllegalStateException(config.getClass().getSimpleName() + "." + key + " == null");
                    }
                } else if ("getParameters".equals(name)  /**@c ProtocolConfig中方法*/
                        && Modifier.isPublic(method.getModifiers())
                        && method.getParameterTypes().length == 0
                        && method.getReturnType() == Map.class) {
                    Map<String, String> map = (Map<String, String>) method.invoke(config, new Object[0]);
                    if (map != null && map.size() > 0) {
                        String pre = (prefix != null && prefix.length() > 0 ? prefix + "." : "");
                        for (Map.Entry<String, String> entry : map.entrySet()) {
                            parameters.put(pre + entry.getKey().replace('-', '.'), entry.getValue());
                        }
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    }

    /**
     * 重写方法：
     * 添加属性值算法逻辑：
     * 1）判断config是否为空
     * 2）获取属性值前缀 prefix = "dubbo." + 标签名 + "."
     * 3) 反射机制获取方法，对set方法进行过滤：以set开头、长度大于3、公有方法、参数只有一个、参数是基本类型
     * 4）从System获取值，key为 prefix + configId + name(属性名) 以及key为 prefix + name，看是否能获取到，若能值设置改值，若不能继续查找
     * 5）获取getter方法以及is方法看是否有设定值，若没有设置值，继续往下查找
     * 6）从ConfigUtil中获取属性值，即从配置文件中获取值，key依然为prefix + configId + name和prefix + name，若没取到值，继续查找
     * 7）从特定的map中legacyProperties获取值，若没有取到值，本地设置值终止
     */

    protected static void appendPropertiesOverride(AbstractConfig abstractConfig) { //设置config类的属性值，使用setter方式  IOC方式
        if (abstractConfig == null) {
            return;
        }

        Class configClass = abstractConfig.getClass();
        String prefix = "dubbo." + getTagName(configClass) + ".";  //如ProviderConfig类对应的标签名前缀为 dubbo.provider.
        Method[] methods = configClass.getMethods();
        String configId = abstractConfig.getId();
        for (Method method : methods) { //遍历配置类中的方法，对符合条件的set方法处理
            try {
                String name = method.getName();
                String value = null;
                String property = "";
                if (name.startsWith("set") && name.length() > 3 && Modifier.isPublic(method.getModifiers())
                        && (method.getParameterCount() == 1) && isPrimitive(method.getParameterTypes()[0])) {
                    //将属性名格式化(取set以后的字符串，按分隔符分隔作为属性名)
                    property = StringUtils.camelToSplitName(name.substring(3, 4).toLowerCase() + name.substring(4), "-");
                    if (StringUtils.isNotEmpty(configId)) {
                        value = System.getProperty(prefix + configId + property); //配置id不为空时，加上配置id作为前缀，key如 dubbo.id1.provider.codec
                        if (StringUtils.isNotEmpty(value)) {
                            logger.info("use system properties , key=" + (prefix + configId + property) + ", value=" + value);
                        }
                    }
                    if (StringUtils.isEmpty(value)) {
                        value = System.getProperty(prefix + property); //若值为空，去掉配置id为key，继续查询，key如 dubbo.provider.codec
                        if (StringUtils.isNotEmpty(value)) {
                            logger.info("use system properties , key=" + (prefix + property) + ", value=" + value);
                        }
                    }
                    if (StringUtils.isEmpty(value)) {
                        Method getter;
                        try { //从get或is方法获取值判断 getCodec()
                            getter = configClass.getMethod("get" + name.substring(3), new Class[0]);
                        } catch (Exception e) {
                            try {
                                getter = configClass.getMethod("is" + name.substring(2), new Class[0]);
                            } catch (Exception e1) {
                                getter = null;
                            }
                        }
                        if (getter != null) {
                            if (getter.invoke(abstractConfig, new Object[0]) == null) { //在配置类对应的get方法或is方法返回值为空时，从配置文件中查找属性值
                                if (StringUtils.isEmpty(value)) {   //从属性文件中获取
                                    if (StringUtils.isNotEmpty(configId)) {
                                        value = ConfigUtils.getProperty(prefix + configId + property);
                                        if (StringUtils.isNotEmpty(value)) {
                                            logger.info("use dubbo properties file, key=" + (prefix + configId + property) + ", value=" + value);
                                        }
                                    }
                                }
                                if (StringUtils.isEmpty(value)) {
                                    value = ConfigUtils.getProperty(prefix + property);
                                    if (StringUtils.isNotEmpty(value)) {
                                        logger.info("use dubbo properties file, key=" + (prefix + property) + ", value=" + value);
                                    }
                                }
                                if (StringUtils.isEmpty(value)) {  //从预定义的key获取（若在系统属性、get方法、配置文件中都没找到，则将key换位预定义的key继续查）
                                    String key = legacyProperties.get(prefix + property); //将指定的key转换
                                    if (key != null && key.length() > 0) {
                                        value = convertLegacyValue(key, ConfigUtils.getProperty(key)); //过滤特殊的key值
                                    }
                                }
                            }
                        }
                    }

                    if (StringUtils.isNotEmpty(value)) { //执行对应的方法，设置属性值
                        try {
                            method.invoke(abstractConfig, value);
                        } catch (Exception e) {
                            logger.info("invoke method error " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) { //错误日志捕获
                logger.error(e.getMessage(), e);
            }
        }

        //logger.info("设置配置类属性值：" + abstractConfig.toString());
    }

    /**
     * 添加处理URL中的参数,将config配置中的符合条件的属性以及属性值添加到map中
     * 算法思路：
     * 1）判断config是否为空
     * 2）config不为空的时候，对get方法或is或getParameters方法进行处理
     * 3）get方法中，取出方法上的注解，分析属性excluded、key、escaped、append、required
     *    excluded判断是否忽略；key获取别名；escaped判断是否转义；append判断是否附加默认属性、required表示属性是必须设置值的
     * 4）getParameters方法中，循环遍历map，把值写入待处理的map中
     */

    protected static void appendParametersOverride (Map<String, String> parametersMap, Object config, String prefix) {
        if (config == null) {
            return;
        }
        Method [] methods = config.getClass().getMethods();
        try {
            for (Method method : methods) {
                String name = method.getName(); //参数个数为0；方法是公有的；返回类型是基本类型
                if ((name.startsWith("get")  || name.startsWith("is")) //筛选get或is方法
                        && method.getParameterCount() == 0 && (!name.equals("getClass"))
                        && Modifier.isPublic(method.getModifiers())
                        && isPrimitive(method.getReturnType())) {
                    Parameter parameter = method.getAnnotation(Parameter.class); //获取方法上注解
                    if (method.getReturnType() == Object.class || parameter != null && parameter.excluded()) {
                        continue;
                    }
                    int i = name.startsWith("get") ? 3 : 2; //判断get和is方法属性起始位置
                    String property = StringUtils.camelToSplitName(name.substring(i, i + 1).toLowerCase() + name.substring(i + 1), ".");
                    String key;
                    if (parameter != null && StringUtils.isNotEmpty(parameter.key())) { //判断是否有别名，若有别名，属性名取别名
                        key = parameter.key();
                    } else {
                        key = property;
                    }
                    Object value = method.invoke(config, new Object[0]);
                    String val = value == null ? "" : String.valueOf(value).trim();
                    if (value != null && val.length() > 0) {
                        if (parameter != null && parameter.escaped()) {
                            val = URL.encode(val);
                        }

                        // 如果注解append为true，将多个值用分隔符相连
                        if (parameter != null && parameter.append()) { //判断参数集中是否存在key，若存在将值相连起来
                            String defaultVal = parametersMap.get(Constants.DEFAULT_KEY + "." + key);
                            if (defaultVal != null && defaultVal.length() > 0) {
                                val = defaultVal + "," + val;
                            }
                            defaultVal = parametersMap.get(key);
                            if (defaultVal != null && defaultVal.length() > 0) {
                                val = defaultVal + "," + val;
                            }
                        }
                        if (prefix != null && prefix.length() > 0) { //若前缀不为空，附加到键上key
                            key = prefix + "." + key;
                        }

                        // 若append为false，相同的key，值会被覆盖
                        parametersMap.put(key, val); /** 存入map */
                    } else if (parameter != null && parameter.required()) { //若属性是必须的，就必须需要值
                        throw new IllegalStateException(key + "is required, "  + "can not null");
                    }

                } else if (name.equals("getParameters")  /**@c 将已有map的属性加到parametersMap */
                        && Modifier.isPublic(method.getModifiers())
                        && method.getParameterTypes().length == 0
                        && method.getReturnType() == Map.class) {
                    Map<String, String> map = (Map<String, String>) method.invoke(config, new Object[0]);
                    if (map != null && map.size() > 0) {
                        // 键加上前缀
                        String pre = (prefix != null && prefix.length() > 0 ? prefix + "." : "");
                        Set<String> keySet = map.keySet();
                        for (String key : keySet) { //key加上前缀并将'-'替换为'.'
                            parametersMap.put(pre + key.replace('-', '.'), map.get(key));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected static void appendAttributes(Map<Object, Object> parameters, Object config) {
        appendAttributes(parameters, config, null);
    }

    /**
     * 遍历配置类config的方法，将包含注解Parameter，且含有属性的attribute的值设置的参数map中
     */
    protected static void appendAttributes(Map<Object, Object> parameters, Object config, String prefix) {
        if (config == null) {
            return;
        }
        Method[] methods = config.getClass().getMethods();
        for (Method method : methods) {
            try {
                String name = method.getName();
                if ((name.startsWith("get") || name.startsWith("is"))
                        && !"getClass".equals(name)
                        && Modifier.isPublic(method.getModifiers())
                        && method.getParameterTypes().length == 0 // 没有参数
                        && isPrimitive(method.getReturnType())) { // 是基本类型
                    Parameter parameter = method.getAnnotation(Parameter.class);
                    if (parameter == null || !parameter.attribute()) //分析注解参数attribute
                        continue;
                    String key;
                    if (parameter != null && parameter.key() != null && parameter.key().length() > 0) {
                        key = parameter.key();
                    } else {
                        int i = name.startsWith("get") ? 3 : 2;/**@c 判定get或is方法 */
                        key = name.substring(i, i + 1).toLowerCase() + name.substring(i + 1);
                    }
                    Object value = method.invoke(config, new Object[0]); //invoke执行目标对象的方法，前面是method，后面是目标对象
                    if (value != null) {
                        if (prefix != null && prefix.length() > 0) {
                            key = prefix + "." + key;
                        }
                        parameters.put(key, value);
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    }

    /**@c 是否是基本类型 */
    private static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type == Character.class
                || type == Boolean.class
                || type == Byte.class
                || type == Short.class
                || type == Integer.class
                || type == Long.class
                || type == Float.class
                || type == Double.class
                || type == Object.class;
    }

    /**@c 将基本类型转换为封装类型 */
    private static Object convertPrimitive(Class<?> type, String value) {
        if (type == char.class || type == Character.class) {
            return value.length() > 0 ? value.charAt(0) : '\0';
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.valueOf(value);
        } else if (type == byte.class || type == Byte.class) {
            return Byte.valueOf(value);
        } else if (type == short.class || type == Short.class) {
            return Short.valueOf(value);
        } else if (type == int.class || type == Integer.class) {
            return Integer.valueOf(value);
        } else if (type == long.class || type == Long.class) {
            return Long.valueOf(value);
        } else if (type == float.class || type == Float.class) {
            return Float.valueOf(value);
        } else if (type == double.class || type == Double.class) {
            return Double.valueOf(value);
        }
        return value;
    }

    protected static void checkExtension(Class<?> type, String property, String value) {
        checkName(property, value);/**@c 检查value值是否符合条件 */
        if (value != null && value.length() > 0
                && !ExtensionLoader.getExtensionLoader(type).hasExtension(value)) { /**@c 检查spi扩展是否正确 */
            throw new IllegalStateException("No such extension " + value + " for " + property + "/" + type.getName());
        }
    }

    protected static void checkMultiExtension(Class<?> type, String property, String value) {
        checkMultiName(property, value); /**@ 与checkName匹配的正则表达式不同 value由多个值组成 */
        if (value != null && value.length() > 0) {
            String[] values = value.split("\\s*[,]+\\s*");
            for (String v : values) {
                if (v.startsWith(Constants.REMOVE_VALUE_PREFIX)) {
                    v = v.substring(1);
                }
                if (Constants.DEFAULT_KEY.equals(v)) {
                    continue;
                }
                if (!ExtensionLoader.getExtensionLoader(type).hasExtension(v)) {
                    throw new IllegalStateException("No such extension " + v + " for " + property + "/" + type.getName());
                }
            }
        }
    }

    protected static void checkLength(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, null);
    }

    protected static void checkPathLength(String property, String value) {
        checkProperty(property, value, MAX_PATH_LENGTH, null);
    }

    protected static void checkName(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_NAME);
    }

    protected static void checkNameHasSymbol(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_NAME_HAS_SYMBOL);
    }

    protected static void checkKey(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_KEY);
    }

    protected static void checkMultiName(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_MULTI_NAME);
    }

    protected static void checkPathName(String property, String value) {
        checkProperty(property, value, MAX_PATH_LENGTH, PATTERN_PATH);
    }

    protected static void checkMethodName(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_METHOD_NAME);
    }

    protected static void checkParameterName(Map<String, String> parameters) {
        if (parameters == null || parameters.size() == 0) {
            return;
        }
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            //change by tony.chenl parameter value maybe has colon.for example napoli address
            checkNameHasSymbol(entry.getKey(), entry.getValue());
        }
    }

    /**@ 检查长度以及正则表达式 只能以数字、字母、中划线、下划线、点号 */
    protected static void checkProperty(String property, String value, int maxlength, Pattern pattern) {
        if (value == null || value.length() == 0) {
            return;
        }
        if (value.length() > maxlength) {
            throw new IllegalStateException("Invalid " + property + "=\"" + value + "\" is longer than " + maxlength);
        }
        if (pattern != null) {
            Matcher matcher = pattern.matcher(value);
            if (!matcher.matches()) {
                throw new IllegalStateException("Invalid " + property + "=\"" + value + "\" contain illegal charactor, only digit, letter, '-', '_' and '.' is legal.");
            }
        }
    }

    /**@ 排除字段，影响哪些操作？解：appendAttributes等进行注解参数判断 */
    @Parameter(excluded = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**@c 将注解中方法的值设置到配置中对应的属性 */
    protected void appendAnnotation(Class<?> annotationClass, Object annotation) {
        Method[] methods = annotationClass.getMethods();
        for (Method method : methods) {
            if (method.getDeclaringClass() != Object.class
                    && method.getReturnType() != void.class
                    && method.getParameterTypes().length == 0
                    && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())) {
                try {
                    String property = method.getName();
                    if ("interfaceClass".equals(property) || "interfaceName".equals(property)) {
                        property = "interface";
                    }
                    String setter = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
                    Object value = method.invoke(annotation, new Object[0]);
                    if (value != null && !value.equals(method.getDefaultValue())) {
                        Class<?> parameterType = ReflectUtils.getBoxedClass(method.getReturnType());
                        if ("filter".equals(property) || "listener".equals(property)) {
                            parameterType = String.class;
                            value = StringUtils.join((String[]) value, ",");
                        } else if ("parameters".equals(property)) {
                            parameterType = Map.class;
                            value = CollectionUtils.toStringMap((String[]) value);
                        }
                        try {
                            Method setterMethod = getClass().getMethod(setter, new Class<?>[]{parameterType});
                            setterMethod.invoke(this, new Object[]{value});
                        } catch (NoSuchMethodException e) {
                            // ignore
                        }
                    }
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 将配置config转换字符串输出
     */
    @Override
    public String toString() {
        try {
            StringBuilder buf = new StringBuilder();
            buf.append("<dubbo:");
            buf.append(getTagName(getClass())); /**@c 将ApplicationConfig等 去掉config小写后得到标签名，getClass()是Object中的方法 */
            Method[] methods = getClass().getMethods();
            for (Method method : methods) {
                try {
                    /**@c 筛选满足条件get、is方法，并且返回值不为空，以键值对的形式展示出来 */
                    String name = method.getName();
                    if ((name.startsWith("get") || name.startsWith("is"))
                            && !"getClass".equals(name) && !"get".equals(name) && !"is".equals(name)
                            && Modifier.isPublic(method.getModifiers()) //修饰符为public
                            && method.getParameterTypes().length == 0   //参数列表为空
                            && isPrimitive(method.getReturnType())) {   //返回类型是基本类
                        int i = name.startsWith("get") ? 3 : 2;
                        String key = name.substring(i, i + 1).toLowerCase() + name.substring(i + 1);
                        Object value = method.invoke(this, new Object[0]);
                        if (value != null) { //属性值不为空
                            buf.append(" ");
                            buf.append(key);
                            buf.append("=\"");
                            buf.append(value);
                            buf.append("\"");
                        }
                    }
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
            }
            buf.append(" />");
            return buf.toString();
        } catch (Throwable t) { // 防御性容错
            logger.warn(t.getMessage(), t);
            return super.toString();
        }
    }

    /**
     * 将注解中的值设置给对应的属性
     * 1.获取注解中的所有方法
     * 2.注解中的方法过滤：getDeclareClass != Object.class；retureType != void.class；parameterCount == 0；
     * Modifier.isPublic； ！Modifier.isStatic
     * 3.对interfaceClass或interfaceName方法处理，属性名按照interface处理
     * 4.执行注解中的方法，进行判断 value != null && !value == method.getDefault
     * 5.如果属性是filter或者listener，可能有多个值，将字符串拼接join
     *   如果属性是getParameters，值为Map，CollectionUtils.toMap
     * 6.当前对象中getClass查找对应的属性方法，如果有该方法，则设置值。若没有，则跳过
     */
    protected void appendAnnotationOverride(Class<?> annotationClass, Object annotation) {
        Method[] methods = annotationClass.getMethods();
            for (Method method : methods) {
                if (method.getDeclaringClass() != Object.class && method.getReturnType() != void.class
                        && method.getParameterCount() == 0 && Modifier.isPublic(method.getModifiers())
                        && !Modifier.isStatic(method.getModifiers())) {

                    try { //异常捕获放在for循环内部，某次失败，并不影响下次执行，放在for循环外，一旦某次失败，for循环就结束了
                        String property = method.getName(); // idea中的watcher可以通过method.name设置值，不能只指定一个值，因为method是个对象，还需要指定root等，如果数据类型复杂就加判断语句，对指定的值进行分析
    //                    if (!property.equals("testAppConfigOut")) {
    //                        continue;
    //                    }
                        if ("interfaceClass".equals(property) || "interfaceName".equals(property)) {
                            property = "interface";
                        }
                        Object value = method.invoke(annotation, new Object[0]); //invoke指定的对象需要是一个实例，不能是个class，java.lang.IllegalArgumentException: object is not an instance of declaring class
                        if (value != null && (!value.equals(method.getDefaultValue()))) {// 值不为空，并且不等于默认值
                            Class<?> parameterType = ReflectUtils.getBoxedClass(method.getReturnType()); //获取返回类型的封装类型
                            if ("filter".equals(property) || "listener".equals(property)) { //多个值用分隔符分隔
                                parameterType = String.class;
                                value = StringUtils.join((String [])value, ",");
                            }
                            if ("parameters".equals(property)) { //返回值为Map类型
                                parameterType = Map.class;
                                value = CollectionUtils.toStringMap((String [])value);
                            }

                            try {
                                Method setter = getClass().getMethod("set" + property.substring(0, 1).toUpperCase() + property.substring(1), new Class<?>[]{parameterType});
                                setter.invoke(this, new Object[]{value}); //为当前对象设置值
                            } catch (NoSuchMethodException e) {

                            }
                        }
                    } catch (Exception e) {
                        logger.error("append annotion error ", e);
                    }
                }
            }

    }

}