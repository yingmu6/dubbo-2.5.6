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
package com.alibaba.dubbo.config;

import com.alibaba.dubbo.config.support.Parameter;

import java.io.Serializable;

/**
 * @author chao.liuc
 * @export
 */
public class ArgumentConfig implements Serializable { //参数是按位置对应的，不是按键的名称对应的

    private static final long serialVersionUID = -2165482463925213595L;

    //arugment index -1 represents not set
    private Integer index = -1;/**@c 参数配置 包含参数位置索引和对应的类型 */

    //argument type
    private String type;  //参数配置中type、index字段都会被忽略(lue)，不会暴露在url中

    //callback interface
    private Boolean callback;

    @Parameter(excluded = true)
    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    @Parameter(excluded = true)
    public String getType() { //参数类型，用字符串表示，比如"java.lang.String"
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setCallback(Boolean callback) {
        this.callback = callback;
    }

    public Boolean isCallback() {
        return callback;
    }

    /**
     * 参数配置中只有callback会暴露在url中
     * 1） 例如：如果设置了callback，没设置就不会出现在url中
     *    sayApi.0.callback=false&sayApi.2.callback=true
     *
     * 2） index、type只要要出现一个，但是如果index没有设置的话，dubbo会根据类型推断出参数的index
     *
     * 3） 设置了index、type，dubbo会检测对应位置的类型是否对应正确，若不正确抛出异常
     *
     */

}