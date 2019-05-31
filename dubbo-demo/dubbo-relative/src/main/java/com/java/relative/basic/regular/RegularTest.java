package com.java.relative.basic.regular;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * 正则表达式定义了字符串的模式。
 * 正则表达式可以用来搜索、编辑或处理文本。
 * 正则表达式并不仅限于某一种语言，但是在每种语言中有细微的差别。
 *
 * @author chensy
 * @date 2019-05-30 23:26
 */
public class RegularTest {
    public static void main(String args[]) {

        // 按指定模式在字符串查找
        String line = "This order was placed for QT3000! OK?";

        // 表达式语法
        String pattern = "(\\D*)(\\d+)(.*)";
       // String pattern = "for";

        //Pattern 与 Matcher 都没有公有构造方法
        // 创建 Pattern（模式） 对象 : 将给定的正则表达式编译为模式。
        Pattern r = Pattern.compile(pattern);

        // 现在创建 matcher 对象  ： 创建一个匹配器，匹配给定的输入与此模式。
        Matcher m = r.matcher(line);
        if (m.find( )) {

            System.out.println("正则表达式" + r.pattern());
            // 字符串查找
            System.out.println("Found value 0: " + m.group(0));

            // 字符传替换
            System.out.println(m.replaceAll("test"));
//
//            int num = m.groupCount();
//            for (int i = 0; i< num; i++) {
//                // 字符串查找
//                System.out.println("Found value 0: " + m.group(i));
//
//                // 字符传替换
//                System.out.println(m.replaceAll("test"));
//
//            }
        } else {
            System.out.println("NO MATCH");
        }
    }

    /**
     * java.util.regex 包主要包括以下三个类：
     *
     * pattern 对象是一个正则表达式的编译表示。Pattern 类没有公共构造方法。要创建一个 Pattern 对象，
     * 你必须首先调用其公共静态编译方法，
     * 它返回一个 Pattern 对象。该方法接受一个正则表达式作为它的第一个参数。
     *
     * Matcher 对象是对输入字符串进行解释和匹配操作的引擎。与Pattern 类一样，Matcher
     * 也没有公共构造方法。你需要调用 Pattern 对象的 matcher 方法来获得一个 Matcher 对象。
     *
     * PatternSyntaxException 是一个非强制异常类，它表示一个正则表达式模式中的语法错误。
     */

    /**
     * https://blog.csdn.net/yaerfeng/article/details/28855587
     *
     * [abc]	 a, b 或 c（简单类）
     * [^abc]	 除 a, b 或 c 之外的任意字符（取反）
     * [a-zA-Z]	 a 到 z，或 A 到 Z，包括（范围）
     * [a-d[m-p]]  a 到 d，或 m 到 p：[a-dm-p]（并集）
     * [a-z&&[def]]	  d，e 或 f（交集）
     * [a-z&&[^bc]]	  除 b 和 c 之外的 a 到 z 字符：[ad-z]（差集）
     *
     *
     * 预定义字符类
     * \D	非数字字符：[^0-9]
     * \d	数字字符：[0-9]
     * \w	单词字符：[a-zA-Z_0-9]
     * \W	非单词字符：[^\w]
     *
     *
     * 量词（quantifiers）允许指定匹配出现的次数
     * X+ (贪婪)、X+?	（勉强）、X++	（侵占）    描述：匹配 X 一次或多次
     *
     * 分组：用中括号或圆括号
     * 如，[abc]+表示一次或者多次的 a 或 b 或 c，(abc)+表示一次或者多次的“abc”组。
     *
     * 边界匹配器
     * ^  行首
     * $  行尾
     * \G 上一个匹配的结尾
     *
     *
     * 常量
     * Pattern.CASE_INSENSITIVE	  等价 (?i)
     * Pattern.COMMENTS	 等价 (?x)
     *
     *
     * 规则可参考常用正则表达式
     * https://c.runoob.com/front-end/854
     */
}
