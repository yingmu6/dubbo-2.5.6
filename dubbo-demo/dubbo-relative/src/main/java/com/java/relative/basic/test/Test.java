package com.java.relative.basic.test;

/**
 * @author chensy
 * @date 2019-05-30 17:14
 */
public class Test {
    public static void main(String[] args) {
        System.out.println(System.getProperties().get("java.library.path"));

//        String str = "erre#s";
//        System.out.println(str.charAt('#'));

        String type = "spring=com.alibaba.dubbo.config.spring.extension.SpringExtensionFactory";
        int i = type.charAt('#');
        System.out.println(i); //此处输出105

        System.out.println(type.indexOf("pr"));

        System.out.println(type.indexOf('r'));
        System.out.println("字符串" + type.substring(3,3)); //取不到值，返回空串，左开右闭，此处相当于从【3，2】的值

        //# 对应的十进制值是35，而35位置的值为i，i对应的10进制的值为105

        //char	charAt(int index)
        //返回 char指定索引处的值。

        /**
         * 本来是想找出字符串中某个字符出现的位置，但是用错方法了。charAt(int index) 是获取指定字符的位置
         */
    }
}
