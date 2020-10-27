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
package com.alibaba.dubbo.config.spring.status;

import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.status.Status;
import com.alibaba.dubbo.common.status.StatusChecker;
import com.alibaba.dubbo.config.spring.ServiceBean;

import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;

import java.lang.reflect.Method;

/**
 * SpringStatusChecker
 *
 * @author william.liangf
 */
@Activate
public class SpringStatusChecker implements StatusChecker {

    private static final Logger logger = LoggerFactory.getLogger(SpringStatusChecker.class);

    /**
     * Spring状态检查
     * 1）获取spring的上下文实例，若为空，则返回UNKNOWN的Status实例
     * 2）判断上下文是不是Lifecycle实例
     *   2.1）若是：判断是否正在运行，若正在运行中，则状态是OK的，若不在
     *        运行中，则状态为ERROR
     *   2.2）若不是：则状态为UNKONWN
     * 3）获取到ApplicationContext的class，循环获取getConfigLocations()方法
     *    若没有此方法，则getSuperclass()获取类Class，再对应获取getConfigLocations()
     * 4）若方法method不为空
     *   4.1）若方法是不可见的，则将accessible设为true，为可见的
     *   4.2）调用ApplicationContext中的invoke方法。获取到配置列表
     *   4.3）若配置不为空，则加到拼接的内容里面
     * 5）返回构建的状态Status
     *
     */
    public Status check() {
        ApplicationContext context = ServiceBean.getSpringContext();
        if (context == null) {
            return new Status(Status.Level.UNKNOWN);
        }
        Status.Level level = Status.Level.OK;
        if (context instanceof Lifecycle) {
            if (((Lifecycle) context).isRunning()) {
                level = Status.Level.OK;
            } else {
                level = Status.Level.ERROR;
            }
        } else {
            level = Status.Level.UNKNOWN;
        }
        StringBuilder buf = new StringBuilder();
        try {
            Class<?> cls = context.getClass();
            Method method = null;
            while (cls != null && method == null) {
                try {
                    method = cls.getDeclaredMethod("getConfigLocations", new Class<?>[0]);
                } catch (NoSuchMethodException t) {
                    cls = cls.getSuperclass();
                }
            }
            if (method != null) {
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                String[] configs = (String[]) method.invoke(context, new Object[0]);
                if (configs != null && configs.length > 0) {
                    for (String config : configs) {
                        if (buf.length() > 0) {
                            buf.append(",");
                        }
                        buf.append(config);
                    }
                }
            }
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
        return new Status(level, buf.toString());
    }

    /**
     * 问题点：history-new
     * 1）Lifecycle了解？定义了生命周期中的start/stop方案
     * 2）Class、Method、getDeclaredMethod()、getSuperclass()了解下
     * 3） while (cls != null && method == null)此处是否会出现死循环
     * 4）invoke的使用
     */

}