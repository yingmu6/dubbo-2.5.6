package com.java.relative.basic.extend.order;

/**
 * @author chensy
 * @date 2019-05-30 09:35
 */
public class Base1 {
    static {
        System.out.println("父类2静态代码块");
    }

    {
        System.out.println("父类2普通代码块");
    }


    public Base1() {
        System.out.println("父类2构造方法");
    }


    static int print(String str) {
        System.out.println(str);
        return 2;
    }

    private int i = print("父类2普通变量 i ");
    private static int j = print("父类2静态变量 j");

}
