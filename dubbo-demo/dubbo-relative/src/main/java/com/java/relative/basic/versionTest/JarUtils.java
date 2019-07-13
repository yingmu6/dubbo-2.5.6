package com.java.relative.basic.versionTest;

/**
 * 使用JarFile、Manifest读取jar的MANIFEST.MF文件
 *
 * @author chensy
 * @date 2019-07-10 01:06
 */

import java.util.jar.*;

public class JarUtils {
    public static String getJarImplementationVersion(String jar)
            throws java.io.IOException {
        JarFile jarfile = new JarFile(jar);
        Manifest manifest = jarfile.getManifest();
        Attributes att = manifest.getMainAttributes();
        return att.getValue("Implementation-Version");
    }

    public static String getJarSpecificationVersion(String jar) throws java.io.IOException {
        JarFile jarfile = new JarFile(jar);
        Manifest manifest = jarfile.getManifest();
        Attributes att = manifest.getMainAttributes();
        return att.getValue("Specification-Version");
    }

    //打开一个jar包并修改其版本信息：
    public static void main(String[] args) throws java.io.IOException {
        String javaMailJar = "/Users/chenshengyong/selfPro/tuya_basic_dd/dubbo-demo/dubbo-relative/src/main/java/com/java/relative/basic/versionTest/hello.jar";

        System.out.println("Specification-Version: "
                + JarUtils.getJarSpecificationVersion(javaMailJar));
        System.out.println("Implementation-Version: "
                + JarUtils.getJarImplementationVersion(javaMailJar));
        /*
         * output :
         *      Specification-Version: 1.3
         *      Implementation-Version: 1.3.1
         */
    }
}
