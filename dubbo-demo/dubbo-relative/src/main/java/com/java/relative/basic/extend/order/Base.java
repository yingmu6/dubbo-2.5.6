package com.java.relative.basic.extend.order;

/*
 * @author chensy
 * @date 2019-05-30 09:31
 */
public class Base { //父类

    static {
        System.out.println("父类静态代码块 k");
    }

    private static int j = print("父类静态变量 j");


    public Base() {
        System.out.println("父类构造方法b");
    }

    private int i = print("父类普通变量 i ");

    {
        System.out.println("父类普通代码块a");
    }

    static int print(String str) {
        System.out.println(str);
        return 2;
    }
}
