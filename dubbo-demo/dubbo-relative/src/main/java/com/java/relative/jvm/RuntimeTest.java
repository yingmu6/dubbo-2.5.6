package com.java.relative.jvm;

/**
 * https://blog.51cto.com/lavasoft/15565
 *
 * Runtime类封装了运行时的环境。每个 Java 应用程序都有一个 Runtime 类实例，使应用程序能够与其运行的环境相连接。
 * 一般不能实例化一个Runtime对象，应用程序也不能创建自己的 Runtime 类实例，但可以通过 getRuntime 方法获取当前Runtime运行时对象的引用。
 * 一旦得到了一个当前的Runtime对象的引用，就可以调用Runtime对象的方法去控制Java虚拟机的状态和行为。
 * 当Applet和其他不被信任的代码调用任何Runtime方法时，常常会引起SecurityException异常。
 *
 * @author chensy
 * @date 2019-05-30 12:31
 */
public class RuntimeTest {
    public static void main(String[] args) throws Exception {
        RuntimeTest test = new RuntimeTest();
        test.getMemory();

        test.exeuteProcees();
        System.in.read();
    }

    // 1、内存管理：
    public void getMemory () {
        Runtime r = Runtime.getRuntime();
        long mem1,mem2;
        Integer someints[] = new Integer[1000];
        // 总内存
        System.out.println("Total memory is ：" + r.totalMemory());
        // 空闲内存
        mem1 = r.freeMemory();
        System.out.println("Initial free is : " + mem1);
        // 提醒JVM垃圾回收
        r.gc();
        mem1 = r.freeMemory();
        System.out.println("Free memory after garbage collection : " + mem1);
        //allocate integers
        for(int i=0; i<1000; i++) someints[i] = new Integer(i);
        mem2 = r.freeMemory();


        System.out.println("Free memory after allocation : " + mem2);
        System.out.println("Memory used by allocation : " +(mem1-mem2));
        //discard Intergers
        for(int i=0; i<1000; i++) someints[i] = null;
        r.gc(); //request garbage collection
        mem2 = r.freeMemory();
        System.out.println("Free memory after collecting " + "discarded integers : " + mem2);
    }

    // 2.执行其它程序

    /**
     * 在安全的环境中，可以在多任务操作系统中使用Java去执行其他特别大的进程（也就是程序）。ecec()方法有几种形式命名想要运行的程序和它的输入参数
     * 。ecec()方法返回一个Process对象，可以使用这个对象控制Java程序与新运行的进程进行交互。ecec()方法本质是依赖于环境。
     *
     * ecec()方法返回Process对象后，在新程序开始运行后就可以使用Process的方法了。可以用destory()方法杀死子进程，
     * 也可以使用waitFor()方法等待程序直到子程序结束，
     * exitValue()方法返回子进程结束时返回的值。如果没有错误，将返回0，否则返回非0
     */
    public void exeuteProcees() {
        Runtime r = Runtime.getRuntime();
        Process p = null;
        try {
            p = r.exec("zkServer start");
            //p = r.exec("zkServer stop");
            p.waitFor();
        } catch (Exception e) {
            System.out.println("Error executing zkServer.");
        }
        System.out.println("zkServer returned " + p.exitValue());
    }

}
