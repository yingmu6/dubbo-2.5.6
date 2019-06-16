package com.java.relative.basic.reflect;

import java.lang.reflect.Constructor;

/**
 * @author chensy
 * @date 2019-06-14 23:39
 */
public class BasicTest {
    public static void main(String[] args) throws Exception {
        Class clazz = Apple.class;
        Constructor constructor = clazz.getConstructor();  //获取构造函数，无参的
        System.out.println(constructor.getName());

        Constructor constructor2 = clazz.getConstructor(double.class); //获取有参数的构造函数，可以传多个参数
        System.out.println(constructor2.getName());

        // com.java.relative.basic.reflect.Apple 的简单类名为Apple
        System.out.println(clazz.getSimpleName());

        Apple apple = new Apple();
        apple.setWeight(12);

        Apple apple1 = apple;   //引用赋值，同时指向一个对象，只要一个引用改变，对应的值都改变
        apple1.setWeight(25);
        System.out.println(apple1.getWeight() + ";" + apple.getWeight());
    }
}
