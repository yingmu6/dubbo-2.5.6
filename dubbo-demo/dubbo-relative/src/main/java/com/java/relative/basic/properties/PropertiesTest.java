package com.java.relative.basic.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 *
 * Properties 继承于 Hashtable。表示一个持久的属性集，属性列表以key-value的形式存在，key和value都是字符串。
 * Properties 类被许多Java类使用。例如，在获取环境变量时它就作为System.getProperties()方法的返回值
 *
 * @author chensy
 * @date 2019-06-04 07:43
 */
public class PropertiesTest {
    public static void main(String[] args) throws Exception {
        PropertiesTest test = new PropertiesTest();
        Properties properties = new Properties();
        test.read(properties);

        //System.getProperty("");
        //test.write(properties);
        System.out.println(PropertiesTest.class.getResource("")); //带上了前缀 file
        System.out.println(System.getProperty("user.dir"));
        System.out.println(PropertiesTest.class.getResource("").getPath()); //取得当前类所在位置
        System.out.println(PropertiesTest.class.getResource("/").getPath());
    }

    /**
     * 将属性文件中的属性值加载到Properties属性集中
     */
    public void read(Properties properties) throws Exception {
        //File file = new File("/Users/chenshengyong/selfPro/tuya_basic_dd/dubbo-demo/dubbo-relative/src/main/java/com/java/relative/basic/properties/test.properties");
        File file = new File(PropertiesTest.class.getResource("").getPath() + "test.properties");
        //File file = new File("/Users/chenshengyong/selfPro/tuya_basic_dd/dubbo-demo/dubbo-relative/target/classes/com/java/relative/basic/properties/test.properties");
        FileInputStream ins = new FileInputStream(file);
        properties.load(ins);
        System.out.println(properties.toString());
    }

    /**
     * 把Properties属性集的内容写到文件中
     */
    public void write(Properties properties) throws Exception {
        properties.setProperty("lang", "en");
        properties.setProperty("address", "china");

        FileOutputStream fos = new FileOutputStream("/Users/chenshengyong/selfPro/tuya_basic_dd/dubbo-demo/dubbo-relative/src/main/java/com/java/relative/basic/properties/out.properties");
        properties.store(fos, "属性输出");
    }

}
