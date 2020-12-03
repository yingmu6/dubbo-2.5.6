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
import com.alibaba.dubbo.common.io.UnsafeStringWriter;
import com.alibaba.dubbo.common.json.JSON;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * StringUtils
 *
 * @author qian.lei
 */

public final class StringUtils {

    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final Logger logger = LoggerFactory.getLogger(StringUtils.class);
    private static final Pattern KVP_PATTERN = Pattern.compile("([_.a-zA-Z0-9][-_.a-zA-Z0-9]*)[=](.*)"); //key value pair pattern.

    private static final Pattern INT_PATTERN = Pattern.compile("^\\d+$");

    private StringUtils() {
    }

    public static boolean isBlank(String str) {
        if (str == null || str.length() == 0)
            return true;
        return false;
    }

    /**
     * is empty string.
     *
     * @param str source string.
     * @return is empty.
     */
    public static boolean isEmpty(String str) {
        if (str == null || str.length() == 0)
            return true;
        return false;
    }

    /**
     * is not empty string.
     *
     * @param str source string.
     * @return is not empty.
     */
    public static boolean isNotEmpty(String str) {
        return str != null && str.length() > 0;
    }

    /**
     * 判断两个字符串是否相等（对java底层的String的equals进行封装，对空参数进行判断处理）
     * 1）若两个参数s1，s2都为空，返回true
     * 2）若只有一个参数为空，返回false
     * 3）返回String的equals比较结果
     */
    public static boolean isEquals(String s1, String s2) { //判断两个字符串是否相等
        if (s1 == null && s2 == null)
            return true;
        if (s1 == null || s2 == null)
            return false;
        return s1.equals(s2);
    }

    /**
     * is integer string.
     *
     * @param str
     * @return is integer
     */
    public static boolean isInteger(String str) {
        if (str == null || str.length() == 0)
            return false;
        return INT_PATTERN.matcher(str).matches();
    }

    public static int parseInteger(String str) {
        if (!isInteger(str))
            return 0;
        return Integer.parseInt(str);
    }

    /**
     * Returns true if s is a legal Java identifier.<p>
     * <a href="http://www.exampledepot.com/egs/java.lang/IsJavaId.html">more info.</a>
     */
    public static boolean isJavaIdentifier(String s) {
        if (s.length() == 0 || !Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断value是否包含在字符串中
     * 1）若待比较的字符串为空，则返回false
     * 2）将待比较的values进行分隔，获取到字符串数组
     *    判断value是否在values对应的数组中
     */
    public static boolean isContains(String values, String value) {
        if (values == null || values.length() == 0) {
            return false;
        }
        return isContains(Constants.COMMA_SPLIT_PATTERN.split(values), value);
    }

    /**
     * 判断值value是否包含在values对应的数组中
     * 1）判断值value和字符数组values是否为空，
     *    若不为空，依次遍历数组，与指定的值value进行比较是否相等，只要其中一个相等，就结束并返回true
     */
    public static boolean isContains(String[] values, String value) {
        if (value != null && value.length() > 0 && values != null && values.length > 0) {
            for (String v : values) {
                if (value.equals(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断字符串是否是数字
     * 依次遍历字符串的所有字符，若有一个字符不是数字，则字符串不是数字
     * 若全部字符都是数字，则字符串为数字组成的字符串
     */
    public static boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (Character.isDigit(str.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param e
     * @return string
     */
    public static String toString(Throwable e) { // 10/22 输出待调试
        UnsafeStringWriter w = new UnsafeStringWriter();
        PrintWriter p = new PrintWriter(w);
        p.print(e.getClass().getName());
        if (e.getMessage() != null) {
            p.print(": " + e.getMessage());
        }
        p.println();
        try {
            e.printStackTrace(p);
            return w.toString();
        } finally {
            p.close();
        }
    }

    /**
     * @param msg
     * @param e
     * @return string
     */
    public static String toString(String msg, Throwable e) {
        UnsafeStringWriter w = new UnsafeStringWriter();
        w.write(msg + "\n");
        PrintWriter p = new PrintWriter(w);
        try {
            e.printStackTrace(p);
            return w.toString();
        } finally {
            p.close();
        }
    }

    /**
     * translat.
     *
     * @param src  source string.
     * @param from src char table.
     * @param to   target char table.
     * @return String.
     */
    public static String translat(String src, String from, String to) {
        if (isEmpty(src)) return src;
        StringBuilder sb = null;
        int ix;
        char c;
        for (int i = 0, len = src.length(); i < len; i++) {
            c = src.charAt(i);
            ix = from.indexOf(c);
            if (ix == -1) {
                if (sb != null)
                    sb.append(c);
            } else {
                if (sb == null) {
                    sb = new StringBuilder(len);
                    sb.append(src, 0, i);
                }
                if (ix < to.length())
                    sb.append(to.charAt(ix));
            }
        }
        return sb == null ? src : sb.toString();
    }

    /**
     * split.
     *
     * @param ch char.
     * @return string array.
     */
    public static String[] split(String str, char ch) {
        List<String> list = null;
        char c;
        int ix = 0, len = str.length();
        for (int i = 0; i < len; i++) {
            c = str.charAt(i);
            if (c == ch) {
                if (list == null)
                    list = new ArrayList<String>();
                list.add(str.substring(ix, i));
                ix = i + 1;
            }
        }
        if (ix > 0)
            list.add(str.substring(ix));
        return list == null ? EMPTY_STRING_ARRAY : (String[]) list.toArray(EMPTY_STRING_ARRAY);
    }

    /**
     * join string.
     *
     * @param array String array.
     * @return String.
     */
    public static String join(String[] array) {
        if (array.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (String s : array)
            sb.append(s);
        return sb.toString();
    }

    /**
     * join string like javascript.
     *
     * @param array String array.
     * @param split split
     * @return String.
     */
    public static String join(String[] array, char split) {
        if (array.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0)
                sb.append(split);
            sb.append(array[i]);
        }
        return sb.toString();
    }

    /**
     * join string like javascript.
     *
     * @param array String array.
     * @param split split
     * @return String.
     * StringBuffer ：线程安全
     * StringBuilder ：线程不安全
     */
    public static String join(String[] array, String split) {
        if (array.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0)
                sb.append(split);
            sb.append(array[i]);
        }
        return sb.toString();
    }

    public static String join(Collection<String> coll, String split) {
        if (coll.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        boolean isFirst = true; //根据是否是第一个，拼接分隔符，很简洁
        for (String s : coll) {
            if (isFirst) isFirst = false;
            else sb.append(split);
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * parse key-value pair.
     * 按指定分隔符分隔字符串，并写入参数映射集合
     *
     * @param str           string.
     * @param itemSeparator item separator.
     * @return key-value map;
     */
    private static Map<String, String> parseKeyValuePair(String str, String itemSeparator) {
        String[] tmp = str.split(itemSeparator);
        Map<String, String> map = new HashMap<String, String>(tmp.length);
        for (int i = 0; i < tmp.length; i++) { //str为application=api_demo&dubbo=2.0.0&interface=com.alibaba.dubbo.demo.ApiDemo&methods=sayHello,sayApi&pid=20130&side=consumer&timestamp=1564548724133
            Matcher matcher = KVP_PATTERN.matcher(tmp[i]); //以&分隔，设置map键值对
            if (matcher.matches() == false) //判断是否是
                continue;
            map.put(matcher.group(1), matcher.group(2));
        }
        return map;
    }

    public static String getQueryStringValue(String qs, String key) {
        Map<String, String> map = StringUtils.parseQueryString(qs);
        return map.get(key);
    }

    /**
     * parse query string to Parameters.（对url字符串进行解析，返回键值对的映射集合）
     *
     * @param qs query string.
     * @return Parameters instance.
     */
    public static Map<String, String> parseQueryString(String qs) {
        if (qs == null || qs.length() == 0)
            return new HashMap<String, String>();
        return parseKeyValuePair(qs, "\\&");
    }

    /**
     * 从参数中获取值构建服务ServiceKey
     * 包含group、interface、version
     */
    public static String getServiceKey(Map<String, String> ps) {
        StringBuilder buf = new StringBuilder();
        String group = ps.get(Constants.GROUP_KEY);
        if (group != null && group.length() > 0) {
            buf.append(group).append("/");
        }
        buf.append(ps.get(Constants.INTERFACE_KEY));
        String version = ps.get(Constants.VERSION_KEY);
        if (version != null && version.length() > 0) {
            buf.append(":").append(version);
        }
        return buf.toString();
    }

    public static String toQueryString(Map<String, String> ps) {
        StringBuilder buf = new StringBuilder();
        if (ps != null && ps.size() > 0) {
            for (Map.Entry<String, String> entry : new TreeMap<String, String>(ps).entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && key.length() > 0
                        && value != null && value.length() > 0) {
                    if (buf.length() > 0) {
                        buf.append("&");
                    }
                    buf.append(key);
                    buf.append("=");
                    buf.append(value);
                }
            }
        }
        return buf.toString();
    }

    /**
     * 将驼峰式的写法换位任意分隔符的算法
     * 1.循环取出字符，判断是否有大写字母
     * 2.若存在，第一次取出大写字母之前的子串，添加上分隔符，然后加上大写字母的小写形式
     * 3.之后的字符，若是小写字母直接添加，若是大写字符，转换为小写字母添加
     */

    /**
     * 将驼峰式字符串转换为使用分隔符分隔
     * 1）若字符串为空，不做处理直接返回
     * 2）遍历字符串中的字符
     *  2.1）取出字符判断是否是大写字符
     *    2.1.1）若是大写字符
     *      2.1.1.1）若目标字符串buf为空，初始化对象（延迟初始化）
     *        2.1.1.1.1）若下标大于0，表明之前还有字符串没拼接，就取当期字符的子串做拼接
     *      2.1.1.2）将分隔符添加到目标字符串（若首字符是大写字符，不拼接分隔符）
     *      2.1.1.3）将大写字符转换为小写字符添加到目标字符串中
     *    2.1.2）若不是大写字符，且目标字符串不为空
     *      2.1.2.1）直接添加append到目标字符串中
     * 3）若目标处理字符串为空，即没有包含大写字母，则原串返回
     *
     * 示例：
     * a）camelToSplitName("wwThH", "_")  的结果为 "ww_th_h"
     * b）不含有大写字母的字符串直接返回，camelToSplitName("aa", "_") 的结果为"aa"首字符
     * c）首字目为大写字符，首字符不处理，camelToSplitName("ABC", "—") 的结果为"a—b—c"首字符
     */
    public static String camelToSplitName(String camelName, String split) { //将有大写字母的地方用指定的分隔符分隔
        if (camelName == null || camelName.length() == 0) {
            return camelName;
        }
        StringBuilder buf = null;
        for (int i = 0; i < camelName.length(); i++) {
            char ch = camelName.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                if (buf == null) {
                    buf = new StringBuilder(); //延迟初始化对象，只有存在大写字母才初始化
                    if (i > 0) {
                        buf.append(camelName.substring(0, i));
                    }
                }
                if (i > 0) {
                    buf.append(split);
                }
                buf.append(Character.toLowerCase(ch));/**@c 将大写字符转换为小写字符 */
            } else if (buf != null) {
                buf.append(ch);
            }
        }
        return buf == null ? camelName : buf.toString();
    }

    public static void main(String[] args) {
//        System.out.println(camelToSplitName("ABC", "—"));
    }

    public static String toArgumentString(Object[] args) {/**@c 将参数列表转换为字符串  10/22 待调试 */
        StringBuilder buf = new StringBuilder();
        for (Object arg : args) {
            if (buf.length() > 0) {
                buf.append(Constants.COMMA_SEPARATOR);
            }
            if (arg == null || ReflectUtils.isPrimitives(arg.getClass())) {
                buf.append(arg);
            } else {
                try {
                    buf.append(JSON.json(arg));
                } catch (IOException e) {
                    logger.warn(e.getMessage(), e);
                    buf.append(arg);
                }
            }
        }
        return buf.toString();
    }
}