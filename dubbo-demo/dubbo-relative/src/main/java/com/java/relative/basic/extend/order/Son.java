package com.java.relative.basic.extend.order;

/**
 * @author chensy
 * @date 2019-05-30 09:32
 */
//子类
public class Son extends Base {

    private int k = print("子类普通变量k");
    private static int l = print("子类静态变量l");

    static {
        System.out.println("子类静态代码块");
    }

    {
        System.out.println("子类普通代码块");
    }


    public Son() {
        System.out.println("子类构造方法");
    }


    static int print(String str) {
        System.out.println(str);
        return 2;
    }

}
