/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.rpc.cluster.configurator;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.rpc.cluster.Configurator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * AbstractOverrideConfigurator
 *
 * @author william.liangf
 */
public abstract class AbstractConfigurator implements Configurator {

    private final URL configuratorUrl;

    public AbstractConfigurator(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("configurator url == null");
        }
        this.configuratorUrl = url;
    }

    public URL getUrl() {
        return configuratorUrl;
    }

    /**
     * 配置url
     * 1）若配置configuratorUrl以及传入的url为空或host，则不处理直接返回url
     * 2）若配置configuratorUrl存在有效端口时，若传入url的端口与配置url的端口相同
     *    则
     */
    public URL configure(URL url) {
        if (configuratorUrl == null || configuratorUrl.getHost() == null
                || url == null || url.getHost() == null) {
            return url;
        }
        if (configuratorUrl.getPort() != 0) {// override输入提供端地址，意图是控制提供者机器。可能在提供端生效 也可能在消费端生效
            if (url.getPort() == configuratorUrl.getPort()) {
                return configureIfMatch(url.getHost(), url);
            }
        } else {// 没有端口，override输入消费端地址 或者 0.0.0.0
            // 1.如果是消费端地址，则意图是控制消费者机器，必定在消费端生效，提供端忽略；
            // 2.如果是0.0.0.0可能是控制提供端，也可能是控制提供端
            if (url.getParameter(Constants.SIDE_KEY, Constants.PROVIDER).equals(Constants.CONSUMER)) {
                return configureIfMatch(NetUtils.getLocalHost(), url);// NetUtils.getLocalHost是消费端注册到zk的消费者地址
            } else if (url.getParameter(Constants.SIDE_KEY, Constants.CONSUMER).equals(Constants.PROVIDER)) {
                return configureIfMatch(Constants.ANYHOST_VALUE, url);//控制所有提供端，地址必定是0.0.0.0，否则就要配端口从而执行上面的if分支了
            }
        }
        return url;
    }
    //override://172.16.90.78:20883/com.alibaba.dubbo.demo.DemoService?category=configurators&disabled=true&dynamic=false&enabled=true

    /**
     * todo @csy 此方法的用途目前有点模糊，待调试了解
     * 1）若配置url的host为"0.0.0.0"，或输入的host与配置的host相等，则进入处理
     * 2）获取配置configuratorUrl中键"application"对应的值，默认是username值
     * 3）获取输入url中键"application"对应的值，默认是username值
     * 4）若configApplication为空，或为"*"，获取输入url中对应的"application"对应的值相等
     *    4.1）构建匹配条件key的集合基础的键，有"category"、"check"、"dynamic"、"enabled"
     *    4.2）遍历configuratorUrl的参数列表，若key以"~"开始 或与"application"相等，或与"side"相等 则将key加入到条件集合中。
     *        4.2.1）若值value不为空 并且不等于"*"，且value不等于 url中key截取到"~"对应的值，若都满足，返回url
     *    4.3）做配置处理doConfigure(URL currentUrl, URL configUrl)
     *        4.3.1）从configuratorUrl移除条件集合Set<String> condtionKeys
     *        4.3.2）做配置处理doConfigure处理
     */
    private URL configureIfMatch(String host, URL url) {
        if (Constants.ANYHOST_VALUE.equals(configuratorUrl.getHost()) || host.equals(configuratorUrl.getHost())) { //0.0.0.0 对任意机器有效
            String configApplication = configuratorUrl.getParameter(Constants.APPLICATION_KEY,
                    configuratorUrl.getUsername());
            String currentApplication = url.getParameter(Constants.APPLICATION_KEY, url.getUsername());
            if (configApplication == null || Constants.ANY_VALUE.equals(configApplication)
                    || configApplication.equals(currentApplication)) {
                Set<String> condtionKeys = new HashSet<String>();
                condtionKeys.add(Constants.CATEGORY_KEY);
                condtionKeys.add(Constants.CHECK_KEY);
                condtionKeys.add(Constants.DYNAMIC_KEY);
                condtionKeys.add(Constants.ENABLED_KEY);
                for (Map.Entry<String, String> entry : configuratorUrl.getParameters().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (key.startsWith("~") || Constants.APPLICATION_KEY.equals(key) || Constants.SIDE_KEY.equals(key)) {
                        condtionKeys.add(key);
                        if (value != null && !Constants.ANY_VALUE.equals(value)
                                && !value.equals(url.getParameter(key.startsWith("~") ? key.substring(1) : key))) {
                            return url;
                        }
                    }
                }
                return doConfigure(url, configuratorUrl.removeParameters(condtionKeys));
            }
        }
        return url;
    }

    /**
     * 根据priority、host依次排序
     * priority值越大，优先级越高；
     * priority相同，特定host优先级高于anyhost 0.0.0.0
     *
     * URL url1 = URL.valueOf("172.14.12.16");
     * URL url2 = URL.valueOf("anyhost");
     * URL url3 = URL.valueOf("0.0.0.0");
     * URL url4 = URL.valueOf("127.0.0.1");
     * System.out.println("一：" + url1.getHost().compareTo(url2.getHost()));
     * System.out.println("二：" + url1.getHost().compareTo(url3.getHost()));
     * System.out.println("三：" + url1.getHost().compareTo(url4.getHost()));
     * 输出   一：-48 ，  二：1  ，三：5
     */

    /**
     * 排序处理
     * 1）若比较的对象为空，则返回-1，表明当前配置Configurator的优先级小于参数指定的优先级
     * 2）先比较配置url与传入url的host值
     *   2.1）若相等，获取配置url以及传入url设置的权重值
             将权重值进行比较，权重值大的优先级高
     *   2.2）若不想等，直接返回比较的结果来决定优先级
     */
    public int compareTo(Configurator o) {
        if (o == null) {
            return -1;
        }
        /**@c host字符串比较 */
        int ipCompare = getUrl().getHost().compareTo(o.getUrl().getHost());
        if (ipCompare == 0) {//ip相同，根据priority排序
            int i = getUrl().getParameter(Constants.PRIORITY_KEY, 0),
                    j = o.getUrl().getParameter(Constants.PRIORITY_KEY, 0);
            if (i < j) {
                return -1;
            } else if (i > j) {
                return 1;
            } else {
                return 0;
            }
        } else {
            return ipCompare;
        }


    }

    protected abstract URL doConfigure(URL currentUrl, URL configUrl);

}
