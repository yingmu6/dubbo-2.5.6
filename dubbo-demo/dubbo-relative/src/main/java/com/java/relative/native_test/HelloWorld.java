package com.java.relative.native_test;

/**
 * Java native方法以及JNI实践
 * https://www.jianshu.com/p/1ba925157f7d
 *
 * 1. native 是用做java 和其他语言（如c++）进行协作时使用的，也就是native 后的函数的实现不是用java写的。
 * 2. 既然都不是java，那就别管它的源代码了，我们只需要知道这个方法已经被实现即可。
 * 3. native的意思就是通知操作系统， 这个函数你必须给我实现，因为我要使用。所以native关键字的函数都是操作系统实现的， java只能调用。
 * 4. java是跨平台的语言，既然是跨了平台，所付出的代价就是牺牲一些对底层的控制，而java要实现对底层的控制，
 * 就需要一些其他语言的帮助，这个就是native的作用了
 *
 * native方法是通过java中的JNI实现的。JNI是Java Native Interface的
 * 目前java与dll交互的技术主要有3种：jni，jawin和jacob。
 * 目前功能性而言：jni >> jawin > jacob
 *
 * @author chensy
 * @date 2019-05-30 16:17
 */
public class HelloWorld {
    public native void sayHelloWorld(); //申明一个native方法

    /**
     *  没有动态链接库
     *  java.lang.UnsatisfiedLinkError: no HelloWorldImpl in java.library.path
     *
     *  JNI在C语言中定义的规则是:Java_包名_类名_方法名
     */
    static {
        System.loadLibrary("HelloWorldImpl"); //装入动态链接库,"HelloWorldImpl"是装入动态链接库的名称
    }

    public static void main(String[] args) {
        HelloWorld helloWorld = new HelloWorld();
        helloWorld.sayHelloWorld();
    }
}

/**
 * https://blog.csdn.net/hap_gx/article/details/9626841(在mac上没有效果)
 *
 # ifndef _Included_packagename_classname
 * JNIEXPORT jint JNICALL Java_packagename_classname_methodName(){...}
 *
 * 实例在目录下 /Users/chenshengyong/selfPro/native_test2
 * 1. javac HelloWorld.java
 * 2. javah -jni HelloWorld
 * 3. gcc -I/Library/Java/JavaVirtualMachines/jdk1.8.0_172.jdk/Contents/Home/include/ -I/Library/Java/JavaVirtualMachines/jdk1.8.0_172.jdk/Contents/Home/include/darwin/ -dynamiclib HelloWorldImpl.cpp -o libhell.jnilib
 * 4. java HelloWorld
 *
 * 但是报错 java.lang.UnsatisfiedLinkError: no HelloWorldImpl in java.library.path
 */

/**
 * JNI调用方法在mac可用
 * https://www.jianshu.com/p/ba1208dd5ba9
 *
 * 1. javac HelloWorld.java
 * 2. （生成.h 头文件）javah -jni HelloWorld
 * 3. (编译.c  c文件 ) gcc -I/System/Library/Frameworks/JavaVM.framework/Headers -c MacJni.c
 * 4.（打包动态库） gcc -dynamiclib -o libmacjni.jnilib MacJni.o
 * 5.（执行java代码） java MacJni
 */

/**
 * 执行流程：
 * 1）java编程native方法
 * 2）使用javah 为native方法生成头文件
 * 3）引入头问题，使用c或c++，实现native方法
 * 4）编译c或c++，生成动态库jnilib,再打包为动态库
 *
 * java引入的包名为 lib+包名.jnilib
 */



