package com.java.relative.basic.reflect;

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
    }

    /**
     * public boolean isAssignableFrom(Class<?> cls)
     *
     * 确定由此类对象表示的类或接口是否与由指定的Class类表示的类或接口相同或是超类或类接口。 如果是，则返回true ; 否则返回false 。
     * 如果此类对象表示基本类型，则如果指定的类参数正好是类对象，则此方法返回true ; 否则返回false 。
     *
     */
}
