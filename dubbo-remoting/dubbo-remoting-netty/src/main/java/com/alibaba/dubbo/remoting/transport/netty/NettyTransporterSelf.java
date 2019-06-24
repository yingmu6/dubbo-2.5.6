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
package com.alibaba.dubbo.remoting.transport.netty;

import com.alibaba.dubbo.common.Node;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.*;
import com.alibaba.dubbo.rpc.Invocation;

/**
 *
 */
public class NettyTransporterSelf implements TransporterSelf {

    public static final String NAME = "netty";

    private TransporterSelf minaSelf;  //通过injectExtension注入依赖，属性名需要值配置文件中出现的，是SPI类型，也可以是基本类型

    private Transporter netty;

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TransporterSelf getMinaSelf() {
        return minaSelf;
    }

    public void setMinaSelf(TransporterSelf minaSelf) {
        this.minaSelf = minaSelf;
    }

    public Transporter getNetty() {
        return netty;
    }

    public void setNetty(Transporter netty) {
        this.netty = netty;
    }

    /**@c  */
    public Server bind(URL url, ChannelHandler listener) throws RemotingException {
        return new NettyServer(url, listener);
    }

    /**@c 创建Netty客户端，NettyClient*/
    public Client connect(URL url, ChannelHandler listener, Invocation invocation) throws RemotingException {
        return new NettyClient(url, listener);
    }

    @Override
    public Server testUrl(Node node) {
        return null;
    }

}