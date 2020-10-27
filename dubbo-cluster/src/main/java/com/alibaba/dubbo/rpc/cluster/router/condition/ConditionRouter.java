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
package com.alibaba.dubbo.rpc.cluster.router.condition;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Router;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ConditionRouter
 *
 * 条件路由：
 * @author william.liangf
 */
public class ConditionRouter implements Router, Comparable<Router> {/**@c 具有路由功能和比较器功能 */

    private static final Logger logger = LoggerFactory.getLogger(ConditionRouter.class);
    private static Pattern ROUTE_PATTERN = Pattern.compile("([&!=,]*)\\s*([^&!=,\\s]+)");/**@c 路由规则正则表达式 */
    private final URL url;
    private final int priority;/**@c 优先级：数字越大、级别越高 */
    private final boolean force;
    private final Map<String, MatchPair> whenCondition;
    private final Map<String, MatchPair> thenCondition;

    /**
     * 构建条件路由
     * 1）设置基本属性，url、priority（优先权）、force（是否强制）
     * 2）获取url中"rule"参数对应的值并解码
     * 3）若rule值为空，抛出非法参数异常
     * 4）将rule中的"consumer." 以及"provider."都字符串都替换为空串
     * 5）获取规则rule字符串中"=>"的下标，构建因果条件
     *   5.1）构建条件规则，如果没包含"=>" 则条件为null，否则取"=>"前面的字符串为条件
     *   5.2）构建结果规则，如果没包含"=>" 则将整个rule字符串作为结果，否则将"=>"后面的字符串为结果
     *   5.3）若whenRule为空或"true"，则返回空的Map，否则解析规则获取到条件Map
     *   5.4）若thenRule为空或"true"，则返回null，否则解析规则获取到条件Map
     *   5.5）设置当前条件路由的属性值whenCondition、thenCondition
     */
    public ConditionRouter(URL url) {
        this.url = url;
        this.priority = url.getParameter(Constants.PRIORITY_KEY, 0);
        this.force = url.getParameter(Constants.FORCE_KEY, false);
        try {
            String rule = url.getParameterAndDecoded(Constants.RULE_KEY);
            if (rule == null || rule.trim().length() == 0) {
                throw new IllegalArgumentException("Illegal route rule!");
            }
            rule = rule.replace("consumer.", "").replace("provider.", "");
            int i = rule.indexOf("=>");
            String whenRule = i < 0 ? null : rule.substring(0, i).trim();
            String thenRule = i < 0 ? rule.trim() : rule.substring(i + 2).trim();
            /**@c 判断条件是否为空 */
            Map<String, MatchPair> when = StringUtils.isBlank(whenRule) || "true".equals(whenRule) ? new HashMap<String, MatchPair>() : parseRule(whenRule);
            Map<String, MatchPair> then = StringUtils.isBlank(thenRule) || "false".equals(thenRule) ? null : parseRule(thenRule);
            // NOTE: When条件是允许为空的，外部业务来保证类似的约束条件
            this.whenCondition = when;
            this.thenCondition = then;
        } catch (ParseException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * 解析路由规则，生成条件condition与匹配对MatchPair的映射集合Map history 此处匹配细节待测试用例调试
     * 1）若规则为空，则返回空的集合Map
     * 2）将规则字符串按规则的正式表达式构建Matcher
     * 3）若存在符合正则表达式的字符串，获取到分隔符separator、匹配的内容content
     *  3.1）若分隔符separator为空，初始化匹配对MatchPair，并设置到条件Map中condition
     *  3.2）若separator为"&"，表明KV开始，后面的串是键值对的串
     *     3.2.1）若conditionMap中内容content条件为空，初始化匹配对MatchPair，并设置到条件Map中condition
     *     3.2.2）若conditionMap中内容content条件不为空，则获取condition对应的匹配对MatchPair，并赋值给pair
     *  3.3）若separator为"="，表明KV的Value部分开始
     *     3.3.1）若pair为空，则抛出非法路由规则异常
     *     3.3.2）将匹配matches的集合以及匹配的内容content，加入到value集合Set<String>中
     *  3.4）若separator为"!="，表明KV的Value部分开始
     *     3.4.1）若pair为空，则抛出非法路由规则异常
     *     3.4.2）将不匹配mismatches的集合以及匹配的内容content，加入到value集合Set<String>中
     *  3.5）若separator为"!="，表明KV的Value部分的多个条目
     *     3.5.1）若values为空，则抛出非法路由规则异常
     *     3.5.2）将匹配的内容content，添加到values集合中
     *  3.6）若separator都不在上述的指定范围，则抛出解析异常
     */
    private static Map<String, MatchPair> parseRule(String rule)
            throws ParseException {
        Map<String, MatchPair> condition = new HashMap<String, MatchPair>();
        if (StringUtils.isBlank(rule)) {
            return condition;
        }
        // 匹配或不匹配Key-Value对
        MatchPair pair = null;
        // 多个Value值
        Set<String> values = null;
        final Matcher matcher = ROUTE_PATTERN.matcher(rule);
        while (matcher.find()) { // 逐个匹配
            String separator = matcher.group(1);
            String content = matcher.group(2);
            // 表达式开始
            if (separator == null || separator.length() == 0) {
                pair = new MatchPair();
                condition.put(content, pair);
            }
            // KV开始
            else if ("&".equals(separator)) {
                if (condition.get(content) == null) {
                    pair = new MatchPair();
                    condition.put(content, pair);
                } else {
                    pair = condition.get(content);
                }
            }
            // KV的Value部分开始
            else if ("=".equals(separator)) {
                if (pair == null)
                    throw new ParseException("Illegal route rule \""
                            + rule + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".", matcher.start());

                values = pair.matches;
                values.add(content);
            }
            // KV的Value部分开始
            else if ("!=".equals(separator)) {
                if (pair == null)
                    throw new ParseException("Illegal route rule \""
                            + rule + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".", matcher.start());

                values = pair.mismatches;
                values.add(content);
            }
            // KV的Value部分的多个条目
            else if (",".equals(separator)) { // 如果为逗号表示
                if (values == null || values.size() == 0)
                    throw new ParseException("Illegal route rule \""
                            + rule + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".", matcher.start());
                values.add(content);
            } else {
                throw new ParseException("Illegal route rule \"" + rule
                        + "\", The error char '" + separator + "' at index "
                        + matcher.start() + " before \"" + content + "\".", matcher.start());
            }
        }
        return condition;
    }

    /**
     * 对指定的调用列表invokers进行条件路由：
     * 1）先判断调用url中的方法名是否存在MatchPair的集合中，matchWhen(url, invocation)，
     *    若不在匹配的集合中，不处理直接返回
     * 2）判断是否在黑名单中，thenCondition == null，若为黑名单，返回空的结果resul，不处理
     * 3）依次将满足条件的invoker加入结果列表中
     */
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation)
            throws RpcException {
        if (invokers == null || invokers.size() == 0) {
            return invokers;
        }
        try {
            if (!matchWhen(url, invocation)) { /**@c 将URL中的内容与调用信息invocation中的内容进行比较 */
                return invokers;
            }
            List<Invoker<T>> result = new ArrayList<Invoker<T>>();
            if (thenCondition == null) {/**@c 黑名单，不返回调用列表，不可调用 */
                logger.warn("The current consumer in the service blacklist. consumer: " + NetUtils.getLocalHost() + ", service: " + url.getServiceKey());
                return result;
            }
            for (Invoker<T> invoker : invokers) {
                if (matchThen(invoker.getUrl(), url)) {/**@c 若能匹配， 则加入到结果列表 */
                    result.add(invoker);
                }
            }
            if (result.size() > 0) {
                return result;
            } else if (force) {
                logger.warn("The route result is empty and force execute. consumer: " + NetUtils.getLocalHost() + ", service: " + url.getServiceKey() + ", router: " + url.getParameterAndDecoded(Constants.RULE_KEY));
                return result;
            }
        } catch (Throwable t) {
            logger.error("Failed to execute condition router rule: " + getUrl() + ", invokers: " + invokers + ", cause: " + t.getMessage(), t);
        }
        return invokers;
    }

    public URL getUrl() {
        return url;
    }

    /**
     * 条件路由router比较（以权重值priority进行比较）
     * 1）若待比较的router为空或者不是ConditionRouter的Class，返回1
     * 2）若Router的类型是ConditionRouter
     *  2.1）比较当前Router的与待比较的Router的priority相等
     *   2.1.1）若priority相等，项判断当前Router的url与待比较Router的url的完整url字符串是否相同
     *   2.1.2）若priority不相等，判断当前Router的priority与待比较路由router的priority大小比较
     */
    public int compareTo(Router o) {
        if (o == null || o.getClass() != ConditionRouter.class) {
            return 1;
        }
        ConditionRouter c = (ConditionRouter) o;
        return this.priority == c.priority ? url.toFullString().compareTo(c.url.toFullString()) : (this.priority > c.priority ? 1 : -1);
    }

    boolean matchWhen(URL url, Invocation invocation) {
        return whenCondition == null || whenCondition.isEmpty() || matchCondition(whenCondition, url, null, invocation);
    }

    private boolean matchThen(URL url, URL param) {
        return !(thenCondition == null || thenCondition.isEmpty()) && matchCondition(thenCondition, url, param, null);
    }

    /**
     * 匹配条件：将调用信息invocation与设置的条件进行匹配
     */
    private boolean matchCondition(Map<String, MatchPair> condition, URL url, URL param, Invocation invocation) {
        Map<String, String> sample = url.toMap(); /**@c 将url拆分存储到map中 */
        boolean result = false;
        for (Map.Entry<String, MatchPair> matchPair : condition.entrySet()) {
            String key = matchPair.getKey();
            String sampleValue;
            //get real invoked method name from invocation
            if (invocation != null && (Constants.METHOD_KEY.equals(key) || Constants.METHODS_KEY.equals(key))) { /**@c 若key为"method"或"methods"，从invocation中获取方法名 */
                sampleValue = invocation.getMethodName();
            } else {
                sampleValue = sample.get(key); /**@c 若key不是方法名，则从url对应的map中获取回应的值 */
            }
            if (sampleValue != null) {
                if (!matchPair.getValue().isMatch(sampleValue, param)) {
                    return false;
                } else {
                    result = true;
                }
            } else {
                //not pass the condition
                if (matchPair.getValue().matches.size() > 0) {
                    return false;
                } else {
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * 匹配对（包含匹配的集合及不匹配的集合）
     */
    private static final class MatchPair {/**@c 内部类  */
        final Set<String> matches = new HashSet<String>();
        final Set<String> mismatches = new HashSet<String>();

        /**
         * 将字符串与url进行模式匹配
         * （若待比较的字符串与matches集合中的任意一个元素匹配上，即为true；若与mismatches，集合中的任意一个元素匹配上，即为false）
         * 1）若集合matches不为空，集合mismatches为空
         *   1.1）只要字符串与matches其中一个元素匹配上，即返回true。若都匹配不上，则返回false
         * 2）若集合mismatches不为空，集合mismatches为空
         *   2.1）只要字符串与mismatches其中一个元素匹配上，即返回false。若都匹配不上，则返回true
         * 3）若集合matches不为空，且集合mismatches不为空
         *   3.1）优先比较集合mismatches，只要字符串与mismatches其中一个元素匹配上，即返回false，
         *   3.2）集合matches若都匹配不上，则匹配集合matches，若字符串与matches其中一个元素匹配上，即返回true
         *   3.3）若集合mismatches、matches都没比较上，则返回false
         * 4）若matches、mismatches集合都为空，返回false
         */
        private boolean isMatch(String value, URL param) {
            if (matches.size() > 0 && mismatches.size() == 0) {
                for (String match : matches) {
                    if (UrlUtils.isMatchGlobPattern(match, value, param)) {
                        return true;
                    }
                }
                return false;
            }

            if (mismatches.size() > 0 && matches.size() == 0) {
                for (String mismatch : mismatches) {
                    if (UrlUtils.isMatchGlobPattern(mismatch, value, param)) {
                        return false;
                    }
                }
                return true;
            }

            /**
             * 此处存在优先级 mismatches > matches
             */
            if (matches.size() > 0 && mismatches.size() > 0) {
                //when both mismatches and matches contain the same value, then using mismatches first
                for (String mismatch : mismatches) {
                    if (UrlUtils.isMatchGlobPattern(mismatch, value, param)) {
                        return false;
                    }
                }
                for (String match : matches) {
                    if (UrlUtils.isMatchGlobPattern(match, value, param)) {
                        return true;
                    }
                }
                return false;
            }
            return false;
        }
    }
}