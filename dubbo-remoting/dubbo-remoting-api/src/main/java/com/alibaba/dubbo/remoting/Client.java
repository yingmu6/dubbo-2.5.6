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
package com.alibaba.dubbo.remoting;

import com.alibaba.dubbo.common.Resetable;

/**
 * Remoting Client. (API/SPI, Prototype, ThreadSafe（线程安全）)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Client%E2%80%93server_model">Client/Server</a>
 *
 * @author qian.lei
 * @see com.alibaba.dubbo.remoting.Transporter#connect(com.alibaba.dubbo.common.URL, ChannelHandler)
 */
public interface Client extends Endpoint, Channel, Resetable {
    /**
     * reconnect.
     */
    void reconnect() throws RemotingException;

    @Deprecated
    void reset(com.alibaba.dubbo.common.Parameters parameters);

    /**
     * https://juejin.im/post/5d5a6bcbe51d4561ee1bdf6a  Client/Server结构(C/S结构)是大家熟知的客户机和服务器结构。
     * C/S 优点：
     * 1) client/server由于客户端实现与服务器的直接相连，没有中间环节，因此响应速度快。
     * 2) 同时由于开发是针对性的,充分满足客户自身的个性化要求。
     * 缺点：
     * 1）由于是针对性开发，因此缺少通用性的特点，业务变更或改变不够灵活，增加了维护和管理的难度
     * 2）需要专门的客户端安装程序，分布功能弱，不能够实现快速部署安装和配置。
     *
     * B/S(Browser/Server,浏览器/服务器)方式的网络结构。
     * 1) 客户端统一采用浏览器如：Netscape和IE，通过Web浏览器向Web服务器提出请求
     * 2) B/S结构简化了客户机的工作，但服务器将担负更多的工作
     * https://www.cnblogs.com/wanghongyun/p/6307602.html  C/S模式与B/S模式的详细介绍
     * https://www.cnblogs.com/jingmin/p/6493216.html B/S、C/S模式介绍
     */

}


