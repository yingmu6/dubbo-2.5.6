package com.java.relative.basic.versionTest;

/**
 * 给Jar包添加版本信息
 * https://blog.csdn.net/u013063153/article/details/53580424
 *
 * JAR包中的MANIFEST.MF文件详解以及编写规范
 * https://www.cnblogs.com/EasonJim/p/6485677.html
 *
 *
 * @author chensy
 * @date 2019-07-10 00:16
 */
public class Hello {
    public static void main(String[] args) {
        new Hello().say("Hello World!");
    }
    Hello() {
        Package p = this.getClass().getPackage();
        System.out.println("Hello Specification Version : " + p.getSpecificationVersion());
        System.out.println("Hello Implementation Version : " + p.getImplementationVersion());
      }
      public void say(String s) {
        System.out.println(s);
     }
}

/**
 * 此处在Terminal中 编译执行，出现无法加载主类
 * 错误: 找不到或无法加载主类 com.java.relative.basic.versionTest.Hello
 * 应该是编译路径没指正确(也可能是编译后的字节码文件与META-INF的位置放错,不必深究)
 *
 * 在桌面创建个文件目录
 * 用同样方式可以
 *
 * 1）jar cvfm hello.jar MANIFEST.MF Hello.class
 * 2）java -jar hello.jar
 */

