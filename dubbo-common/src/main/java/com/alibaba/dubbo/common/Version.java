/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ClassHelper;
import com.alibaba.fastjson.JSONObject;

import java.net.URL;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * Version
 *
 * @author william.liangf
 */
public final class Version {

    private static final Logger logger = LoggerFactory.getLogger(Version.class);
    private static final String VERSION = getVersion(Version.class, "2.0.0");
    private static final boolean INTERNAL = hasResource("com/alibaba/dubbo/registry/internal/RemoteRegistry.class"); //内部的
    private static final boolean COMPATIBLE = hasResource("com/taobao/remoting/impl/ConnectionRequest.class"); //兼容

    static {
        // 检查是否存在重复的jar包
        Version.checkDuplicate(Version.class);
    }

    private Version() {
    }

    public static String getVersion() {
        return VERSION;
    }

    public static boolean isInternalVersion() {
        return INTERNAL;
    }

    public static boolean isCompatibleVersion() {
        return COMPATIBLE;
    }

    private static boolean hasResource(String path) {
        try {
            return Version.class.getClassLoader().getResource(path) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public static String getVersion(Class<?> cls, String defaultVersion) {
        try {
            // 首先查找MANIFEST.MF规范中的版本号
            String version = cls.getPackage().getImplementationVersion();
            if (version == null || version.length() == 0) {
                version = cls.getPackage().getSpecificationVersion();
            }
            if (version == null || version.length() == 0) {
                // 如果规范中没有版本号，基于jar包名获取版本号
                /**
                 * ProtectionDomain(保护域)、codeSource（代码源）    https://blog.csdn.net/yfqnihao/article/details/8271415
                 * 在java.security包，安全处理
                 * https://www.cnblogs.com/f1194361820/p/4189269.html
                 */
                CodeSource codeSource = cls.getProtectionDomain().getCodeSource();
                if (codeSource == null) { //使用默认的版本号
                    logger.info("No codeSource for class " + cls.getName() + " when getVersion, use default version " + defaultVersion);
                } else { //存在jar包，从jar的包名中获取到版本号
                    String file = codeSource.getLocation().getFile();
                    if (file != null && file.length() > 0 && file.endsWith(".jar")) {
                        file = file.substring(0, file.length() - 4); //jar包的包名
                        int i = file.lastIndexOf('/');
                        if (i >= 0) {
                            file = file.substring(i + 1);
                        }
                        i = file.indexOf("-");
                        if (i >= 0) {
                            file = file.substring(i + 1);
                        }
                        while (file.length() > 0 && !Character.isDigit(file.charAt(0))) { //文件名不是以数字开头
                            i = file.indexOf("-");
                            if (i >= 0) {
                                file = file.substring(i + 1);
                            } else {
                                break;
                            }
                        }
                        version = file;
                    }
                }
            }
            // 返回版本号，如果为空返回缺省版本号
            return version == null || version.length() == 0 ? defaultVersion : version;
        } catch (Throwable e) { // 防御性容错
            // 忽略异常，返回缺省版本号
            logger.error("return default version, ignore exception " + e.getMessage(), e);
            return defaultVersion;
        }
    }

    public static void checkDuplicate(Class<?> cls, boolean failOnError) {
        checkDuplicate(cls.getName().replace('.', '/') + ".class", failOnError);
    }

    /**
     * 检查路径下是否有重复的类（不抛出异常）
     */
    public static void checkDuplicate(Class<?> cls) {
        checkDuplicate(cls, false);
    }

    /**
     * 检查路径下是否有重复的类
     * 1）获取Version的类加载器以及该类路径下的URL列表
     * 2）枚举URL列表，获取url对应的文件路径file，若file不为空，则添加到集合中
     * 3）若file集合列表不为空
     *   3.1）若需要打印异常，则抛出IllegalStateException非法状态异常
     *   3.2）若不需要打印异常，则打印error日志
     */
    public static void checkDuplicate(String path, boolean failOnError) {
        try {
            // 在ClassPath搜文件
            Enumeration<URL> urls = ClassHelper.getCallerClassLoader(Version.class).getResources(path);
            Set<String> files = new HashSet<String>();
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url != null) {
                    String file = url.getFile();
                    if (file != null && file.length() > 0) {
                        files.add(file);
                    }
                }
            }
            // 如果有多个，就表示重复
            if (files.size() > 1) {
                String error = "Duplicate class " + path + " in " + files.size() + " jar " + files;
                if (failOnError) {
                    throw new IllegalStateException(error);
                } else {
                    logger.error(error);
                }
            }
        } catch (Throwable e) { // 防御性容错
            logger.error(e.getMessage(), e);
        }

        /**
         * 问题集：todo @csy-new
         * 1）类加载器了解以及getResources的使用
         * 2）url.getFile()获取到的是文件路径吗？
         * 3）待调试
         */
    }

    //Test
    public static void main(String[] args) {
        System.out.println(getVersion(com.alibaba.dubbo.common.URL.class, "1.3.3"));
    }

}