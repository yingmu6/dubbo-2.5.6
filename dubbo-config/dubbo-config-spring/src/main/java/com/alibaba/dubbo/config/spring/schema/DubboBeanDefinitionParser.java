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

    /**
     * 1.通过spring 解析到 xml对应的元素、属性值
     * 2.写到dubbo的config对象中，如ServiceConfig、ReferenceConfig等
     * Element 解析的XML元素、ParserContext解析的上下文、beanClass XML元素对应的bean类型，required是否必须
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
        if (ProtocolConfig.class.equals(beanClass)) {// 对ProtocolConfig类型处理
            for (String name : parserContext.getRegistry().getBeanDefinitionNames()) {
                BeanDefinition definition = parserContext.getRegistry().getBeanDefinition(name);
                PropertyValue property = definition.getPropertyValues().getPropertyValue("protocol");
                if (property != null) {
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
                // 解析属性
                parseProperties(element.getChildNodes(), classDefinition);
                beanDefinition.getPropertyValues().addPropertyValue("ref", new BeanDefinitionHolder(classDefinition, id + "Impl"));
            }
        } else if (ProviderConfig.class.equals(beanClass)) {
            // 解析嵌套元素
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
    }

    /**
     * 确定Class是否是基本类型primitive（基本的）
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
     * 2）遍历解析的数组，若值不为空，构建RuntimeBeanReference实例，并加到ManagedList列表
     * 3）将构建的列表，添加到属性中
     */
    @SuppressWarnings("unchecked")
    private static void parseMultiRef(String property, String value, RootBeanDefinition beanDefinition,
                                      ParserContext parserContext) {
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
     *   2.1.1）若节点名称NodeName与便签tag名称相等 或getLocalName与tag名称相等
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