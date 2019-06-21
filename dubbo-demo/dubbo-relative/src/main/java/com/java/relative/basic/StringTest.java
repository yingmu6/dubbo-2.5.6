package com.java.relative.basic;

/**
 * String测试
 * @author chensy
 * @date 2019-06-21 14:41
 */
public class StringTest {
    public static void main(String[] args) {
        StringTest test = new StringTest();
        test.placeholder();
    }

    //占位符测试
    public void placeholder() {
        //String code = String.format("hello, %s, %s", "world"); 缺少参数，没有和占位符对应
        String code = String.format("hello, %s, %s", "world", "!");
        System.out.println(code);
    }
}
