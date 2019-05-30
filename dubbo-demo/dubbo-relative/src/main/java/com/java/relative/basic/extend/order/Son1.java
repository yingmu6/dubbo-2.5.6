package com.java.relative.basic.extend.order;

/**
 * @author chensy
 * @date 2019-05-30 09:36
 */
public class Son1 extends Base1 {

    static {
        System.out.println("子类2静态代码块");
    }

    {
        System.out.println("子类2普通代码块");
    }


    public Son1() {
        System.out.println("子类2构造方法");
    }


    static int print(String str) {
        System.out.println(str);
        return 2;
    }

    private int k = print("子类2普通变量k");
    private static int l = print("子类2静态变量l");
}
