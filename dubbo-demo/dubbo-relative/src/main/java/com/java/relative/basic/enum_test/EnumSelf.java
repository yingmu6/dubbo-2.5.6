package com.java.relative.basic.enum_test;

/**
 * 自定义枚举类型
 * @author chensy
 * @date 2019-06-13 15:34
 */
public enum EnumSelf { //与类class声明方式相似
    ZHANG_SAN(12), //多个枚举值用逗号分隔，此处至少有一个枚举值
    LI_SI(14);

    private int age; //成员变量不能放在枚举变量前面
    private EnumSelf (int age) { //构造函数是私有的
       this.age = age;
    }
    public int getAge() {
        return this.age;
    }

    public static void main(String[] args) {
        EnumSelf zhang = EnumSelf.ZHANG_SAN;
        System.out.println(zhang.getAge());

        EnumSelf lisi = EnumSelf.LI_SI;  //创建枚举对象
        System.out.println(lisi.getAge());
    }
}

/**
 *
 * 1) enum 类型不支持 public 和 protected 修饰符的构造方法，因此构造函数一定要是 private 或 friendly 的。也正因为如此，所以枚举对象是无法在程序中通过直接调用其构造方法来初始化的。
 * 2) 定义 enum 类型时候，如果是简单类型，那么最后一个枚举值后不用跟任何一个符号；但如果有定制方法，那么最后一个枚举值与后面代码要用分号';'隔开，不能用逗号或空格。
 * 3) 由于 enum 类型的值实际上是通过运行期构造出对象来表示的，所以在 cluster 环境下，每个虚拟机都会构造出一个同义的枚举对象。因而在做比较操作时候就需要注意，如果直接通过使用等号 ( ‘ == ’ ) 操作符，
 * 这些看似一样的枚举值一定不相等，因为这不是同一个对象实例。
 */
