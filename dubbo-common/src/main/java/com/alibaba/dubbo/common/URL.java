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
package com.alibaba.dubbo.common;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * URL - Uniform Resource Locator (Immutable（不可变的）, ThreadSafe)
 * <p>
 * url example:
 * <ul>
 * <li>http://www.facebook.com/friends?param1=value1&amp;param2=value2
 * <li>http://username:password@10.20.130.230:8080/list?version=1.0.0
 * <li>ftp://username:password@192.168.1.7:21/1/read.txt
 * <li>registry://192.168.1.7:9090/com.alibaba.service1?param1=value1&amp;param2=value2
 * </ul>
 * <p>
 * Some strange example below:
 * <ul>
 * <li>192.168.1.3:20880<br>
 * for this case, url protocol = null, url host = 192.168.1.3, port = 20880, url path = null
 * <li>file:///home/user1/router.js?type=script<br>
 * for this case, url protocol = null, url host = null, url path = home/user1/router.js
 * <li>file://home/user1/router.js?type=script<br>
 * for this case, url protocol = file, url host = home, url path = user1/router.js
 * <li>file:///D:/1/router.js?type=script<br>
 * for this case, url protocol = file, url host = null, url path = D:/1/router.js
 * <li>file:/D:/1/router.js?type=script<br>
 * same as above file:///D:/1/router.js?type=script
 * <li>/home/user1/router.js?type=script <br>
 * for this case, url protocol = null, url host = null, url path = home/user1/router.js
 * <li>home/user1/router.js?type=script <br>
 * for this case, url protocol = null, url host = home, url path = user1/router.js
 * </ul>
 *
 * @author william.liangf
 * @author ding.lid
 * @see java.net.URL
 * @see java.net.URI
 */
//服务地址
/**
 dubbo://10.168.113.101:20980/com.tuya.atop.client.service.user.IAtopUserInfoProvider?anyhost=true&application=airtake_service
 &default.retries=0&default.timeout=30000&dubbo=2.5.3&group=airtake&interface=com.tuya.atop.client.service.user.IAtopUserInfoProvider
 &methods=updateModifyTime,abateSession,getSessionByUid,getUserIdBySession,getUser,abateUserSessionList,getSession&pid=693
 &revision=0.0.2-20151209.122336-7&side=provider&threadpool=fixed&threads=300×tamp=1531221686039  **/
// dubbo的URL采用总线型方法，即配置都放在url里面的参数

/**
 * todo @csy-v1 URL学习实践？dubbo URL的用户以及应用场景
 */
public final class URL implements Serializable {//可进行序列化

    // 网络URL学习一下，对比一下与dubbo的自定义的URL的异同
    // URL语法结构  协议：//授权机构/路径?查询条件

    //与JAVA URL的异同：1）能获取、设置url中的参数，结构相似  2）dubbo中url没有openStream()、openConnection()
    private static final long serialVersionUID = -1985165475234910535L;

    private final String protocol;

    /**
     * @csy-v2 用户名：密码用户场景是怎样的？怎么使用
     * https://www.cnblogs.com/insane-Mr-Li/p/10142461.html URL与资源
     * 有些服务器都要求输入用户名和密码才允许用户访问数据，比如FTP，可选的
     *
     * URL是互联网资源的标准化名称、提供了一种定位互联网上任意资源的手段
     * 大多数URL协的语法都建立在下面9个部分构成的通用格式上：
     * <scheme>://<user>:<password>@<host>:<port>/<path>;<params>?<query>#<frag>
     */
    private final String username;

    private final String password;

    private final String host;

    private final int port;

    private final String path; //就是接口的完整名称，如com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService

    /**
     * 参数集合(附加参数集合，如side、application、generic等)
     * todo @csy-v2 断点分析，看里面存入的值以及写入的地方
     * 将参数值写到map集合中，然后根据key来取值
     */
    private final Map<String, String> parameters;

    // ==== cache ====  todo @csy-h1 URL中怎样使用缓存的？
    // 原子性并且不可以被序列化
    private volatile transient Map<String, Number> numbers;

    private volatile transient Map<String, URL> urls;

    private volatile transient String ip;

    private volatile transient String full;  //todo @csy-h1 用途？指完整的url吗？

    private volatile transient String identity;

    private volatile transient String parameter;

    private volatile transient String string;

    protected URL() {
        this.protocol = null;
        this.username = null;//用户名和密码可以不填
        this.password = null;
        this.host = null;
        this.port = 0;
        this.path = null;
        this.parameters = null;
    }

    //服务地址对外提供有多个构造函数，但内部使用一个，没有的值使用空或默认值
    public URL(String protocol, String host, int port) {
        this(protocol, null, null, host, port, null, (Map<String, String>) null);
    }

    public URL(String protocol, String host, int port, String[] pairs) { // 变长参数...与下面的path参数冲突，改为数组
        this(protocol, null, null, host, port, null, CollectionUtils.toStringMap(pairs));
    }

    public URL(String protocol, String host, int port, Map<String, String> parameters) {
        this(protocol, null, null, host, port, null, parameters);
    }

    public URL(String protocol, String host, int port, String path) {
        this(protocol, null, null, host, port, path, (Map<String, String>) null);
    }

    public URL(String protocol, String host, int port, String path, String... pairs) {
        this(protocol, null, null, host, port, path, CollectionUtils.toStringMap(pairs));
    }

    public URL(String protocol, String host, int port, String path, Map<String, String> parameters) {
        this(protocol, null, null, host, port, path, parameters);
    }

    public URL(String protocol, String username, String password, String host, int port, String path) {
        this(protocol, username, password, host, port, path, (Map<String, String>) null);
    }

    public URL(String protocol, String username, String password, String host, int port, String path, String... pairs) {
        this(protocol, username, password, host, port, path, CollectionUtils.toStringMap(pairs));
    }

    public URL(String protocol, String username, String password, String host, int port, String path, Map<String, String> parameters) {
        //在有密码的时候，需要有用户名
        if ((username == null || username.length() == 0)
                && password != null && password.length() > 0) {
            throw new IllegalArgumentException("Invalid url, password without username!");
        }
        this.protocol = protocol;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = (port < 0 ? 0 : port);
        // trim the beginning "/"
        while (path != null && path.startsWith("/")) {/**@c path 接口名*/
            path = path.substring(1);
        }
        this.path = path;
        if (parameters == null) {
            parameters = new HashMap<String, String>();
        } else {
            parameters = new HashMap<String, String>(parameters);
        }
        this.parameters = Collections.unmodifiableMap(parameters);//不能修改的Map，只能只读
    }

    /**
     * String与Url转换(把字符串的url构造成URL对象)
     * 从url字符串中解析出相关的参数，如：username、password、host等，然后构建URL对象
     * Parse url string
     *
     * @param url URL string
     * @return URL instance
     * @see URL
     */
    public static URL valueOf(String url) {
        if (url == null || (url = url.trim()).length() == 0) {
            throw new IllegalArgumentException("url == null");
        }
        String protocol = null;
        String username = null;
        String password = null;
        String host = null;
        int port = 0;
        String path = null;
        Map<String, String> parameters = null;
        int i = url.indexOf("?"); // seperator between body（主体） and parameters
        if (i >= 0) {
            String[] parts = url.substring(i + 1).split("\\&");//将参数按&符号分隔，所有的参数都会分隔
            parameters = new HashMap<String, String>();
            for (String part : parts) {
                part = part.trim();
                if (part.length() > 0) {
                    int j = part.indexOf('=');//返回-1 表示没有该字符出现
                    if (j >= 0) {
                        parameters.put(part.substring(0, j), part.substring(j + 1));//substring（start,end） [start,end) 左闭右开
                    } else {
                        parameters.put(part, part);//没有含等号，key、value都相等
                    }
                }
            }
            url = url.substring(0, i);
        }
        i = url.indexOf("://");
        if (i >= 0) {
            if (i == 0) throw new IllegalStateException("url missing protocol: \"" + url + "\"");
            protocol = url.substring(0, i);
            url = url.substring(i + 3);
        } else {
            // case: file:/path/to/file.txt
            i = url.indexOf(":/");
            if (i >= 0) {
                if (i == 0) throw new IllegalStateException("url missing protocol: \"" + url + "\"");
                protocol = url.substring(0, i);//值为：如  dubbo
                url = url.substring(i + 1);
            }
        }

        i = url.indexOf("/");  //此处的url已经去掉参数部分
        if (i >= 0) {
            path = url.substring(i + 1);  //path值为 如：com....IUserService
            url = url.substring(0, i);
        }
        i = url.indexOf("@");
        if (i >= 0) {      //判断是否包含用户名、密码
            username = url.substring(0, i);
            int j = username.indexOf(":");
            if (j >= 0) {
                password = username.substring(j + 1);
                username = username.substring(0, j);
            }
            url = url.substring(i + 1);
        }
        i = url.indexOf(":");
        if (i >= 0 && i < url.length() - 1) {
            port = Integer.parseInt(url.substring(i + 1));
            url = url.substring(0, i);
        }
        if (url.length() > 0) host = url;
        return new URL(protocol, username, password, host, port, path, parameters);
    }

    public static String encode(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        try {
            /**
             * 对URL中网际协议以外的字符编码
             * @csy-v2 为什么需要url需要编解码？
             *
             * https://www.ruanyifeng.com/blog/2010/02/url_encoding.html 关于URL编码
             * URL就是网址，只要上网，就一定会用到。
             * 一般来说，URL只能使用英文字母、阿拉伯数字和某些标点符号，不能使用其他文字和符号，这是因为网络标准RFC 1738做了硬性规定。如果包含了其它字符，比如汉字等，就需要编码后使用
             * 有几方面的编码，如：1）网址路径的编码，用的是utf-8编码 2）查询字符串的编码，用的是操作系统的默认编码
             * 3）GET和POST方法的编码，用的是网页的编码 4）Ajax调用的URL包含汉字不同浏览器有不同的编码
             * 保证客户端只用一种编码方法向服务器发出请求方式：使用Javascript先对URL编码，然后再向服务器提交，不要给浏览器插手的机会。因为Javascript的输出总是一致的，所以就保证了服务器得到的数据是格式统一的
             *
             * 编码：是信息从一种形式或格式转换为另一种形式的过程。编码是将用预先规定的方法 将文字、数字或其它对象编成数码，或将信息、数据转换成规定的电脉冲信号。解码，是编码的逆过程。
             * 编码是从一个字符，比如‘郭’，到一段二进制码流的过程。解码是从一段二进制码流到一个字符的过程。
             * https://blog.csdn.net/Marksinoberg/article/details/52254401 编码，解码，乱码，问题详解
             */
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String decode(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String getProtocol() {
        return protocol;
    }

    public URL setProtocol(String protocol) {//设置的时候生成新的URL
        return new URL(protocol, username, password, host, port, path, getParameters());
    }

    public String getUsername() {
        return username;
    }

    public URL setUsername(String username) {
        return new URL(protocol, username, password, host, port, path, getParameters());
    }

    public String getPassword() {
        return password;
    }

    public URL setPassword(String password) {
        return new URL(protocol, username, password, host, port, path, getParameters());
    }

    public String getAuthority() { // anthority：权威、全力
        if ((username == null || username.length() == 0)
                && (password == null || password.length() == 0)) {
            return null;
        }
        return (username == null ? "" : username)
                + ":" + (password == null ? "" : password);
    }

    public String getHost() {
        return host;
    }

    public URL setHost(String host) {
        return new URL(protocol, username, password, host, port, path, getParameters());
    }

    /**
     * 获取IP地址.
     * <p>
     * 请注意：
     * 如果和Socket的地址对比，
     * 或用地址作为Map的Key查找，
     * 请使用IP而不是Host，
     * 否则配置域名会有问题
     *
     * @return ip
     */
    public String getIp() {
        if (ip == null) {
            ip = NetUtils.getIpByHost(host);
        }
        return ip;
    }

    public int getPort() {
        return port;
    }

    public URL setPort(int port) {
        return new URL(protocol, username, password, host, port, path, getParameters());
    }

    public int getPort(int defaultPort) {
        return port <= 0 ? defaultPort : port;
    }

    public String getAddress() {
        return port <= 0 ? host : host + ":" + port; //判断port是否有效，若有效address为 host:port,否则为host
    }

    // 将字符串地址解析为URL
    public URL setAddress(String address) {
        int i = address.lastIndexOf(':');
        String host;
        int port = this.port;
        if (i >= 0) { //包含分隔符":"，则截取出host、post
            host = address.substring(0, i);
            port = Integer.parseInt(address.substring(i + 1));
        } else {
            host = address;
        }
        // 构造URL
        return new URL(protocol, username, password, host, port, path, getParameters());
    }

    public String getBackupAddress() {
        return getBackupAddress(0);
    }

    /**
     * @csy-v2 什么是回路地址？用途值的是啥
     * 回路地址：一般都会用来检查本地网络协议、基本数据接口等是否正常的
     * 本地回环地址指的是以127开头的地址（127.0.0.1 - 127.255.255.254），通常用127.0.0.1来表示
     *
     * https://blog.csdn.net/JohnLee_chun/article/details/54020119  127.0.0.1与localhost
     * localhost 是不经网卡传输！它不受网络防火墙和网卡相关的的限制。
     * 127.0.0.1 是通过网卡传输，依赖网卡，并受到网络防火墙和网卡相关的限制。
     */
    public String getBackupAddress(int defaultPort) {
        StringBuilder address = new StringBuilder(appendDefaultPort(getAddress(), defaultPort));
        String[] backups = getParameter(Constants.BACKUP_KEY, new String[0]);
        if (backups != null && backups.length > 0) {
            for (String backup : backups) {
                address.append(",");
                address.append(appendDefaultPort(backup, defaultPort));
            }
        }
        return address.toString();
    }

    public List<URL> getBackupUrls() { //获取回路Url
        List<URL> urls = new ArrayList<URL>();
        urls.add(this); //将当前Url加入列表
        String[] backups = getParameter(Constants.BACKUP_KEY, new String[0]);
        if (backups != null && backups.length > 0) {
            for (String backup : backups) {
                urls.add(this.setAddress(backup));
            }
        }
        return urls;
    }

    // 在ip地址上附加默认端口
    private String appendDefaultPort(String address, int defaultPort) {
        if (address != null && address.length() > 0
                && defaultPort > 0) {
            int i = address.indexOf(':');
            // ip地址如：10.20.153.10
            if (i < 0) {
                return address + ":" + defaultPort;
            // ip地址如：10.20.153.10:0
            } else if (Integer.parseInt(address.substring(i + 1)) == 0) {
                return address.substring(0, i + 1) + defaultPort;
            } //若指定了ip地址的端口，则不适用默认端口
        }
        return address;
    }

    public String getPath() {
        return path;
    }

    public URL setPath(String path) {
        return new URL(protocol, username, password, host, port, path, getParameters());
    }

    public String getAbsolutePath() {//绝对路径，以/开头
        if (path != null && !path.startsWith("/")) {
            return "/" + path;
        }
        return path;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getParameterAndDecoded(String key) {
        return getParameterAndDecoded(key, null);
    }

    public String getParameterAndDecoded(String key, String defaultValue) {/**@c 获取参数并且对参数解码*/
        return decode(getParameter(key, defaultValue));
    }

    /**
     * 获取参数中指定key对应的value
     * 若没有查询到，则查询默认key对应的值（即default.+key）
     */
    public String getParameter(String key) {
        String value = parameters.get(key);
        if (value == null || value.length() == 0) {
            value = parameters.get(Constants.DEFAULT_KEY_PREFIX + key);
        }
        return value;
    }

    /**
     * 从参数集合Map中获取key对应的值，若不存在则返回输入的默认值
     * 若key对应的键不存在，并且默认值为空，那么返回的值就为null
     */
    public String getParameter(String key, String defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value;
    }

    // 获取url中指定的key的值的数组，
    public String[] getParameter(String key, String[] defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        // @csy 20/05 finish match与pattern以及正则表达基本使用
        return Constants.COMMA_SPLIT_PATTERN.split(value);//将值按逗号分隔
    }

    /**
     * @csy 20/05 finish
     * 此处的numbers的用途？解：用来管理各种类型的参数值
     * https://www.runoob.com/java/java-number.html  Java Number & Math 类
     * 一般地，当需要使用数字的时候，我们通常使用内置数据类型，如：byte、int、long、double 等
     * 然而，在实际开发过程中，我们经常会遇到需要使用对象，而不是内置数据类型的情形。为了解决这个问题，Java 语言为每一个内置数据类型提供了对应的包装类。
     * 所有的包装类（Integer、Long、Byte、Double、Float、Short）都是抽象类 Number 的子类。
     *
     * 拆箱、装箱 https://juejin.im/post/5b5183e7e51d451912531cb5
     * Java中基础数据类型与它们的包装类进行运算时，编译器会自动帮我们进行转换，转换过程对程序员是透明的，
     * 这就是装箱和拆箱，装箱和拆箱可以让我们的代码更简洁易懂
     *
     * 拆箱：将包装类转换为基本类型数据，装箱：将基本类型数据封装为包装类
     * 编译器会自动帮我们进行装箱或拆箱.
     * 进行 = 赋值操作（装箱或拆箱）
     * 进行+，-，*，/混合运算 （拆箱）
     * 进行>,<,==比较运算（拆箱）
     * 调用equals进行比较（装箱）
     * ArrayList,HashMap等集合类 添加基础类型数据时（装箱）
     *
     * https://www.jianshu.com/p/0ce2279c5691  Java 自动装箱与拆箱的实现原理
     *
     * https://ghohankawk.github.io/2017/06/02/java-match/ java中的pattern和matcher的用法
     * https://www.runoob.com/java/java-regular-expressions.html  Java 正则表达式
     *
     */
    private Map<String, Number> getNumbers() {
        if (numbers == null) { // 允许并发重复创建
            numbers = new ConcurrentHashMap<String, Number>();
        }
        return numbers;
    }

    private Map<String, URL> getUrls() {
        if (urls == null) { // 允许并发重复创建
            urls = new ConcurrentHashMap<String, URL>();
        }
        return urls;
    }

    public URL getUrlParameter(String key) { //获取指定key的URL，并且设置到成员变量中
        URL u = getUrls().get(key);
        if (u != null) {
            return u;
        }
        String value = getParameterAndDecoded(key);
        if (value == null || value.length() == 0) {
            return null;
        }
        u = URL.valueOf(value);
        getUrls().put(key, u);
        return u;
    }

    // 获取参数对应的基本类型的值
    public double getParameter(String key, double defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.doubleValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        double d = Double.parseDouble(value);
        getNumbers().put(key, d); //Number适配各种基本类型
        return d;
    }

    /**
     * 判断本地缓存的map中是否存在值
     * 若存在则直接返回值，若不存在尝试查找默认值
     */
    public float getParameter(String key, float defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.floatValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        float f = Float.parseFloat(value);
        getNumbers().put(key, f);
        return f;
    }

    public long getParameter(String key, long defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.longValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        long l = Long.parseLong(value);
        getNumbers().put(key, l);
        return l;
    }

    //如果参数中没取到值，就使用默认的值
    public int getParameter(String key, int defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.intValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        getNumbers().put(key, i);
        return i;
    }

    //获取指定参数的值，并且返回对应的类型
    public short getParameter(String key, short defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.shortValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        short s = Short.parseShort(value);
        getNumbers().put(key, s);
        return s;
    }

    public byte getParameter(String key, byte defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.byteValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        byte b = Byte.parseByte(value);
        getNumbers().put(key, b);
        return b;
    }

    public float getPositiveParameter(String key, float defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        float value = getParameter(key, defaultValue);
        if (value <= 0) {
            return defaultValue;
        }
        return value;
    }

    public double getPositiveParameter(String key, double defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        double value = getParameter(key, defaultValue);
        if (value <= 0) {
            return defaultValue;
        }
        return value;
    }

    //获取参数中指定key的value，并且返回正数
    public long getPositiveParameter(String key, long defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        long value = getParameter(key, defaultValue);
        if (value <= 0) {
            return defaultValue;
        }
        return value;
    }

    // 获取绝对值，positive：绝对的
    public int getPositiveParameter(String key, int defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        int value = getParameter(key, defaultValue);
        if (value <= 0) {
            return defaultValue;
        }
        return value;
    }

    public short getPositiveParameter(String key, short defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        short value = getParameter(key, defaultValue);
        if (value <= 0) {
            return defaultValue;
        }
        return value;
    }

    public byte getPositiveParameter(String key, byte defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        byte value = getParameter(key, defaultValue);
        if (value <= 0) {
            return defaultValue;
        }
        return value;
    }

    public char getParameter(String key, char defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value.charAt(0);
    }

    public boolean getParameter(String key, boolean defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    //判断参数是否存在
    public boolean hasParameter(String key) {
        String value = getParameter(key);
        return value != null && value.length() > 0;
    }

    public String getMethodParameterAndDecoded(String method, String key) {
        return URL.decode(getMethodParameter(method, key));
    }

    public String getMethodParameterAndDecoded(String method, String key, String defaultValue) {
        return URL.decode(getMethodParameter(method, key, defaultValue));
    }

    //方法参数是指method列表中内容吗？还是指特定方法中的参数吗？ ： 附加参数map中取值，如"side" -> "consumer"
    public String getMethodParameter(String method, String key) {
        String value = parameters.get(method + "." + key); //todo @csy-h1 哪种情况是method + key作为键的
        if (value == null || value.length() == 0) {
            return getParameter(key);
        }
        return value;
    }

    // todo @csy-v2 方法中的参数是指什么？ 具体使用场景
    public String getMethodParameter(String method, String key, String defaultValue) {
        String value = getMethodParameter(method, key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value;
    }

    public double getMethodParameter(String method, String key, double defaultValue) {
        String methodKey = method + "." + key;
        Number n = getNumbers().get(methodKey);
        if (n != null) {
            return n.intValue();
        }
        String value = getMethodParameter(method, key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        double d = Double.parseDouble(value);
        getNumbers().put(methodKey, d);
        return d;
    }

    public float getMethodParameter(String method, String key, float defaultValue) {
        String methodKey = method + "." + key;
        Number n = getNumbers().get(methodKey);
        if (n != null) {
            return n.intValue();
        }
        String value = getMethodParameter(method, key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        float f = Float.parseFloat(value);
        getNumbers().put(methodKey, f);
        return f;
    }

    public long getMethodParameter(String method, String key, long defaultValue) {
        String methodKey = method + "." + key;
        Number n = getNumbers().get(methodKey);
        if (n != null) {
            return n.intValue();
        }
        String value = getMethodParameter(method, key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        long l = Long.parseLong(value);
        getNumbers().put(methodKey, l);
        return l;
    }

    /**
     * 获取方法参数 todo 0812 场景覆盖？待调试
     */
    public int getMethodParameter(String method, String key, int defaultValue) {
        String methodKey = method + "." + key;
        Number n = getNumbers().get(methodKey);
        if (n != null) {
            return n.intValue();
        }
        String value = getMethodParameter(method, key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        getNumbers().put(methodKey, i);
        return i;
    }

    public short getMethodParameter(String method, String key, short defaultValue) {
        String methodKey = method + "." + key;
        Number n = getNumbers().get(methodKey);
        if (n != null) {
            return n.shortValue();
        }
        String value = getMethodParameter(method, key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        short s = Short.parseShort(value);
        getNumbers().put(methodKey, s);
        return s;
    }

    public byte getMethodParameter(String method, String key, byte defaultValue) {
        String methodKey = method + "." + key;
        Number n = getNumbers().get(methodKey);
        if (n != null) {
            return n.byteValue();
        }
        String value = getMethodParameter(method, key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        byte b = Byte.parseByte(value);
        getNumbers().put(methodKey, b);
        return b;
    }

    public double getMethodPositiveParameter(String method, String key, double defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        double value = getMethodParameter(method, key, defaultValue);
        if (value <= 0) {
            return defaultValue;
        }
        return value;
    }

    public float getMethodPositiveParameter(String method, String key, float defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        float value = getMethodParameter(method, key, defaultValue);
        if (value <= 0) {
            return defaultValue;
        }
        return value;
    }

    public long getMethodPositiveParameter(String method, String key, long defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        long value = getMethodParameter(method, key, defaultValue);
        if (value <= 0) {
            return defaultValue;
        }
        return value;
    }

    public int getMethodPositiveParameter(String method, String key, int defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        int value = getMethodParameter(method, key, defaultValue);
        if (value <= 0) {
            return defaultValue;
        }
        return value;
    }

    public short getMethodPositiveParameter(String method, String key, short defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        short value = getMethodParameter(method, key, defaultValue);
        if (value <= 0) {
            return defaultValue;
        }
        return value;
    }

    public byte getMethodPositiveParameter(String method, String key, byte defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        byte value = getMethodParameter(method, key, defaultValue);
        if (value <= 0) {
            return defaultValue;
        }
        return value;
    }

    public char getMethodParameter(String method, String key, char defaultValue) {
        String value = getMethodParameter(method, key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value.charAt(0);
    }

    public boolean getMethodParameter(String method, String key, boolean defaultValue) {
        String value = getMethodParameter(method, key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public boolean hasMethodParameter(String method, String key) {
        if (method == null) {
            String suffix = "." + key;
            for (String fullKey : parameters.keySet()) {
                if (fullKey.endsWith(suffix)) {
                    return true;
                }
            }
            return false;
        }
        if (key == null) {
            String prefix = method + ".";
            for (String fullKey : parameters.keySet()) {
                if (fullKey.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
        String value = getMethodParameter(method, key);
        return value != null && value.length() > 0;
    }

    public boolean isLocalHost() {
        return NetUtils.isLocalHost(host) || getParameter(Constants.LOCALHOST_KEY, false);
    }

    public boolean isAnyHost() {
        return Constants.ANYHOST_VALUE.equals(host) || getParameter(Constants.ANYHOST_KEY, false);
    }

    // 添加参数并且对值进行编码
    public URL addParameterAndEncoded(String key, String value) {
        if (value == null || value.length() == 0) {
            return this;
        }
        return addParameter(key, encode(value));
    }

    // 添加url中的参数，添加基本类型的参数
    public URL addParameter(String key, boolean value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, char value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, byte value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, short value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, int value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, long value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, float value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, double value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, Enum<?> value) {
        if (value == null) return this;
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, Number value) {
        if (value == null) return this;
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, CharSequence value) {
        if (value == null || value.length() == 0) return this;
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, String value) {
        if (key == null || key.length() == 0
                || value == null || value.length() == 0) {
            return this;
        }
        // 如果没有修改，直接返回。
        if (value.equals(getParameters().get(key))) { // value != null
            return this;
        }

        Map<String, String> map = new HashMap<String, String>(getParameters());
        map.put(key, value);
        return new URL(protocol, username, password, host, port, path, map);
    }

    /**
     * 判断参数是否存在，如果不存在就添加参数 Absent（缺少的、不存在的），存在就返回当前url，this
     * 和普通的map不一样，普通的map若存在key，会把key对应的value更新，
     * 而addParameterIfAbsent不会更改已存在的key对应的value
     */
    public URL addParameterIfAbsent(String key, String value) {
        if (key == null || key.length() == 0
                || value == null || value.length() == 0) {
            return this;
        }
        if (hasParameter(key)) { //若key存在，则返回存在的对象
            return this;
        }
        Map<String, String> map = new HashMap<String, String>(getParameters()); // 不存在则往参数map中设置参数
        map.put(key, value);
        return new URL(protocol, username, password, host, port, path, map);
    }

    /**
     * Add parameters to a new url.
     * 将参数集合添加到新的url的参数集合中
     * 1）若参数集合为空，则返回当前的url，不处理
     * 2）遍历输入的参数集合
     *   2.1）获取每个参数key在当前url对应的值value
     *     2.1.1）若value为空，entry.getValue()不为空，表明值不相等，跳出循环
     *     2.1.2）若value不为空，若value与entry.getValue()不相等，，表明值不相等，跳出循环
     * 3）若相等标志hasAndEqual为true，没有修改直接返回
     * 4）将传入的参数集合与当前url的集合进行组装（相同的键，值会变更），并构建新的url
     *    相同key的时候，后面的值会把前面的更新，也就是输入的参数集合parameters会对当前url的集合getParameters()更新
     */
    public URL addParameters(Map<String, String> parameters) {
        if (parameters == null || parameters.size() == 0) {
            return this;
        }

        boolean hasAndEqual = true;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String value = getParameters().get(entry.getKey());
            if (value == null) {
                if (entry.getValue() != null) {
                    hasAndEqual = false;
                    break;
                }
            } else {
                if (!value.equals(entry.getValue())) {
                    hasAndEqual = false;
                    break;
                }
            }
        }
        // 如果没有修改，直接返回。
        if (hasAndEqual) return this;

        // 参数存在修改，则更新参数的值
        Map<String, String> map = new HashMap<String, String>(getParameters());
        map.putAll(parameters);
        return new URL(protocol, username, password, host, port, path, map);
    }

    public URL addParametersIfAbsent(Map<String, String> parameters) {/**@c todo @csy-h1 没有判断是否存在 */
        if (parameters == null || parameters.size() == 0) {
            return this;
        }
        Map<String, String> map = new HashMap<String, String>(parameters);
        map.putAll(getParameters());
        return new URL(protocol, username, password, host, port, path, map);
    }

    //可以添加若干个参数（按key、value方式传递参数）

    /**
     * 往url中添加多个参数
     * 1）若参数为空则返回不处理
     * 2）若参数个数不是偶数个，则抛出异常
     * 3）对参数个数除以2，key的下标依次为2*i，即0、2、4.... ，value的下标为2*i+1，即1，3，5...
     * 如：Constants.INTERFACE_KEY, child,Constants.CHECK_KEY, String.valueOf(false) ,四个参数
     */
    public URL addParameters(String... pairs) {
        if (pairs == null || pairs.length == 0) {
            return this;
        }
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Map pairs can not be odd number.");
        }
        Map<String, String> map = new HashMap<String, String>();
        int len = pairs.length / 2;
        for (int i = 0; i < len; i++) {
            map.put(pairs[2 * i], pairs[2 * i + 1]);
        }
        return addParameters(map);
    }

    public URL addParameterString(String query) {
        if (query == null || query.length() == 0) {
            return this;
        }
        return addParameters(StringUtils.parseQueryString(query));
    }

    public URL removeParameter(String key) {
        if (key == null || key.length() == 0) {
            return this;
        }
        return removeParameters(key);
    }

    public URL removeParameters(Collection<String> keys) {
        if (keys == null || keys.size() == 0) {
            return this;
        }
        return removeParameters(keys.toArray(new String[0]));
    }

    /**@c 移除参数后，重新构建URL */

    /**
     * 从URL参数集合中移除指定的参数，并构造URL返回
     * 1）若移除的参数列表为空，不做处理，返回当前的URL
     * 2）通过当前URL中getParameters()的参数集合构建新的map
     * 3）循环遍历需要移除的参数列表，依次从参数集合map中移除相关的key
     * 4）若新建的map和原有的map键的数目一致，表明没有移除到老的map的key，返回当前url
     * 5）若对老的map有移除，则将核心参数protocol、username等，以及参数map构建URL，并返回
     */
    public URL removeParameters(String... keys) {
        if (keys == null || keys.length == 0) {
            return this;
        }
        Map<String, String> map = new HashMap<String, String>(getParameters());
        for (String key : keys) {/**@c 需要移除的key */
            map.remove(key);
        }
        if (map.size() == getParameters().size()) {
            return this;
        }
        return new URL(protocol, username, password, host, port, path, map);
    }

    public URL clearParameters() {
        return new URL(protocol, username, password, host, port, path, new HashMap<String, String>());
    }

    /**
     * 从url中获取指定参数的值
     * 1）若key为url中的protocol、username、password、host、port、path，直接取url对应的属性
     * 2）若key是参数列表中的键，则从Map<String, String> parameters参数Map中获取
     *   （若key直接取不到，会以"default."+key，作为新key取值）
     */
    public String getRawParameter(String key) {
        if ("protocol".equals(key))
            return protocol;
        if ("username".equals(key))
            return username;
        if ("password".equals(key))
            return password;
        if ("host".equals(key))
            return host;
        if ("port".equals(key))
            return String.valueOf(port);
        if ("path".equals(key))
            return path;
        return getParameter(key);
    }

    /**
     * 将url转换到Map存储
     */
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<String, String>(parameters);
        if (protocol != null)
            map.put("protocol", protocol);
        if (username != null)
            map.put("username", username);
        if (password != null)
            map.put("password", password);
        if (host != null)
            map.put("host", host);
        if (port > 0)
            map.put("port", String.valueOf(port));
        if (path != null)
            map.put("path", path);
        return map;
    }

    public String toString() {
        if (string != null) {
            return string;
        }
        return string = buildString(false, true); // no show username and password
    }

    public String toString(String... parameters) {
        return buildString(false, true, parameters); // no show username and password
    }

    public String toIdentityString() {
        if (identity != null) {
            return identity;
        }
        return identity = buildString(true, false); // only return identity message, see the method "equals" and "hashCode"
    }

    public String toIdentityString(String... parameters) {
        return buildString(true, false, parameters); // only return identity message, see the method "equals" and "hashCode"
    }

    /**
     * 构建完整的url字符串（拼接用户、参数集合）
     */
    public String toFullString() {
        if (full != null) {
            return full;
        }
        return full = buildString(true, true);
    }

    public String toFullString(String... parameters) {
        return buildString(true, true, parameters);
    }

    public String toParameterString() {
        if (parameter != null) {
            return parameter;
        }
        return parameter = toParameterString(new String[0]);
    }

    public String toParameterString(String... parameters) {
        StringBuilder buf = new StringBuilder();
        buildParameters(buf, false, parameters);
        return buf.toString();
    }

    //构建url中的参数

    /**
     * 过滤url中的参数集合，选择附加append指定的参数parameters
     * 如url的参数有 [{"name","zhang"},{"age":12}]，过滤的参数列表为name，则会将参数name以及对应的值附加到url中
     * 1）判断url的参数集合parameters是否为空，在不为空的时候才进入操作
     * 2）通过传入的参数，构建参数列表includes
     * 3）通过url的参数集合parameters，构建新的TreeMap集合
     * 4）匹配url中的每个参数（对第一个参数特殊处理）
     *    若在指定的参数集合中，表明是需要附加的参数
     */
    private void buildParameters(StringBuilder buf, boolean concat, String[] parameters) {
        if (getParameters() != null && getParameters().size() > 0) {
            List<String> includes = (parameters == null || parameters.length == 0 ? null : Arrays.asList(parameters));
            boolean first = true;
            for (Map.Entry<String, String> entry : new TreeMap<String, String>(getParameters()).entrySet()) {
                if (entry.getKey() != null && entry.getKey().length() > 0
                        && (includes == null || includes.contains(entry.getKey()))) {
                    if (first) {
                        if (concat) {
                            buf.append("?");
                        }
                        first = false;
                    } else {
                        buf.append("&");
                    }
                    buf.append(entry.getKey());
                    buf.append("=");
                    buf.append(entry.getValue() == null ? "" : entry.getValue().trim());
                }
            }
        }
    }

    private String buildString(boolean appendUser, boolean appendParameter, String... parameters) {
        return buildString(appendUser, appendParameter, false, false, parameters);
    }

    /**@c 将url参数构建成字符串 */

    /**
     * 构建url字符串
     * 1）若协议不为空，则字符创为"protocol://"
     * 2）若需要拼接用户并且用户名不为空，则添加用户；
     *    若密码不为空，则添加密码，如 username:password，附加"@"
     * 3）是否使用ip
     *    若使用ip，则根据host查找ip，InetAddress.getByName(hostName).getHostAddress()，如"111.231.91.23"
     *    若使用host，直接返回host，如"www.xxx.com"
     * 4）host不为空，则附加到url字符串中
     *    若端口port不为空，则附加在url，如 host:port
     * 5）判断是否用 服务接口对应的key
     *    若使用：则path形式如 group/interface:version
     *    若不使用：则直接使用url中的path值即可
     * 6）若path值不为空，则添加到字符串中，如".../path"
     * 7）判断是否要添加参数，若需要添加参数，将参数附加到字符串中
     * 8）返回构建的字符串
     */
    private String buildString(boolean appendUser, boolean appendParameter, boolean useIP, boolean useService, String... parameters) {
        StringBuilder buf = new StringBuilder();
        if (protocol != null && protocol.length() > 0) {
            buf.append(protocol);
            buf.append("://");
        }
        if (appendUser && username != null && username.length() > 0) {
            buf.append(username);
            if (password != null && password.length() > 0) {
                buf.append(":");
                buf.append(password);
            }
            buf.append("@");
        }
        String host;
        if (useIP) {
            host = getIp();
        } else {
            host = getHost();
        }
        if (host != null && host.length() > 0) {
            buf.append(host);
            if (port > 0) {
                buf.append(":");
                buf.append(port);
            }
        }
        String path;
        if (useService) {
            path = getServiceKey();
        } else {
            path = getPath();
        }
        if (path != null && path.length() > 0) {
            buf.append("/");
            buf.append(path);
        }
        if (appendParameter) {
            buildParameters(buf, true, parameters);
        }
        return buf.toString();
    }

    // todo @csy-v2 装换为java URL的用途？
    public java.net.URL toJavaURL() {
        try {
            return new java.net.URL(toString());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public InetSocketAddress toInetSocketAddress() { //构建InetSocketAddress地址
        return new InetSocketAddress(host, port);
    }

    /**
     * 获取服务key，格式如："group/interface:version"
     * 1）获取服务接口名，若接口名为null，直接返回null
     * 2）从url的参数map中，获取分组group对应的值
     *    若存在分组，则将分组值添加到 构建的key中，如"group/"
     * 3）添加接口到构建的key中，如"group/interface"
     * 4）从url的参数map中，获取到版本号version
     *    若存在版本号，则添加到构建的key中，如"group/interface:version"
     * 5）返回构建的服务key对应的字符串
     */
    public String getServiceKey() {
        String inf = getServiceInterface(); //从URL中获取接口的完整名称
        if (inf == null) return null;
        StringBuilder buf = new StringBuilder();
        String group = getParameter(Constants.GROUP_KEY); //从URL中获取分组名称
        if (group != null && group.length() > 0) {
            buf.append(group).append("/");
        }
        buf.append(inf);
        String version = getParameter(Constants.VERSION_KEY);
        if (version != null && version.length() > 0) {
            buf.append(":").append(version);
        }
        return buf.toString();
    }

    //todo @csy-h1 用途？
    public String toServiceStringWithoutResolving() {
        return buildString(true, false, false, true);
    }

    /**
     * 转换服务字符串
     *   1）需要附加用户信息（用户名、密码） "username:password@"
     *   2）不需要附加参数
     *   3）使用ip的表现形式，"ip:port"
     *   4）使用服务接口形式，"group/interface:version"
     */
    public String toServiceString() {
        return buildString(true, false, true, true);
    }

    @Deprecated
    public String getServiceName() {
        return getServiceInterface();
    }

    /**
     * 获取 服务接口名
     * 从url中获取interface键对应的值，若没有该键，则以url中的path为默认值
     */
    public String getServiceInterface() {
        return getParameter(Constants.INTERFACE_KEY, path);
    }

    public URL setServiceInterface(String service) {
        return addParameter(Constants.INTERFACE_KEY, service);
    }

    /**
     * @see #getParameter(String, int)
     * @deprecated Replace to <code>getParameter(String, int)</code>
     */
    @Deprecated
    public int getIntParameter(String key) {
        return getParameter(key, 0);
    }

    /**
     * @see #getParameter(String, int)
     * @deprecated Replace to <code>getParameter(String, int)</code>
     */
    @Deprecated
    public int getIntParameter(String key, int defaultValue) {
        return getParameter(key, defaultValue);
    }

    /**
     * @see #getPositiveParameter(String, int)
     * @deprecated Replace to <code>getPositiveParameter(String, int)</code>
     */
    @Deprecated
    public int getPositiveIntParameter(String key, int defaultValue) {
        return getPositiveParameter(key, defaultValue);
    }

    /**
     * @see #getParameter(String, boolean)
     * @deprecated Replace to <code>getParameter(String, boolean)</code>
     */
    @Deprecated
    public boolean getBooleanParameter(String key) {
        return getParameter(key, false);
    }

    /**
     * @see #getParameter(String, boolean)
     * @deprecated Replace to <code>getParameter(String, boolean)</code>
     */
    @Deprecated
    public boolean getBooleanParameter(String key, boolean defaultValue) {
        return getParameter(key, defaultValue);
    }

    /**
     * @see #getMethodParameter(String, String, int)
     * @deprecated Replace to <code>getMethodParameter(String, String, int)</code>
     */
    @Deprecated
    public int getMethodIntParameter(String method, String key) {
        return getMethodParameter(method, key, 0);
    }

    /**
     * @see #getMethodParameter(String, String, int)
     * @deprecated Replace to <code>getMethodParameter(String, String, int)</code>
     */
    @Deprecated
    public int getMethodIntParameter(String method, String key, int defaultValue) {
        return getMethodParameter(method, key, defaultValue);
    }

    /**
     * @see #getMethodPositiveParameter(String, String, int)
     * @deprecated Replace to <code>getMethodPositiveParameter(String, String, int)</code>
     */
    @Deprecated
    public int getMethodPositiveIntParameter(String method, String key, int defaultValue) {
        return getMethodPositiveParameter(method, key, defaultValue);
    }

    /**
     * @see #getMethodParameter(String, String, boolean)
     * @deprecated Replace to <code>getMethodParameter(String, String, boolean)</code>
     */
    @Deprecated
    public boolean getMethodBooleanParameter(String method, String key) {
        return getMethodParameter(method, key, false);
    }

    /**
     * @see #getMethodParameter(String, String, boolean)
     * @deprecated Replace to <code>getMethodParameter(String, String, boolean)</code>
     */
    @Deprecated
    public boolean getMethodBooleanParameter(String method, String key, boolean defaultValue) {
        return getMethodParameter(method, key, defaultValue);
    }

    /**
     * @csy 20/05/18 为啥重新hashcode、equals方法（覆盖equals比较的是内容相等，==比较的是对象的地址是否相等）
     * https://juejin.im/post/5a4379d4f265da432003874c
     *
     * 覆盖equals时需要满足的准则： 1）自反性，x.equals(x) 是true  2）对称性，x.equals(y)是true，y.equals(x)也是true
     * 3）传递性，x.equals(y)是true，y.equals(z) 是true，那么x.equals(z) 也是true。 4）一致性，多次调用x.quals(y) 始终返回true或false
     * 覆盖equals时一定要覆盖hashCode equals函数里面一定要是Object类型作为参数
     * 如果两个对象equals，那么它们的hashCode必然相等，但是hashCode相等，equals不一定相等。
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + port;
        result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        URL other = (URL) obj;
        if (host == null) {
            if (other.host != null)
                return false;
        } else if (!host.equals(other.host))
            return false;
        if (parameters == null) {
            if (other.parameters != null)
                return false;
        } else if (!parameters.equals(other.parameters))
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        if (port != other.port)
            return false;
        if (protocol == null) {
            if (other.protocol != null)
                return false;
        } else if (!protocol.equals(other.protocol))
            return false;
        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;
        return true;
    }

}
