package com.java.relative.basic.reflect;

import com.java.relative.basic.regular.RegularTest;

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

        // https://www.jianshu.com/p/a777be7024b9 （关键是，是否相同、是否是超类、是否是接口）
        //isAssignableFrom 判断一个class（类）是否与另一个class相同，或者是否是另一个class的超类或接口
        //通常调用格式是 Class1.isAssignableFrom (Class2)  Assignable：可分配的
        System.out.println("assign:" + Apple.class.isAssignableFrom(Fruits.class)); //输出false 因为A与B不相同，并且A不是B的接口，也不是B的超类
        System.out.println("assign2:" + Apple.class.isAssignableFrom(RedApple.class)); //输出true  因为A是B的超类
        System.out.println("assign3:" + RedApple.class.isAssignableFrom(Apple.class)); //输出false
        System.out.println("assign4:" + Fruits.class.isAssignableFrom(Apple.class));   //输出true 因为A是B的超类
        System.out.println("assign5:" + Fruits.class.isAssignableFrom(Fruits.class));  //输出true 因为A与B相同

        //instanceof (是用来判断一个对象实例是否是一个类或接口的实例。)
        // 调用格式 对象实例名 instanceof TypeName（类名或接口名）

        System.out.println("instance: " + (apple instanceof Apple)); //输出true  apple是Apple类的实例
        System.out.println("instance2: " + (apple instanceof Fruits)); //输出true apple是接口Fruits的实例
        System.out.println("instance3: " + (apple instanceof RedApple)); //输入false apple不是子类的实例

        System.out.println("superClass: " + RedApple.class.getSuperclass()); //获取超类的class
    }
}
