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
public class HeaderExchanger implements Exchanger { //11/16 是指对请求头处理，请求体不处理？都处理的

    public static final String NAME = "header";
    //客户端去连接、服务端去绑定

    /**
     * 连接服务（客户端调用）
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
     * 绑定服务并创建服务对象（服务端调用）
     */
    public ExchangeServer bind(URL url, ExchangeHandler handler) throws RemotingException {
        //HeaderExchangeServer 会进行心跳检测
        return new HeaderExchangeServer(Transporters.bind(url, new DecodeHandler(new HeaderExchangeHandler(handler)))); //构建DecodeHandler、HeaderExchangeHandler处理器
    }

    /**
     * Dubbo分析之Exchange层  https://segmentfault.com/a/1190000016802475
     * ----
     * Exchange层，属于信息交换层，是对Request和Response的抽象。
     * 为什么要单独抽象出一个Exchange层，而不是在Protocol层直接对Netty或者Mina引用？这个问题其实不难理解，
     * Netty或者Mina对外接口和调用方式都不一样，如果在Protocol层直接对Mina做引用，对于Protocol层来讲，就依赖了具体而不是抽象，
     * 过几天想要换成Netty，就需要对Protocol层做大量的修改。这样不符合开闭原则。
     * Dubbo要使用TCP长连接，就得自己实现Request和Response的抽象概念，这样客户端与服务端之间的交互才能有去有回。
     *
     * received方法用于接受信息，这个方法是Provider和Consumer共用的。Provider接收的信息必然是Request，它所处的角色就类似与服务器。
     * Consumer接收的信息必然是Response，它所处的角色就类似于客户端。
     *
     * dubbo的exchange层  https://juejin.im/post/6844904146903154701
     * Dubbo 剖析：网络通信总结  https://xiaozhuanlan.com/topic/6238415790
     */

}