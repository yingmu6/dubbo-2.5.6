package com.java.relative.basic.map_test;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

/**
 *
 * 在Java集合中，我们通常都通过 “Iterator(迭代器)” 或 “Enumeration(枚举类)” 去遍历集合
 * https://www.cnblogs.com/skywang12345/p/3311275.html
 *
 * @author chensy
 * @date 2019-06-13 20:08
 */
public class EnumerationTest {
    public static void main(String[] args) {
        Hashtable<String, String> table = new Hashtable<>();
        table.put("name", "zhang");
        table.put("address", "hangzhou");

        EnumerationTest test = new EnumerationTest();
       // test.useEnumeration(table);

        System.out.println("------");
        test.useIterator(table);
    }

    // 使用枚举类进行遍历
    public void useEnumeration (Hashtable<String, String> table) {
        Enumeration enumeration = table.elements();
        while (enumeration.hasMoreElements()) { // 是否有更多元素
            System.out.println(enumeration.nextElement()); // 输出元素
        }
    }

    // 使用迭代器进行迭代
    public void useIterator (Hashtable<String, String> table) {
        Iterator it = table.keySet().iterator(); //遍历键的集合
        // it.remove(); 为啥此处报错 java.lang.IllegalStateException: Hashtable Enumerator
        while (it.hasNext()) {
            System.out.println(it.next());
        }
    }
}

/**
 * (01) 函数接口不同
 *         Enumeration只有2个函数接口。通过Enumeration，我们只能读取集合的数据，而不能对数据进行修改。
 *         Iterator只有3个函数接口。Iterator除了能读取集合的数据之外，也能数据进行删除操作。
 *
 * (02) Iterator支持fail-fast机制，而Enumeration不支持。
 *         Enumeration 是JDK 1.0添加的接口。使用到它的函数包括Vector、Hashtable等类，这些类都是JDK 1.0中加入的，Enumeration存在的目的就是为它们提供遍历接口。
 *         Enumeration本身并没有支持同步，而在Vector、Hashtable实现Enumeration时，添加了同步。
 *
 *
 *  Java 集合系列04之 fail-fast总结(通过ArrayList来说明fail-fast的原理、解决办法)
 *  https://www.cnblogs.com/skywang12345/p/3308762.html
 *
 *  fail-fast 机制是java集合(Collection)中的一种错误机制。当多个线程对同一个集合的内容进行操作时，就可能会产生fail-fast事件。
 *  例如：当某一个线程A通过iterator去遍历某集合的过程中，若该集合的内容被其他线程所改变了；那么线程A访问集合时，
 *  就会抛出ConcurrentModificationException异常，产生fail-fast事件。
 *
 *  fail-fast机制，是一种错误检测机制。它只能被用来检测错误，因为JDK并不保证fail-fast机制一定会发生。若在多线程环境下使用fail-fast机制的集合，
 *  建议使用“java.util.concurrent包下的类”去取代“java.util包下的类”
 *
 *  private static List<String> list = new ArrayList<String>();
 *  替换为
 *  private static List<String> list = new CopyOnWriteArrayList<String>();
 */
