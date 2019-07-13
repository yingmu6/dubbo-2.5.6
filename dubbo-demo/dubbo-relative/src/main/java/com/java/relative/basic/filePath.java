package com.java.relative.basic;

import java.io.File;

/**
 * 文件相对路径 https://blog.csdn.net/beloveddarling/article/details/53694103
 * @author chensy
 * @date 2019-07-05 12:54
 */
public class filePath {
    public static void main(String[] args) throws Exception {
        File file = new File("xxx-service/src/main/java/com/xxx/xxx/service/storage/user.xls");
        System.out.println(file.exists());
        file.createNewFile();
        System.out.println(file.exists() + "," + file.getAbsolutePath());
    }
}

/**
 * 在project中，相对路径的根目录是project的根文件夹，
 *
 * 1. 使用工程相对路径是靠不住的。
 * 2. 使用CLASSPATH路径是可靠的。
 * 3. 对于程序要读取的文件，尽可能放到CLASSPATH下，这样就能保证在开发和发布时候均正常读取。
 */

/**
 * java源代码改了以后，需要重新编译下，class文件才会对应改动
 */