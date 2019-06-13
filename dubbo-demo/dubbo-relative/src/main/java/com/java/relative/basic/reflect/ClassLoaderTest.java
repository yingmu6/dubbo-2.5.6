package com.java.relative.basic.reflect;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * 类加载器
 * @author chensy
 * @date 2019-06-13 00:06
 */
public class ClassLoaderTest {
    private int b = 0;
    public static void main(String[] args) throws Exception {
        ClassLoaderTest loaderTest = new ClassLoaderTest();
        //loaderTest.instance();

        // 加载的资源，要在resources目录下
//        loaderTest.getResources("com.java.relative.basic.reflect.Fruits");
//        loaderTest.getResourcesList("com.java.relative.basic.reflect.Fruits");
        //loaderTest.getResource("META-INF/dubbo/internal/com.alibaba.dubbo.common.extension.ExtensionFactory");

    }

    //实例化对象
    public void instance() {
        try {
            Class apple = Class.forName("com.java.relative.basic.reflect.Apple");
            Apple appleIns = (Apple) apple.newInstance(); //实例化对象
            appleIns.setWeight(23);
            System.out.println(appleIns.getWeight());

//            Class fruit = Class.forName("com.java.relative.basic.reflect.Fruits"); //返回与给定字符串名称的类或接口相关联的 Class对象。
//            if (fruit.isInterface()) {
//                System.out.println("interface");
//            }
//            Fruits fruits = (Fruits) fruit.newInstance(); //返回类的实例，若是接口调用会报异常
//            fruits.setWeight(23);
//            System.out.println(fruits.getWeight());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //加载指定名称的资源列表（先判断当前模块是否存在资源，若不存在，从其它模块查找资源）
    // 输出 file:/Users/chenshengyong/selfPro/tuya_basic_dd/dubbo-demo/dubbo-relative/target/classes/com.java.relative.basic.reflect.Fruits
    public void getResources(String name) throws Exception{
       URL url = ClassLoaderTest.class.getClassLoader().getResource(name);
       System.out.println(url == null ? "" : url.toString());

    }

    //加载指定名称的资源列表（在所有模块中查找指定名称的资源）

    /**
     * 输出 file:/Users/chenshengyong/selfPro/tuya_basic_dd/dubbo-demo/dubbo-relative/target/classes/com.java.relative.basic.reflect.Fruits
     *     file:/Users/chenshengyong/selfPro/tuya_basic_dd/dubbo-config/dubbo-config-spring/target/classes/com.java.relative.basic.reflect.Fruits
     */
    public void getResourcesList(String name) throws Exception {
        Enumeration<URL> urls = ClassLoaderTest.class.getClassLoader().getResources(name);
        while (urls.hasMoreElements()) {
            java.net.URL url = urls.nextElement();
            System.out.println(url == null ? "" : url.toString());
        }
    }


}

/**
 *  类加载器是负责加载类的对象。 ClassLoader类是一个抽象类
 *  用来加载 Class 的。它负责将Class的字节码形式转换成内存形式的 Class 对象。 byte[] -> (ClassLoader) -> Class
 *  字节码可以来自于磁盘文件 *.class，也可以是 jar 包里的 *.class，也可以来自远程服务器提供的字节流，字节码的本质就是一个字节数组 []byte
 *
 *  https://juejin.im/post/5c04892351882516e70dcc9b
 *
 *  延迟加载：
 *  JVM 运行并不是一次性加载所需要的全部类的，它是按需加载，也就是延迟加载。程序在运行的过程中会逐渐遇到很多不认识的新类，
 *  这时候就会调用 ClassLoader 来加载这些类。加载完成后就会将 Class 对象存在 ClassLoader 里面，下次就不需要重新加载了
 *
 *  Class传递性（双亲委派） AppClassLoader（下 classpath） -> ExtensionClassLoader(中 lib/ext/*.jar) -> BootstrapClassLoader(上 lib/rt.jar)
 *  JVM 运行实例中会存在多个 ClassLoader，不同的 ClassLoader 会从不同的地方加载字节码文件
 *  JVM 中内置了三个重要的 ClassLoader，分别是 BootstrapClassLoader、ExtensionClassLoader 和 AppClassLoader。
 *  三个 ClassLoader 之间形成了级联的父子关系，每个 ClassLoader 都很懒，尽量把工作交给父类做，父类干不了了自己才会干
 *
 *  ClassLoader 里面有三个重要的方法 loadClass()、findClass() 和 defineClass()。
 *  自定义类加载器不易破坏双亲委派规则，不要轻易覆盖 loadClass 方法。否则可能会导致自定义加载器无法加载内置的核心类库。
 *
 *  ClassLoader 的意义，它相当于类的命名空间，起到了类隔离的作用。位于同一个 ClassLoader 里面的类名是唯一的，不同的 ClassLoader 可以持有同名的类。ClassLoader 是类名称的容器，是类的沙箱。
 *  不同的 ClassLoader 之间也会有合作，它们之间的合作是通过 parent 属性和双亲委派机制来完成的
 *
 *  Thread.contextClassLoader
 *  Thread.currentThread().getContextClassLoader().loadClass(name);
 *  它可以做到跨线程共享类，只要它们共享同一个 contextClassLoader。父子线程之间会自动传递 contextClassLoader，所以共享起来将是自动化的。
 */
