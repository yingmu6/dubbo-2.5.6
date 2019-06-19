package com.java.relative.thread.single;

/**
 * https://www.cnblogs.com/xz816111/p/8470048.html
 * 双重判定 创建单例
 * @author chensy
 * @date 2019-06-16 12:52
 */
public class Singleton {
    private static Singleton instance;
    private volatile static Singleton instance2; // 使用了volatile关键字后，重排序被禁止，所有的写（write）操作都将发生在读（read）操作之前。

    private Singleton() { //对外不暴露公有函数

    }

    private Singleton getInstance() {
        if (instance == null) { //多线程的情况下，如线程1，2，3 ，并发访问时都能通过这里，就会实例多次
            instance = new Singleton();
        }
        return instance;
    }

    private synchronized Singleton getInstance2() { //在外层加锁，但这里每个线程访问都加锁，会导致很大的性能开销，
        // 并且加锁其实只需要在第一次初始化的时候用到，
        // 之后的调用都没必要再进行加锁。
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }

    private Singleton getInstance3() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}

/**
 * 上述写法看似解决了问题，但是有个很大的隐患。实例化对象的那行代码（标记为error的那行），实际上可以分解成以下三个步骤：
 *
 * 分配内存空间
 * 初始化对象
 * 将对象指向刚分配的内存空间
 *
 * 但是有些编译器为了性能的原因，可能会将第二步和第三步进行重排序，顺序就成了：
 *
 * 分配内存空间
 * 将对象指向刚分配的内存空间
 * 初始化对象
 *
 * 编译器可能会对初始化对象的步骤进行重排序，所以可能访问到一个初始化未完成的对象。
 */
