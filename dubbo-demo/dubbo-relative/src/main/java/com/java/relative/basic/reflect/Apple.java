package com.java.relative.basic.reflect;

import java.lang.reflect.Constructor;

/**
 * 苹果类
 * @author chensy
 * @date 2019-06-12 23:30
 */
public class Apple implements Fruits {
    private double weight;
    @Override
    public void setWeight(double weight) {
       this.weight = weight;
    }
    public Apple() {

    }
    public Apple(double weight) {
      this.weight = weight;
    }

    @Override
    public double getWeight() {
        return this.weight;
    }

    public static void main(String[] args) {
        Class fruit = Fruits.class;
        Class apple = Apple.class;
        /** 判断当前类或接口是否与指定的类或接口相同， 若不同，再看是否与超类或类实现的接口相同 */
        System.out.println(fruit.isAssignableFrom(fruit)); //返回true
        System.out.println(apple.isAssignableFrom(apple)); //返回true
        System.out.println(apple.isAssignableFrom(fruit)); //返回false
        System.out.println(fruit.isAssignableFrom(apple)); //返回true

        try {
            Constructor app = apple.getConstructor();
            Constructor app2 = apple.getConstructor(double.class); //获取包含指定参数的构造函数
            System.out.println(app.getName());
            Class[] param = app2.getParameterTypes();
            System.out.println(app2.getName() + "," + param[0].getSimpleName());

            //获取指定类型的构造器，并传入实参进行实例
            Apple appleIns = (Apple) apple.getConstructor(double.class).newInstance(12.9);
            System.out.println(appleIns.getWeight());

            //获取无参的构造器
            Apple appleIns2 = (Apple) apple.getConstructor().newInstance();
            appleIns2.setWeight(133);
            System.out.println(appleIns2.getWeight());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    /**
     * public boolean isAssignableFrom(Class<?> cls)
     *
     * 确定由此类对象表示的类或接口是否与由指定的Class类表示的类或接口相同或是超类或类接口。 如果是，则返回true ; 否则返回false 。
     * 如果此类对象表示基本类型，则如果指定的类参数正好是类对象，则此方法返回true ; 否则返回false 。
     *
     */
}
