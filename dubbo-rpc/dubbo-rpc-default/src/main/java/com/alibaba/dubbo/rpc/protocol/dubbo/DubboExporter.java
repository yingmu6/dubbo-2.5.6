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
package com.alibaba.dubbo.rpc.protocol.dubbo;

import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.protocol.AbstractExporter;

import java.util.Map;

/**
 * DubboExporter
 *
 * @author william.liangf
 */
public class DubboExporter<T> extends AbstractExporter<T> {// read finish
    /**
     * 数据结构 -》
     *
     * 类继承关系：
     * 1）DubboExporter继承AbstractExporter抽象类
     * 2）AbstractExporter实现Exporter接口
     *
     * 包含的数据：
     * 1）Invoker<T> invoker ：具体的执行者
     * 2）Map<String, Exporter<?>> exporterMap ：暴露服务的本地缓存
     * 3）unexported：服务销毁状态
     *
     * 包含的功能：
     * 1）构造DubboExporter信息：DubboExporter(Invoker<T> invoker, String key, Map<String, Exporter<?>> exporterMap)
     * 2）取消服务暴露：unexport()
     */

    private final String key;

    private final Map<String, Exporter<?>> exporterMap; //暴露export的map

    public DubboExporter(Invoker<T> invoker, String key, Map<String, Exporter<?>> exporterMap) {
        super(invoker);//调用父类的构造函数，把值传入父类
        this.key = key;
        this.exporterMap = exporterMap;
    }

    @Override
    public void unexport() {
        super.unexport(); //将invoker节点销毁
        exporterMap.remove(key); //移除缓存中对应的值
    }

}