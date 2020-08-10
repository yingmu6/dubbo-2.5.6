package com.alibaba.dubbo.demo.provider.self.config;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * Dubbo 配置测试
 * @author chensy
 * @date 2019-05-31 22:02
 */
public class ConfigTest {
    public static void main(String[] args) throws Exception {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("config_app_test");
        //System.setProperty("dubbo.application.name", "csy_app_config3");
        //System.setProperty("dubbo.properties.file", "/Users/chenshengyong/selfPro/tuya_basic_dd/dubbo-demo/dubbo-demo-provider/src/main/resources/dubbo.properties.file");
        //System.setProperty("dubbo.application.service.name", "leasyy");
//        applicationConfig.setTestAppConfigOut("yuu测试ooi");
//
//        System.out.println("aa输出值"+System.getProperty("aa"));
//        System.setProperty("aa", "aaeee");
//        System.out.println("aa输出值22"+System.getProperty("aa"));
//        Properties properties = new Properties();
        //properties.setProperty(Constants.DUBBO_PROPERTIES_KEY, "/Users/chenshengyong/selfPro/tuya_basic_dd/dubbo-demo/dubbo-demo-provider/src/main/resources/dubbo.config.properties");
        //ConfigUtils.setProperties(properties);

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setProtocol("zookeeper");
        registryConfig.setAddress("127.0.0.1:2181");

        ConfigTest configTest = new ConfigTest();
        Map<String, String> map = new HashMap<>();
        String prefix = "";
        map.put(Constants.DEFAULT_KEY + "." + "application", "fsdf");


        ServiceConfig serviceConfig = new ServiceConfig();
        Map<String, String> configMap = new HashMap<>();
        configMap.put("age", "11");
        configMap.put("grade", "343");
        serviceConfig.setParameters(configMap);
//        configTest.testAppendParamter(map, applicationConfig, prefix);
//        configTest.testAppendParamter(map, registryConfig, prefix);
//        configTest.testAppendParamter(map, serviceConfig, prefix);

        System.out.println(map);

        System.out.println("---------override-----");

        Map<String, String> mapSelf = new HashMap<>();
        String prefixSelf = "";
        mapSelf.put(Constants.DEFAULT_KEY + "." + "application", "fsdf");
        ServiceConfig serviceConfigSelf = new ServiceConfig();
        Map<String, String> configMapSelf = new HashMap<>();
        configMapSelf.put("age", "11");
        configMapSelf.put("grade", "343");
        serviceConfigSelf.setParameters(configMapSelf);
//        configTest.testAppendParamterSelf(mapSelf, applicationConfig, prefixSelf);
//        configTest.testAppendParamterSelf(mapSelf, registryConfig, prefixSelf);
//        configTest.testAppendParamterSelf(mapSelf, serviceConfigSelf, prefixSelf);
//        System.out.println(mapSelf);

        //System.out.println(false || false && true); //运算符 || && 从左到右

        //configTest.testSelfAppendProperties(applicationConfig);
       // configTest.testAppendProperties(applicationConfig);
        //configTest.testAppendProperties(applicationConfig);

        //configTest.testCamelToSplitName("testAppConfigOut", "*");
        //configTest.simulateCamelToSplitName("testAppConfigOut", "*_");

        //configTest.testMethodInvoke(applicationConfig);

        configTest.testGetProperty();
    }

    public void testAppendParamter(Map<String, String> map, Object config, String prefix) {
         new ApplicationConfig().testAppendParameter(map, config, prefix);
    }

//    public void testAppendParamterSelf(Map<String, String> map, Object config, String prefix) {
//        new ApplicationConfig().testAppendParameterSelf(map, config, prefix);
//    }

    /**@c 测试appendProperties方法 */
    public void testAppendProperties(ApplicationConfig applicationConfig) {
        System.out.println("添加参数前" + applicationConfig.toString());
        //dubbo.application.config_app.name
        //dubbo.application.name
        applicationConfig.testAppendProperties(applicationConfig);
        System.out.println("添加参数后" + applicationConfig.toString());
    }

    /**@c 测试appendProperties方法 */
    public void testSelfAppendProperties(ApplicationConfig applicationConfig) {
        System.out.println("self添加参数前" + applicationConfig.toString());
        applicationConfig.testAppendProperties(applicationConfig);
        System.out.println("self添加参数后" + applicationConfig.toString());
    }

    public void testCamelToSplitName(String camelName, String split) {
        System.out.println(StringUtils.camelToSplitName(camelName, split));
    }

    public void testMethodInvoke(ApplicationConfig applicationConfig) throws Exception {
        Method method = applicationConfig.getClass().getMethod("getName");
        Object obj = method.invoke(applicationConfig);
        System.out.println("值："+(String)obj);
    }

    //模拟实现将驼峰式的字符串转换位任意分隔符的字符串
    public void simulateCamelToSplitName(String camelName, String split) {
        StringBuffer buffer = null;
        for (int i = 0; i < camelName.length(); i++) {
            char ch = camelName.charAt(i);
            if (ch >= 'A' && ch <= 'Z') { //没有大写字符就不处理
                if (buffer == null) {
                    buffer = new StringBuffer();
                    buffer.append(camelName.substring(0, i));
                }
                buffer.append(split);
                buffer.append(camelName.substring(i, i + 1).toLowerCase());
                //buf.append(Character.toLowerCase(ch))
            } else if (buffer != null) {
                buffer.append(ch);
            }
        }
        if (buffer != null) {
            System.out.println(buffer.toString());
        }
    }

    //同一个属性在多个地方出现，比如1）java -D；2）Spring配置；3）properties文件配置时的加载顺序
    //优先加载java -D，然后spring配置，最后加载properties文件
    public void testGetProperty() {
        String prefix = "dubbo." + "application" + ".";
        String configId = "config_app";
        String value = null;
//        if (configId != null && configId.length() > 0) {
//            value = ConfigUtils.getProperty(prefix + configId + "." + "name");
//        }
        if (value == null || value.length() == 0) {
            value = ConfigUtils.getProperty(prefix + "name");
        }
        System.out.println("测试property：" + value);
    }
}
