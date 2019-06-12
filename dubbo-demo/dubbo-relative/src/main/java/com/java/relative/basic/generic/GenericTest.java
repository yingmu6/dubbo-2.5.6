package com.java.relative.basic.generic;

/**
 * 泛型  https://www.cnblogs.com/lwbqqyumidi/p/3837629.html
 * 泛型类型在逻辑上看以看成是多个不同的类型，实际上都是相同的基本类型
 *
 * 只是作用于代码编译阶段，在编译过程中，对于正确检验泛型结果后，会将泛型的相关信息擦出；编译过程检测、运行中擦除
 * 类型通配符一般是使用 ? 代替具体的类型实参
 * 类型通配符上限通过形如Box<? extends Number>形式定义，相对应的，类型通配符下限为Box<? super Number>形式，其含义与类型通配符上限正好相反
 *
 * TODO demo编写
 * @author chensy
 * @date 2019-06-12 08:32
 */
public class GenericTest {
}
