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
package com.alibaba.dubbo.remoting.transport;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Endpoint;
import com.alibaba.dubbo.remoting.RemotingException;

/**
 * AbstractPeer
 * wheel（轮子）、Peer（同龄人、匹配、盯着看、凝视）
 * @author qian.lei
 * @author william.liangf
 */
public abstract class AbstractPeer implements Endpoint, ChannelHandler {
    /**
     * 数据结构：
     * 类继承关系：
     * AbstractPeer实现了Endpoint、ChannelHandler接口
     *
     * 维护的数据：
     * ChannelHandler（通道处理器）、URL（处理的url）
     * closing（是否在关闭中）、closed（是否已关闭）
     */

    private final ChannelHandler handler;

    private volatile URL url;

    // closing closed分别表示关闭流程中、完成关闭
    private volatile boolean closing;

    private volatile boolean closed;

    public AbstractPeer(URL url, ChannelHandler handler) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        this.url = url; //url如：dubbo://172.16.90.239:20883/com.alibaba.dubbo.demo.DemoService?anyhost=true&application=demo-provider&channel.readonly.sent=true&codec=dubbo&dubbo=2.0.0&generic=false&heartbeat=60000&interface=com.alibaba.dubbo.demo.DemoService&methods=sayLang,sayHello&pid=50341&service.filter=selfFilter&side=provider&timestamp=1569496786254
        this.handler = handler;
    }

    public void send(Object message) throws RemotingException {
        send(message, url.getParameter(Constants.SENT_KEY, false));
    }

    public void close() {
        closed = true;
    }

    public void close(int timeout) {
        close();
    }

    public void startClose() {
        if (isClosed()) {
            return;
        }
        closing = true;
    }

    public URL getUrl() {
        return url;
    }

    protected void setUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        this.url = url;
    }

    public ChannelHandler getChannelHandler() {
        if (handler instanceof ChannelHandlerDelegate) {
            return ((ChannelHandlerDelegate) handler).getHandler();
        } else {
            return handler;
        }
    }

    /**
     * @return ChannelHandler
     */
    @Deprecated
    public ChannelHandler getHandler() {
        return getDelegateHandler();
    }

    /**
     * 返回最终的handler，可能已被wrap,需要区别于getChannelHandler
     *
     * @return ChannelHandler
     */
    public ChannelHandler getDelegateHandler() {
        return handler;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isClosing() {
        return closing && !closed;
    }

    public void connected(Channel ch) throws RemotingException {
        if (closed) {
            return;
        }
        handler.connected(ch);
    }

    public void disconnected(Channel ch) throws RemotingException {
        handler.disconnected(ch);
    }

    public void sent(Channel ch, Object msg) throws RemotingException {
        if (closed) {
            return;
        }
        handler.sent(ch, msg);
    }

    public void received(Channel ch, Object msg) throws RemotingException {
        if (closed) {
            return;
        }
        handler.received(ch, msg);
    }

    public void caught(Channel ch, Throwable ex) throws RemotingException {
        handler.caught(ch, ex);
    }
}