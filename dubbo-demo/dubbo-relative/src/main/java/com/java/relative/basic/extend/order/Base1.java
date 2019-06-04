package com.java.relative.basic.extend.order;

/**
 * 面向对象三大特性的总结  https://www.jianshu.com/p/7b08e274bc0b
 * 1：封装  ：封装是一种对类的保护机制，防止该类的属性、方法及数据被外部类随意地访问。（访问修饰符、this、内部类）
 * 2：继承  ：将多个子类中共同的部分抽离出来形成父类，实现代码稳定部分的封装，也大大提高了代码的复用性。继承的初始化顺序是先初始化父类再初始化子类。
 * 3：多态  ：多态也称为动态绑定，是指在执行期间才来判断所引用对象的实际类型，再根据其实际的类型调用相应的方法。
 * 只通过父类就能够引用不同的子类，这就是多态（向上转型、抽象类、接口）
 *
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

/**
 * 全面理解Java中继承关系  https://blog.csdn.net/hxhaaj/article/details/81174764
 * 类的方法来自与自身以及父类继承
 *
 * 继承是从已有类中派生出新的类，新的类能吸收已有类的属性和方法，并且能拓展新的属性和行为
 * 子类被称为派生类，父类又被称为超类
 *
 * 继承的好处
 *   提高了代码的复用性
 *   提高了代码的维护性
 *   让类与类之间产生了关系，是多态的前提
 * 缺点
 *   增加了耦合性
 *
 * 子类只能继承父类的所有非私有的成员变量和方法
 * 子类不能继承获得父类的构造方法，但是可以通过super关键字来访问父类构造方法
 */
