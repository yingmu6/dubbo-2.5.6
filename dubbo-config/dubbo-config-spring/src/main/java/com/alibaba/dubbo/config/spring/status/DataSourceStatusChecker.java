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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Map;

/**
 * DataSourceStatusChecker（数据源状态检查）
 *
 * @author william.liangf
 */
@Activate
public class DataSourceStatusChecker implements StatusChecker {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceStatusChecker.class);


    /**
     * 数据源检查
     * 1）获取spring的上下文实例，若为空，则返回UNKNOWN的Status实例
     * 2）获取DataSource类型的所有实例，并存入Map中。若Map为空，则返回UNKNOWN的Status实例
     * 3）遍历数据源Map集合
     *   3.1）获取数据源DataSource
     *   3.2）若存在拼接的内容，则加上", "分隔
     *   3.3）尝试与数据源表示的对象建立连接，获取元数据对象，并返回结果集ResultSet
     *        若结果集不存在，level则置为ERROR
     *   3.4）拼接内容，包含元数据的url、ProductName、ProductVersion等
     * 4）返回拼接的内容
     */
    @SuppressWarnings("unchecked")
    public Status check() {
        ApplicationContext context = ServiceBean.getSpringContext();
        if (context == null) {
            return new Status(Status.Level.UNKNOWN);
        }
        Map<String, DataSource> dataSources = context.getBeansOfType(DataSource.class, false, false);
        if (dataSources == null || dataSources.size() == 0) {
            return new Status(Status.Level.UNKNOWN);
        }
        Status.Level level = Status.Level.OK;
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
            DataSource dataSource = entry.getValue();
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(entry.getKey());
            try {
                Connection connection = dataSource.getConnection(); // @csy-finish 支持数据源到数据库查询？是的
                try {
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getTypeInfo();
                    try {
                        if (!resultSet.next()) {
                            level = Status.Level.ERROR;
                        }
                    } finally {
                        resultSet.close();
                    }
                    buf.append(metaData.getURL());
                    buf.append("(");
                    buf.append(metaData.getDatabaseProductName());
                    buf.append("-");
                    buf.append(metaData.getDatabaseProductVersion());
                    buf.append(")");
                } finally {
                    connection.close();
                }
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
                return new Status(level, e.getMessage());
            }
        }
        return new Status(level, buf.toString());
    }

    /**
     * 问题集 history-new
     * 1）ApplicationContext方法getBeansOfType的了解
     *   解：<T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
     *   includeNonSingletons（是否包含非单例）、allowEagerInit（是否允许急于初始化）
     *  获取指定类Class（包括子类subclasses）对应的所有的bean
     *
     * 2）此处的功能是连接数据源？dubbo也能连接数据库操作？
     * https://my.oschina.net/hokkaido/blog/85366 数据库与数据源的区别
     * 数据源定义的是连接到实际数据库的一条路径而已，数据源中并无真正的数据，它仅仅记录的是你连接到哪个数据库，以及如何连接的，如odbc数据源
     *
     * 3）了解DatabaseMetaData、ResultSet
     * 4）测试用例覆盖，待调试，什么场景下会用到DataSource
     */

}