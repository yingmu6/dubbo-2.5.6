/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.common.extension;

/**
 * ExtensionFactory
 *
 * @author william.liangf
 * @export
 */
@SPI
public interface ExtensionFactory { /**@c 通过扩展工厂方法获取扩展实例  */

    /**
     * Get extension.
     *
     * @param type object type.
     * @param name object name.
     * @return object instance.
     */
    <T> T getExtension(Class<T> type, String name); // 获取接口指定名称的实例

    /**
     * 工厂模式专门负责将大量有共同接口的类实例化。工厂模式可以动态决定将哪一个类实例化，不必事先知道每次要实例化哪一个类。
     *
     * 工厂模式的几种形态： https://www.jianshu.com/p/bf8341c75304
     * （1）简单工厂（Simple Factory）模式，又称静态工厂方法模式（Static Factory Method Pattern）。
     * （2）工厂方法（Factory Method）模式，又称多态性工厂（Polymorphic Factory）模式或虚拟构造子（Virtual Constructor）模式；
     * （3）抽象工厂（Abstract Factory）模式，又称工具箱（Kit 或Toolkit）模式。
     */
}
