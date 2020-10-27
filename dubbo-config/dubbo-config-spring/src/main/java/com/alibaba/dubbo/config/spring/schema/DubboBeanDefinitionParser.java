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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.ArgumentConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.MethodConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.spring.ReferenceBean;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.alibaba.dubbo.rpc.Protocol;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * AbstractBeanDefinitionParser
 *
 * @author william.liangf
 * @export
 */
/**@c dubbo对xml的解析 自定义解析 */
public class DubboBeanDefinitionParser implements BeanDefinitionParser { // dubbo bean定义解析器

    private static final Logger logger = LoggerFactory.getLogger(DubboBeanDefinitionParser.class);
    private static final Pattern GROUP_AND_VERION = Pattern.compile("^[\\-.0-9_a-zA-Z]+(\\:[\\-.0-9_a-zA-Z]+)?$");
    private final Class<?> beanClass;
    private final boolean required;

    public DubboBeanDefinitionParser(Class<?> beanClass, boolean required) { //history 此处的required，是指bean是否必须还是指id是否必须？因为xml中并没有看到ModuleConfig
        this.beanClass = beanClass; // bean的类型，如ApplicationConfig.class、ProtocolConfig.class等
        this.required = required;
    }

    public static void main(String[] args) {
        System.out.println(ArgumentConfig.class.getName());
    }

    /**
     * 1.通过spring 解析到 xml对应的元素、属性值
     * 2.写到dubbo的config对象中，如ServiceConfig、ReferenceConfig等
     * Element 解析的XML元素、ParserContext解析的上下文、beanClass XML元素对应的bean类型，required是否必须
     */

    /**
     * 解析流程处理：
     * 输入参数：Element（HTML DOM的元素）、ParserContext（解析器上下文）、beanClass（bean的Class类）、required（是否必须）
     * 1）创建根元素的bean定义RootBeanDefinition(Spring中的定义)，RootBeanDefinition的数据格式
     *    （Root bean: class [com.alibaba.dubbo.config.ProtocolConfig]; scope=; abstract=false;
     *    lazyInit=false; autowireMode=0; dependencyCheck=0; autowireCandidate=true; primary=false; factoryBeanName=null;
     *    factoryMethodName=null; initMethodName=null; destroyMethodName=null）
     * 2）设置属性值beanClass（如ApplicationConfig的class）、lazyInit（是否延迟初始化）
     * 3）获取dubbo配置config的id值（设置dubbo自定义的config中的id，如ApplicationConfig中的id）
     *   3.1）先尝试获取元素属性id值，若不能取到，则继续查找
     *   3.2）尝试获取元素属性name值，若不能取到，判断beanClass是否是ProtocolConfig，若是则取"dubbo"值（针对元素<dubbo:protocol>），
     *        否则尝试获取属性"interface"的值（针对元素<dubbo:service>和<dubbo:reference>），若获取的beanName为空，则继续查找
     *   3.3）获取beanClass的类名，如"com.alibaba.dubbo.config.ApplicationConfig"（兜底方案，肯定到这步能取到）
     *   3.4）判断是否存在id对应的bean是否存在，若存在则将bean的名字加上序号区分，如dubbo、dubbo1等
     * 4）若id不为空
     *   4.1）判断注册实例是否包含id，若有包含，则抛出id重复的异常
     *   4.2）获取注册实例，将id与bean设置到spring中Map<String, BeanDefinition>本地缓存中
     *   4.3）添加beanDefinition属性id对应的值
     * 5）判断beanClass的类型（对ProtocolConfig、ServiceBean、ProviderConfig、ConsumerConfig解析处理）
     *   5.1）若是ProtocolConfig类型，对<dubbo:protocol>处理，遍历已注册的bean的名字列表
     *        （如：xml里元素包含的名字列表中"dubbo-provider"、"com.alibaba.dubbo.config.RegistryConfig"、"dubbo"等）
     *     5.1.1）获取bean的名称name对应的实例BeanDefinition
     *     5.1.2）获取bean实例中"protocol"对应的属性实例PropertyValue
     *     5.1.3）若属性实例不为空，则获取属性值
     *       5.1.3.1）若属性值value是ProtocolConfig的实例且id值
     *        与value的名称getName()相同，则将"protocol"对应的属性对象置为new RuntimeBeanReference(id)
     *   5.2）若是ServiceBean类型（对<dubbo:service>元素处理）
     *     5.2.1）获取"class"对应的class名称
     *     5.2.2）若className不为空
     *       5.2.2.1）创建RootBeanDefinition根bean实例，并设置属性值beanClass、lazyInit
     *       5.2.2.2）解析子节点的属性
     *       5.2.2.3）设置元素ref属性的值（BeanDefinitionHolder对象值）
     *   5.3）若是ProviderConfig类型
     *     5.3.1）解析嵌套的元素，tag为"service"，property为"provider"
     *   5.4）若是ConsumerConfig
     *     5.4.1）解析嵌套的元素，tag为"reference"，property为"consumer"
     * 6）获取beanClass的方法列表，并遍历
     *    6.1）获取方法的名称，并判断方法是否是public、是否是"set"开头，是否参数只有一个
     *     6.1.1）若满足条件，则获取第一个参数类型(参数只有一个)
     *     6.1.2）从方法名中取出属性名，并且使用分隔符分隔，如setNameOrAge，得到的结构为"name-or-age"
     *     6.1.3）将属性property添加到属性集合中
     *     6.1.4）通过get方法名构建get的Method对象
     *            若没有get方法，则尝试is方法构造
     *     6.1.5）若get方法对象为空或者不是public方法或type类型与get方法对象的返回值不同，则进入下次循环
     *     6.1.6）判断属性值property
     *      6.1.6.1）若值为"parameters"，则parseParameters()解析参数，返回ManagedMap
     *      6.1.6.2）若值为"methods"，则parseMethods()解析子节点<dubbo:method>，并设置到根元素中RootBeanDefinition
     *      6.1.6.3）若值为"arguments"，则parseArguments()解析子节点<dubbo:arguments>，并设置到根元素中RootBeanDefinition
     *      6.1.6.4）若值都不是上面的，元素中获取property对应的值，并且值不为空
     *        6.1.6.4.1）若property值为"registry"，且属性对应的值为"N/A"（忽悠大小写）
     *          6.1.6.4.1.1）构建RegistryConfig实例，设置地址为"N/A"
     *          6.1.6.4.1.2）并且为元素设置属性property的值为registryConfig
     *        6.1.6.4.2）若property值为"registry"，且包含","，表明有多个注册地址
     *          6.1.6.4.2.1）解析多个引用parseMultiRef
     *        6.1.6.4.3）若property值为"providers"，且包含","，表明有多个提供者
     *          6.1.6.4.3.1）解析多个引用parseMultiRef
     *        6.1.6.4.4）若property值为"protocols"，且包含","，表明有多个协议
     *          6.1.6.4.4.1）解析多个引用parseMultiRef
     *        6.1.6.4.5）若值都不是上面的
     *          6.1.6.4.5.1）判断Class对象是否是基本类型isPrimitive(type)
     *            6.1.6.4.5.1.1）兼容旧版本xsd中的default值
     *                若property为"async"且值value为"false"（不是异步）或property为"timeout"且值value为"0"（没有设置超时）
     *                或property为"delay"且值value为"0"（没有延时）或property为"version"且值value为"0.0.0"（没有版本号）
     *                或property为"stat"且值value为"-1" 或property为"reliable"且值value为"false"
     *                将属性值置为null，即value = null，并赋值给引用对象reference = value
     *         6.1.6.4.5.2）若property的值为"protocol"并且Protocol存在value对应值的扩展
     *                       并且value不包含在parserContext上下文的实例中，或ProtocolConfig的名称与从上下文实例的BeanClassName不相等
     *           6.1.6.4.5.2.1）若元素标签名为"dubbo:provider"，给出推荐更换的建议
     *           6.1.6.4.5.2.2）兼容旧版本配置，构建ProtocolConfig，并赋值给reference
     *         6.1.6.4.5.3）若property的值为"monitor"且value不包含在parserContext上下文的实例中
     *                      或MonitorConfig的名称与从上下文实例的BeanClassName不相等
     *                      将属性值value，构建MonitorConfig配置对象
     *         6.1.6.4.5.4）若property的值为"onreturn"
     *           6.1.6.4.5.4.1） 查找"."最后出现的位置，符号之前为引用对象returnRef，符号之后为引用方法returnMethod
     *           6.1.6.4.5.4.2） 创建RuntimeBeanReference对象，并赋值给reference，然后添加属性"onreturnMethod"值
     *         6.1.6.4.5.5）若property的值为"onthrow"
     *           6.1.6.4.5.5.1） 查找"."最后出现的位置，符号之前为引用对象throwRef，符号之后为引用方法throwMethod
     *           6.1.6.4.5.5.2） 创建RuntimeBeanReference对象，并赋值给reference，然后添加属性"onthrowMethod"值
     *         6.1.6.4.5.6）若都不满足上面的条件
     *           6.1.6.4.5.6.1） 若property置为"ref"，并且value的值在parserContext上下文的bean中
     *             6.1.6.4.5.6.1.1）从parserContext上下文中获取value对应的BeanDefinition，
     *                              若bean实例对象不是单例的，抛出非法状态异常
     *             6.1.6.4.5.6.1.2）创建RuntimeBeanReference对象，并赋值给reference
     *         6.1.6.4.5.7）设置属性对addPropertyValue(property, reference)
     * 7）获取element.getAttributes元素的属性集合NamedNodeMap
     *   7.1）遍历属性节点Node，获取节点的本地名LocalName
     *   7.2）若Set<String> props集合中不包含LocalName，若ManagedMap为空，则对应创建。
     *   7.3）创建TypedStringValue，并设置到ManagedMap中
     *   7.4）若parameters不为空，则添加到属性"parameters"中
     *   7.5）返回构建的BeanDefinition实例
     */
    @SuppressWarnings("unchecked")
    private static BeanDefinition parse(Element element, ParserContext parserContext, Class<?> beanClass, boolean required) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(); //进入此方法时，spring已经加载好xml，并把元素放入Element
        beanDefinition.setBeanClass(beanClass);
        beanDefinition.setLazyInit(false);
        String id = element.getAttribute("id");
        if ((id == null || id.length() == 0) && required) { //元素id为空且为必须的情况，取属性name、interface、或者beanClass的名字做为id
            String generatedBeanName = element.getAttribute("name");
            if (generatedBeanName == null || generatedBeanName.length() == 0) {
                if (ProtocolConfig.class.equals(beanClass)) {
                    generatedBeanName = "dubbo";
                } else {
                    generatedBeanName = element.getAttribute("interface");
                }
            }
            if (generatedBeanName == null || generatedBeanName.length() == 0) {
                generatedBeanName = beanClass.getName();
            }
            id = generatedBeanName;
            int counter = 2;
            while (parserContext.getRegistry().containsBeanDefinition(id)) {/**@c 统计数字用途：如有id相同的元素，符号加上序号区分 */
                id = generatedBeanName + (counter++); // 循环遍历，添加序号，知道找到没有相同名称的id，如已存在demo2,demo3，就会产生demo4的元素
            }
        }
        if (id != null && id.length() > 0) { //感觉此处是多余的？id怎么会是空？ 解：当id为空且required=false时，不会产生id的值
            if (parserContext.getRegistry().containsBeanDefinition(id)) { //bean的id不能重复
                throw new IllegalStateException("Duplicate spring bean id " + id);
            }
            parserContext.getRegistry().registerBeanDefinition(id, beanDefinition); //注册id与bean的关系
            beanDefinition.getPropertyValues().addPropertyValue("id", id);
        }
        //对ProtocolConfig、ServiceBean、ProviderConfig、ConsumerConfig解析处理
        if (ProtocolConfig.class.equals(beanClass)) {/**@c 是否是ProtocolConfig类型 */
            for (String name : parserContext.getRegistry().getBeanDefinitionNames()) {
                BeanDefinition definition = parserContext.getRegistry().getBeanDefinition(name);
                PropertyValue property = definition.getPropertyValues().getPropertyValue("protocol");
                if (property != null) {// history-new 此处如何覆盖进入？目前<dubbo:protocol> 没有protocol属性
                    Object value = property.getValue();
                    if (value instanceof ProtocolConfig && id.equals(((ProtocolConfig) value).getName())) {
                        definition.getPropertyValues().addPropertyValue("protocol", new RuntimeBeanReference(id));
                    }
                }
            }
        } else if (ServiceBean.class.equals(beanClass)) { //history-new <dubbo:service class=""> 此处的class的用途是？
            String className = element.getAttribute("class");
            if (className != null && className.length() > 0) {
                RootBeanDefinition classDefinition = new RootBeanDefinition();
                classDefinition.setBeanClass(ReflectUtils.forName(className));
                classDefinition.setLazyInit(false);
                parseProperties(element.getChildNodes(), classDefinition);
                beanDefinition.getPropertyValues().addPropertyValue("ref", new BeanDefinitionHolder(classDefinition, id + "Impl"));
            }
        } else if (ProviderConfig.class.equals(beanClass)) {
            parseNested(element, parserContext, ServiceBean.class, true, "service", "provider", id, beanDefinition);
        } else if (ConsumerConfig.class.equals(beanClass)) {
            parseNested(element, parserContext, ReferenceBean.class, false, "reference", "consumer", id, beanDefinition);
        }
        Set<String> props = new HashSet<String>();
        ManagedMap parameters = null;
        for (Method setter : beanClass.getMethods()) {
            String name = setter.getName(); //方法解析
            if (name.length() > 3 && name.startsWith("set")
                    && Modifier.isPublic(setter.getModifiers())
                    && setter.getParameterTypes().length == 1) {
                Class<?> type = setter.getParameterTypes()[0]; //参数类型
                /**@c 解析属性名称，如setName解析后的属性名为name */
                String property = StringUtils.camelToSplitName(name.substring(3, 4).toLowerCase() + name.substring(4), "-");
                props.add(property);
                Method getter = null;
                try {
                    getter = beanClass.getMethod("get" + name.substring(3), new Class<?>[0]); //判断getter方法或is方法是否存在
                } catch (NoSuchMethodException e) {
                    try {
                        getter = beanClass.getMethod("is" + name.substring(3), new Class<?>[0]);
                    } catch (NoSuchMethodException e2) {
                    }
                }
                if (getter == null
                        || !Modifier.isPublic(getter.getModifiers())
                        || !type.equals(getter.getReturnType())) {
                    continue;
                }
                if ("parameters".equals(property)) { //解析<dubbo:parameter>
                    parameters = parseParameters(element.getChildNodes(), beanDefinition);
                } else if ("methods".equals(property)) { //解析<dubbo:method>
                    parseMethods(id, element.getChildNodes(), beanDefinition, parserContext);
                } else if ("arguments".equals(property)) { //解析<dubbo:argument>
                    parseArguments(id, element.getChildNodes(), beanDefinition, parserContext);
                } else {
                    String value = element.getAttribute(property);
                    if (value != null) {
                        value = value.trim();
                        if (value.length() > 0) {
                            if ("registry".equals(property) && RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(value)) { //读取xml中的值，写到config bean中
                                RegistryConfig registryConfig = new RegistryConfig();
                                registryConfig.setAddress(RegistryConfig.NO_AVAILABLE);
                                beanDefinition.getPropertyValues().addPropertyValue(property, registryConfig);
                            } else if ("registry".equals(property) && value.indexOf(',') != -1) {
                                parseMultiRef("registries", value, beanDefinition, parserContext);
                            } else if ("provider".equals(property) && value.indexOf(',') != -1) {
                                parseMultiRef("providers", value, beanDefinition, parserContext);
                            } else if ("protocol".equals(property) && value.indexOf(',') != -1) {
                                parseMultiRef("protocols", value, beanDefinition, parserContext);
                            } else {
                                Object reference;
                                if (isPrimitive(type)) {
                                    if ("async".equals(property) && "false".equals(value)
                                            || "timeout".equals(property) && "0".equals(value)
                                            || "delay".equals(property) && "0".equals(value)
                                            || "version".equals(property) && "0.0.0".equals(value)
                                            || "stat".equals(property) && "-1".equals(value)
                                            || "reliable".equals(property) && "false".equals(value)) {
                                        // 兼容旧版本xsd中的default值
                                        value = null;
                                    }
                                    reference = value;
                                } else if ("protocol".equals(property)
                                        && ExtensionLoader.getExtensionLoader(Protocol.class).hasExtension(value)
                                        && (!parserContext.getRegistry().containsBeanDefinition(value)
                                        || !ProtocolConfig.class.getName().equals(parserContext.getRegistry().getBeanDefinition(value).getBeanClassName()))) {
                                    if ("dubbo:provider".equals(element.getTagName())) {
                                        logger.warn("Recommended replace <dubbo:provider protocol=\"" + value + "\" ... /> to <dubbo:protocol name=\"" + value + "\" ... />");
                                    }
                                    // 兼容旧版本配置
                                    ProtocolConfig protocol = new ProtocolConfig();
                                    protocol.setName(value);
                                    reference = protocol;
                                } else if ("monitor".equals(property)
                                        && (!parserContext.getRegistry().containsBeanDefinition(value)
                                        || !MonitorConfig.class.getName().equals(parserContext.getRegistry().getBeanDefinition(value).getBeanClassName()))) {
                                    // 兼容旧版本配置
                                    reference = convertMonitor(value);
                                } else if ("onreturn".equals(property)) {
                                    int index = value.lastIndexOf(".");
                                    String returnRef = value.substring(0, index);
                                    String returnMethod = value.substring(index + 1);
                                    reference = new RuntimeBeanReference(returnRef);
                                    beanDefinition.getPropertyValues().addPropertyValue("onreturnMethod", returnMethod);
                                } else if ("onthrow".equals(property)) {
                                    int index = value.lastIndexOf(".");
                                    String throwRef = value.substring(0, index);
                                    String throwMethod = value.substring(index + 1);
                                    reference = new RuntimeBeanReference(throwRef);
                                    beanDefinition.getPropertyValues().addPropertyValue("onthrowMethod", throwMethod);
                                } else {
                                    if ("ref".equals(property) && parserContext.getRegistry().containsBeanDefinition(value)) {
                                        BeanDefinition refBean = parserContext.getRegistry().getBeanDefinition(value);
                                        if (!refBean.isSingleton()) {
                                            throw new IllegalStateException("The exported service ref " + value + " must be singleton! Please set the " + value + " bean scope to singleton, eg: <bean id=\"" + value + "\" scope=\"singleton\" ...>");
                                        }
                                    }
                                    reference = new RuntimeBeanReference(value);
                                }
                                beanDefinition.getPropertyValues().addPropertyValue(property, reference); //设置属性对
                            }
                        }
                    }
                }
            }
        }
        NamedNodeMap attributes = element.getAttributes();
        int len = attributes.getLength();
        for (int i = 0; i < len; i++) {
            Node node = attributes.item(i);
            String name = node.getLocalName();
            if (!props.contains(name)) {
                if (parameters == null) {
                    parameters = new ManagedMap();
                }
                String value = node.getNodeValue();
                parameters.put(name, new TypedStringValue(value, String.class));
            }
        }
        if (parameters != null) {
            beanDefinition.getPropertyValues().addPropertyValue("parameters", parameters);
        }
        return beanDefinition;

        /**
         * parse()
         * 问题集：history-new
         * 1）待调试看数据
         * 2）6.1.6.4.5.1.1中此处的配置含义不清楚
         * 3）3.1.1.2中 此处的interface值会是啥？
         * 4）&&与||的优先级
         * 5）6中待调试 beanClass是否是  **Config 对象吗
         * 6）6.1.6.4.5.2中!parserContext.getRegistry().containsBeanDefinition(value)了解，能否测试覆盖
         * 7）onreturn、onthrow了解？属于什么功能，应用场景是啥？
         * 8）RuntimeBeanReference这个对象了解以及使用
         * 9）NamedNodeMap了解、Node了解以及LocalName
         * 10）spring中的RootBeanDefinition了解
         */
    }

    /**
     * 转换到监控配置MonitorConfig
     * 1）若监控配置的字符串monitor为空，则返回null
     * 2）若字符串monitor有GROUP_AND_VERION正则表达式匹配
     *  2.1）将字符串monitor按分隔符":"分隔
     *  2.2）若存在分隔符，则取出group、version
     *  2.3）若不存在分隔符，则将字符串作为group、并且version置为null
     * 3）创建监控配置MonitorConfig，并设置group、version值
     */
    protected static MonitorConfig convertMonitor(String monitor) {
        if (monitor == null || monitor.length() == 0) {
            return null;
        }
        if (GROUP_AND_VERION.matcher(monitor).matches()) {
            String group;
            String version;
            int i = monitor.indexOf(':');
            if (i > 0) {
                group = monitor.substring(0, i);
                version = monitor.substring(i + 1);
            } else {
                group = monitor;
                version = null;
            }
            MonitorConfig monitorConfig = new MonitorConfig();
            monitorConfig.setGroup(group);
            monitorConfig.setVersion(version);
            return monitorConfig;
        }
        return null;
        /**
         * 问题集：history-new
         * 1）能看懂正则表达式的串，并能使用Pattern、Matcher
         */
    }

    /**
     * 确定Class是否是基本类型primitive（基本的）
     * Class中的isPrimitive()方法：
     * Determines if the specified Class object represents a primitive type
     * （确定指定的Class对象是否表示基本的类型）
     * history-new 此处cls.isPrimitive()是否有cls == Boolean.class的功能，属不属于重复判断？
     */
    private static boolean isPrimitive(Class<?> cls) {
        return cls.isPrimitive() || cls == Boolean.class || cls == Byte.class
                || cls == Character.class || cls == Short.class || cls == Integer.class
                || cls == Long.class || cls == Float.class || cls == Double.class
                || cls == String.class || cls == Date.class || cls == Class.class;
    }

    /**
     * 解析多引用
     * 1）将value按逗号进行分隔
     * 2）遍历解析的数组
     *   2.1）若值不为空，构建RuntimeBeanReference实例，并加到ManagedList列表
     * 3）将构建的列表，添加到属性中  history 待调试
     */
    @SuppressWarnings("unchecked")
    private static void parseMultiRef(String property, String value, RootBeanDefinition beanDefinition,
                                      ParserContext parserContext) {/**@c history-h2 解析多引用 ？*/
        String[] values = value.split("\\s*[,]+\\s*");
        ManagedList list = null;
        for (int i = 0; i < values.length; i++) {
            String v = values[i];
            if (v != null && v.length() > 0) {
                if (list == null) {
                    list = new ManagedList();
                }
                list.add(new RuntimeBeanReference(v));
            }
        }
        beanDefinition.getPropertyValues().addPropertyValue(property, list);
    }

    /**
     * 解析嵌套元素（包含子节点）
     * 1）获取元素对应的所有子节点
     * 2）若节点列表不为空，遍历节点列表
     *  2.1）获取每个节点，若节点是Element实例时
     *   2.1.1）若节点名称NodeName与便签tag名称相等 或getLocalName与tag名称相等 history-new getLocalName待了解
     *    2.1.1.1）判断是否是第一个节点 first是否为true，若是第一个节点
     *         获取元素的default的值，若默认值为空，则将"default"属性置为false
     *    2.1.1.2）递归解析节点获取到BeanDefinition实例
     *    2.1.1.3）若解析的实例BeanDefinition不为空，则添加属性值
     *          键位输入的property，值为RuntimeBeanReference
     */
    private static void parseNested(Element element, ParserContext parserContext, Class<?> beanClass, boolean required, String tag, String property, String ref, BeanDefinition beanDefinition) {
        NodeList nodeList = element.getChildNodes();/**@c 解析嵌套元素 */
        if (nodeList != null && nodeList.getLength() > 0) {
            boolean first = true;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    if (tag.equals(node.getNodeName())
                            || tag.equals(node.getLocalName())) {
                        if (first) {
                            first = false;
                            String isDefault = element.getAttribute("default");
                            if (isDefault == null || isDefault.length() == 0) {
                                beanDefinition.getPropertyValues().addPropertyValue("default", "false");
                            }
                        }
                        BeanDefinition subDefinition = parse((Element) node, parserContext, beanClass, required); //递归循环解析
                        if (subDefinition != null && ref != null && ref.length() > 0) {
                            subDefinition.getPropertyValues().addPropertyValue(property, new RuntimeBeanReference(ref));
                        }
                    }
                }
            }
        }
    }

    /**
     * 解析属性，设置name属性的值
     * 1）若节点列表不为空，遍历节点列表
     * 2）获取节点Node，判断是否是Element实例
     *  2.1）若是，判断字符串"property"是否与node.getNodeName()或node.getLocalName()相同
     *   2.1.1）若相同，将Node强制转换到Element，并获取属性"name"的值
     *    2.1.1.1）若属性name字符串的值不为空，则获取节点Node中"value"、"ref"的属性值
     *     2.1.1.1.1）若value值不为空，则将属性name的值设置为value
     *     2.1.1.1.2）若ref值不为空，则将属性name的值设置为new RuntimeBeanReference(ref)
     *     2.1.1.1.3）若value、ref值都为空，则抛出未支持的异常
     */
    private static void parseProperties(NodeList nodeList, RootBeanDefinition beanDefinition) { //解析属性
        if (nodeList != null && nodeList.getLength() > 0) {//history-new 哪种情况进入该判断？
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) { //history-new Node、Element待了解实践？
                    if ("property".equals(node.getNodeName())
                            || "property".equals(node.getLocalName())) {
                        String name = ((Element) node).getAttribute("name");
                        if (name != null && name.length() > 0) {
                            String value = ((Element) node).getAttribute("value");
                            String ref = ((Element) node).getAttribute("ref");
                            if (value != null && value.length() > 0) {
                                beanDefinition.getPropertyValues().addPropertyValue(name, value);
                            } else if (ref != null && ref.length() > 0) {
                                beanDefinition.getPropertyValues().addPropertyValue(name, new RuntimeBeanReference(ref));
                            } else {
                                throw new UnsupportedOperationException("Unsupported <property name=\"" + name + "\"> sub tag, Only supported <property name=\"" + name + "\" ref=\"...\" /> or <property name=\"" + name + "\" value=\"...\" />");
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 解析节点列表的参数，并存入ManagedMap中
     * 1）若节点列表不为空
     *   1.1）遍历每个节点nodeList.item(i)
     *   1.2）若节点是Element的实例
     *     1.2.1）若节点的名字为"parameter"或节点的本地名字为"parameter"
     *      1.2.1.1）若ManagedMap为空，则初始化对象 history 了解ManagedMap
     *      1.2.1.2）获取节点的属性"key"、"value"对应的值
     *      1.2.1.3）获取节点的属性"hide"，并判断是否与"true"相等
     *              若相等，则为key加上前缀"."
     *      1.2.1.4）通过值value，构建TypedStringValue对象，并设置到ManagedMap中
     * 2）若节点列表为空，返回null
     */
    @SuppressWarnings("unchecked")
    private static ManagedMap parseParameters(NodeList nodeList, RootBeanDefinition beanDefinition) {
        if (nodeList != null && nodeList.getLength() > 0) {/**@c 解析<dubbo:parameter> */
            ManagedMap parameters = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    if ("parameter".equals(node.getNodeName())
                            || "parameter".equals(node.getLocalName())) {
                        if (parameters == null) {
                            parameters = new ManagedMap();
                        }
                        String key = ((Element) node).getAttribute("key");
                        String value = ((Element) node).getAttribute("value");
                        boolean hide = "true".equals(((Element) node).getAttribute("hide"));
                        if (hide) {
                            key = Constants.HIDE_KEY_PREFIX + key;
                        }
                        parameters.put(key, new TypedStringValue(value, String.class));
                    }
                }
            }
            return parameters;
        }
        return null;
    }

    /**
     * 解析方法元素 <dubbo:method>
     * 若节点列表不为空，遍历节点
     * 1）遍历每个节点
     *    1.1）若节点node是Element的实例
     *      1.1.1）将node强制转换为Element
     *      1.1.2）若节点的NodeName或LocalName为"method"
     *        1.1.2.1）取出元素Element属性name的值，作为方法名
     *        1.1.2.2）若方法名称为空，则抛出非法状态异常
     *        1.1.2.3）若方法列表为空，初始化方法列表
     *        1.1.2.4）解析节点元素，生成方法元素对应的bean，设置内容到ArgumentConfig
     *        1.1.2.5）方法名设置为 id + "." + methodName，
     *                 并构建BeanDefinitionHolder实例，并加到ManagedList方法列表中
     * 2）若处理的ManagedList方法列表不会空
     *    将"methods"属性值添加到根元素属性列表中
     */
    @SuppressWarnings("unchecked")
    private static void parseMethods(String id, NodeList nodeList, RootBeanDefinition beanDefinition,
                                     ParserContext parserContext) {/**@c 解析<dubbo:method> */
        if (nodeList != null && nodeList.getLength() > 0) {
            ManagedList methods = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    Element element = (Element) node;
                    if ("method".equals(node.getNodeName()) || "method".equals(node.getLocalName())) {
                        String methodName = element.getAttribute("name");
                        if (methodName == null || methodName.length() == 0) {
                            throw new IllegalStateException("<dubbo:method> name attribute == null");
                        }
                        if (methods == null) {
                            methods = new ManagedList();
                        }
                        BeanDefinition methodBeanDefinition = parse(((Element) node),
                                parserContext, MethodConfig.class, false);
                        String name = id + "." + methodName;
                        BeanDefinitionHolder methodBeanDefinitionHolder = new BeanDefinitionHolder(
                                methodBeanDefinition, name);
                        methods.add(methodBeanDefinitionHolder);
                    }
                }
            }
            if (methods != null) {
                beanDefinition.getPropertyValues().addPropertyValue("methods", methods);
            }
        }
    }

    /**
     * 解析方法元素 <dubbo:argument>  history 与parameters有啥不同
     * 若节点列表不为空，遍历节点
     * 1）遍历每个节点
     *    1.1）若节点node是Element的实例
     *      1.1.1）将node强制转换为Element
     *      1.1.2）若节点的NodeName或LocalName为"argument"
     *        1.1.2.1）取出元素Element属性"index"的值，作为参数下标argumentIndex
     *        1.1.2.2）若参数列表为空，初始化参数列表
     *        1.1.2.3）解析节点元素，设置内容到ArgumentConfig
     *        1.1.2.5）参数名设置为 id + "." + argumentIndex，
     *                 并构建BeanDefinitionHolder实例，并加到ManagedList方法列表中
     * 2）若处理的ManagedList方法列表不会空
     *    将"arguments"属性值添加到根元素属性列表中
     */
    @SuppressWarnings("unchecked")
    private static void parseArguments(String id, NodeList nodeList, RootBeanDefinition beanDefinition,
                                       ParserContext parserContext) {/**@c 解析<dubbo:argument> */
        if (nodeList != null && nodeList.getLength() > 0) {
            ManagedList arguments = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    Element element = (Element) node;
                    if ("argument".equals(node.getNodeName()) || "argument".equals(node.getLocalName())) {
                        String argumentIndex = element.getAttribute("index");
                        if (arguments == null) {
                            arguments = new ManagedList();
                        }
                        BeanDefinition argumentBeanDefinition = parse(((Element) node),
                                parserContext, ArgumentConfig.class, false);
                        String name = id + "." + argumentIndex;
                        BeanDefinitionHolder argumentBeanDefinitionHolder = new BeanDefinitionHolder(
                                argumentBeanDefinition, name);
                        arguments.add(argumentBeanDefinitionHolder);
                    }
                }
            }
            if (arguments != null) {
                beanDefinition.getPropertyValues().addPropertyValue("arguments", arguments);
            }
        }
    }

    /**
     * 重写spring BeanDefinitionParser bean解析器的解析方法parse
     * 1）spring中解析器的方法parse()的用途：解析元素
     *   @param element the element that is to be parsed into one or more {@link BeanDefinition BeanDefinitions}
     *   元素element（w3c的文档树）将被解析到一个或多个的spring 的bean中
     * 	 @param parserContext the object encapsulating（封装了） the current state（状态） of the parsing process（解析过程）;
     * 	 parserContext：解析的上下文，包含了解析过程的状态
     */
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        return parse(element, parserContext, beanClass, required);
    }

    /**
     *   BeanDefinition：描述的是一个bean的实例，有属性值、构造参数列表，以及为具体的实现方式提供更多的信息
     *   A BeanDefinition describes a bean instance, which has property values,
     *   constructor argument values, and further information supplied by
     *   concrete implementations.
     */

}