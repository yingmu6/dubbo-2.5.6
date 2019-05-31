package com.java.relative.basic.abstract_test;

/**
 * @author chensy
 * @date 2019-05-31 17:42
 */
public class AbstractDemo
{
    public static void main(String [] args)
    {
        /* 以下是不允许的，会引发错误 */
        // 抽象方法不能直接实例化
        // Employee e = new Employee("George W.", "Houston, TX", 43);

       // System.out.println("\n Call mailCheck using Employee reference--");
        //e.mailCheck();

        Salary s = new Salary("Mohd Mohtashim", "Ambehta, UP", 3, 3600.00);
        Employee e = new Salary("John Adams", "Boston, MA", 2, 2400.00);

        System.out.println("Call mailCheck using Salary reference --");
        s.mailCheck();

        System.out.println("Call mailCheck using Employee reference--");
        e.mailCheck();

        e.sayHello();

        s.sayHello();
    }

    /**
     *
     * 1. 抽象类不能被实例化，如果被实例化，就会报错，编译无法通过。只有抽象类的非抽象子类可以创建对象。
     * 2. 抽象类中不一定包含抽象方法，但是有抽象方法的类必定是抽象类。
     * 3. 抽象类中的抽象方法只是声明，不包含方法体，就是不给出方法的具体实现也就是方法的具体功能。
     * 4. 构造方法，类方法（用 static 修饰的方法）不能声明为抽象方法。
     * 5. 抽象类的子类必须给出抽象类中的抽象方法的具体实现，除非该子类也是抽象类
     *
     */
}
