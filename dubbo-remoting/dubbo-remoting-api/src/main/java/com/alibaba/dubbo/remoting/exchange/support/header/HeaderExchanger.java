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
package com.alibaba.dubbo.remoting.exchange.support.header;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Transporters;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.remoting.exchange.Exchanger;
import com.alibaba.dubbo.remoting.transport.DecodeHandler;

/**
 * DefaultMessenger
 *
 * @author william.liangf
 */
//history-h1 header交换？
public class HeaderExchanger implements Exchanger {

    public static final String NAME = "header";
    //客户端去连接、服务端去绑定

    /**
     * 连接服务
     * 1）构建HeaderExchangeHandler（头交换处理类），设置属性它的ExchangeHandler属性
     * 2）构建DecodeHandler，将HeaderExchangeHandler设置到父类AbstractChannelHandlerDelegate
     *   （抽象通道处理委托类）的ChannelHandler属性中，请求header要特殊处理
     *   （此处解码处理类对Dubbo协议头的16字段按Dubbo协议约定的格式解码，而请求body按常规序列化、反序列化就行）
     * 3）调用Transporters.connect连接服务，并返回Client
     * 4）构建心跳客户端HeaderExchangeClient，并返回
     */
    public ExchangeClient connect(URL url, ExchangeHandler handler) throws RemotingException {
        return new HeaderExchangeClient(Transporters.connect(url, new DecodeHandler(new HeaderExchangeHandler(handler))), true);
    }

    /**
     * 绑定服务 -- 代码流程
     * 1）创建HeaderExchangeHandler处理类，new HeaderExchangeHandler(handler)
     * 2）将HeaderExchangeHandler作为构造参数，创建DecodeHandler解码处理类，new DecodeHandler(new HeaderExchangeHandler(handler))
     * 3）
     */
    public ExchangeServer bind(URL url, ExchangeHandler handler) throws RemotingException { //service export 步骤12
        //HeaderExchangeServer 会进行心跳检测
        return new HeaderExchangeServer(Transporters.bind(url, new DecodeHandler(new HeaderExchangeHandler(handler)))); //构建DecodeHandler、HeaderExchangeHandler处理器
    }

}