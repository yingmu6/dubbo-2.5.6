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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * DefaultMessageClient
 *
 * @author william.liangf
 * @author chao.liuc
 */
public class HeaderExchangeClient implements ExchangeClient {

    private static final Logger logger = LoggerFactory.getLogger(HeaderExchangeClient.class);

    private static final ScheduledThreadPoolExecutor scheduled = new ScheduledThreadPoolExecutor(2, new NamedThreadFactory("dubbo-remoting-client-heartbeat", true));
    private final Client client;
    private final ExchangeChannel channel;
    // ???????????????
    private ScheduledFuture<?> heatbeatTimer;
    // ??????????????????????????????0????????????????????????
    private int heartbeat;
    private int heartbeatTimeout;

    public HeaderExchangeClient(Client client, boolean needHeartbeat) {
        if (client == null) {
            throw new IllegalArgumentException("client == null");
        }
        this.client = client;
        this.channel = new HeaderExchangeChannel(client);//Client???Channel???????????????????????????
        String dubbo = client.getUrl().getParameter(Constants.DUBBO_VERSION_KEY);
        this.heartbeat = client.getUrl().getParameter(Constants.HEARTBEAT_KEY, dubbo != null && dubbo.startsWith("1.0.") ? Constants.DEFAULT_HEARTBEAT : 0);
        this.heartbeatTimeout = client.getUrl().getParameter(Constants.HEARTBEAT_TIMEOUT_KEY, heartbeat * 3);
        if (heartbeatTimeout < heartbeat * 2) {
            throw new IllegalStateException("heartbeatTimeout < heartbeatInterval * 2");
        }
        if (needHeartbeat) { /**@???????????????????????? */
            startHeatbeatTimer();
        }
    }

    public ResponseFuture request(Object request) throws RemotingException {
        return channel.request(request);
    }

    public URL getUrl() {
        return channel.getUrl();
    }

    public InetSocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
    }

    public ResponseFuture request(Object request, int timeout) throws RemotingException {
        return channel.request(request, timeout);
    }

    public ChannelHandler getChannelHandler() {
        return channel.getChannelHandler();
    }

    public boolean isConnected() {
        return channel.isConnected();
    }

    public InetSocketAddress getLocalAddress() {
        return channel.getLocalAddress();
    }

    public ExchangeHandler getExchangeHandler() {
        return channel.getExchangeHandler();
    }

    public void send(Object message) throws RemotingException {
        channel.send(message);
    }

    public void send(Object message, boolean sent) throws RemotingException {
        channel.send(message, sent);
    }

    public boolean isClosed() {
        return channel.isClosed();
    }

    public void close() {
        doClose();
        channel.close();
    }

    /**
     * ???????????????????????????
     * 1?????????????????????closing??????true????????????????????????
     * 2????????????????????????
     * 3???????????????ExchangeChannel
     */
    public void close(int timeout) {
        // ??????client??????????????????
        startClose();
        doClose();
        channel.close(timeout);
    }

    @Override
    public void startClose() {
        channel.startClose();
    }

    public void reset(URL url) {
        client.reset(url);
    }

    @Deprecated
    public void reset(com.alibaba.dubbo.common.Parameters parameters) {
        reset(getUrl().addParameters(parameters.getParameters()));
    }

    public void reconnect() throws RemotingException {
        client.reconnect();
    }

    public Object getAttribute(String key) {
        return channel.getAttribute(key);
    }

    public void setAttribute(String key, Object value) {
        channel.setAttribute(key, value);
    }

    public void removeAttribute(String key) {
        channel.removeAttribute(key);
    }

    public boolean hasAttribute(String key) {
        return channel.hasAttribute(key);
    }

    /**@c ????????????????????? */
    /**
     * 1??????????????????????????????
     * 2????????????????????????0
     *    ?????????????????????????????????????????? history-new ScheduledThreadPoolExecutor??????
     */
    private void startHeatbeatTimer() {
        stopHeartbeatTimer();
        if (heartbeat > 0) {
            heatbeatTimer = scheduled.scheduleWithFixedDelay(
                    new HeartBeatTask(new HeartBeatTask.ChannelProvider() {
                        public Collection<Channel> getChannels() {
                            return Collections.<Channel>singletonList(HeaderExchangeClient.this);
                        }
                    }, heartbeat, heartbeatTimeout),
                    heartbeat, heartbeat, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * ?????????????????????
     * 1??????????????????????????????????????????????????????
     *    1.1???????????????????????????heatbeatTimer??????
     *    1.2?????????????????????????????????
     * 2?????????????????????????????????null
     */
    private void stopHeartbeatTimer() {
        if (heatbeatTimer != null && !heatbeatTimer.isCancelled()) {
            try {
                heatbeatTimer.cancel(true);
                scheduled.purge();
            } catch (Throwable e) {
                if (logger.isWarnEnabled()) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
        heatbeatTimer = null;
    }

    private void doClose() {
        stopHeartbeatTimer();
    }

    @Override
    public String toString() {
        return "HeaderExchangeClient [channel=" + channel + "]";
    }
}