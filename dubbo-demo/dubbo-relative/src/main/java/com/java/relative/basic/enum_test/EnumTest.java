package com.java.relative.basic.enum_test;

/**
 * 枚举测试 Enum、Enumeration
 * https://www.ibm.com/developerworks/cn/java/j-lo-enum/index.html    Java 语言中Enum 类型的使用介绍
 *
 * https://blog.csdn.net/aitangyong/article/details/54896269  熟练使用java的Enum、EnumSet、EnumMap
 * @author chensy
 * @date 2019-06-13 14:42
 */
public class EnumTest {
    private enum WeekDay { Mon, Tue, Wed, Thu }; //定义Enum类型，简单类型，最后一个枚举值后不用跟任何一个符号

    public static void main(String[] args) {
        EnumTest enumTest = new EnumTest();
        WeekDay weekDay = WeekDay.Tue;
        enumTest.basic(weekDay);

        enumTest.useEnumMethod ();
    }

    public void basic(WeekDay weekDay) { //声明枚举对象
         switch (weekDay) {
             case Mon :  //直接使用枚举值
                 System.out.println("Mon out");
                 break;
             case Tue:
                 System.out.println("Tue out");
                 break;
             case Wed:
                 System.out.println("Wed out");
                 break;
             case Thu:
                 System.out.println("Thu out");
                 break;
         }
    }

    public void useEnumMethod () { //通过Enum的valueOf方法构建枚举对象
        WeekDay weekDay = Enum.valueOf(WeekDay.class, "Mon");
        System.out.println(weekDay.name()); //枚举常量的名称
        System.out.println(weekDay.ordinal()); //枚举常量的序列，从0开始
    }


}






/**
 * https://www.cnblogs.com/skywang12345/p/3311275.html  集合系列18之 Iterator和Enumeration比较
 *
 * 1）枚举类型的定义也非常的简单，用 enum 关键字加上名称和大括号包含起来的枚举值体即可
 * 2）通过类似 class 的定义来给枚举进行定制。比如要给 enum 类型增加属性
 * 3）enum 类型不支持 public 和 protected 修饰符的构造方法，因此构造函数一定要是 private 或 friendly 的。也正因为如此，
 * 所以枚举对象是无法在程序中通过直接调用其构造方法来初始化的。
 * 4）switch中可以使用枚举值
 *
 * 类似使用常量，但是常量 （类型不安全、没有命名空间等等）
 */
