package com.java.relative.basic.io;

import com.java.relative.basic.reflect.ClassLoaderTest;

import java.io.*;
import java.net.URL;

/**
 * 字节、字符流处理
 * https://blog.csdn.net/puppylpg/article/details/45620387
 *
 * URL 获取字节流
 * @author chensy
 * @date 2019-06-13 14:14
 */
public class StreamTest {
    int b = 0;
    public static void main(String[] args) throws Exception {
        StreamTest streamTest = new StreamTest();
        streamTest.readContent("com.java.relative.basic.reflect.Fruits");
        streamTest.readContent2("com.java.relative.basic.reflect.Fruits");
        streamTest.readContent3("com.java.relative.basic.reflect.Fruits");
    }

    /**
     * String与byte[]数组转换
     * 1）String转换为byte数组， str.getBytes()
     * 2) byte数组转换为String  new String(byte[])
     *
     * 用URI构造File，然后在构建文件输入流
     */
    public void readContent(String name) throws Exception {
        URL url = ClassLoaderTest.class.getClassLoader().getResource(name);
        if (url != null) {
            File file = new File(url.toURI());
            FileInputStream fis = new FileInputStream(file);
            byte [] bytes = new byte[fis.available()-1]; //下标是可利用的数 -1
            while((b = fis.read()) != -1) {
                fis.read(bytes);
            }
            System.out.println(new String(bytes));
        }
    }

    /**
     * 按字节流处理(逐个字节处理)
     */
    public void readContent2(String name) throws Exception {
        URL url = ClassLoaderTest.class.getClassLoader().getResource(name);
        if (url != null) {
            InputStream is = url.openStream(); //字节流处理
            BufferedInputStream fis = (BufferedInputStream) is; //强制转换
            //FileInputStream fis = (FileInputStream)is;
            byte [] bytes = new byte[fis.available()-1];
            while((b = fis.read()) != -1) {
                fis.read(bytes);
            }
            System.out.println(new String(bytes));
        }
    }

    /**
     * 按字符流处理
     */
    public void readContent3(String name) throws Exception {
        URL url = ClassLoaderTest.class.getClassLoader().getResource(name);
        if (url != null) {
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is); //将用输入流构建桥梁InputStreamReader，然后再构建字符流
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                System.out.println("按字符流处理:" + line);
            }

        }
    }

    /**
     * Java中的流分为字符流和字节流，其中字符流主要针对字符文本进行处理，而字节流处理范围更为广泛，毕竟图片、电影等只要是文件都是以二进制的形式存在的，而不是字符。
     *
     * 将字节流转换成字符流的桥梁——InputStreamReader；
     * InputStreamReader(InputStream in) ：创建一个使用默认字符集的 InputStreamReader。传入的对象是InputStream类型，而自己本身是Reader的子类。
     * 将字符流转换成字节流的桥梁——OutputStreamWriter。
     */

}
