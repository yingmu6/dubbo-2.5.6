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
package com.alibaba.dubbo.registry;

import com.alibaba.dubbo.common.Node;
import com.alibaba.dubbo.common.URL;

/**
 * Registry（注册）. (SPI, Prototype, ThreadSafe)
 * 继承Node（节点功能）、RegistryService（注册相关服务），既是一个节点，又能提供注册相关服务
 *
 * @author william.liangf
 * @see com.alibaba.dubbo.registry.RegistryFactory#getRegistry(URL)
 * @see com.alibaba.dubbo.registry.support.AbstractRegistry
 */
public interface Registry extends Node, RegistryService {/**@c 是集群中的一个节点，并且继承注册服务 */
}