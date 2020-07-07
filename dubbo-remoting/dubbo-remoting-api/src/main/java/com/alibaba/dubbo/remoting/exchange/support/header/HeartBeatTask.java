/*
 * Copyright 1999-2012 Alibaba Group.
 *    
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *    
 *        http://www.apache.org/licenses/LICENSE-2.0
 *    
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.alibaba.dubbo.remoting.exchange.support.header;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.exchange.Request;

import java.util.Collection;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
final class HeartBeatTask implements Runnable { //心跳检查定时任务

    private static final Logger logger = LoggerFactory.getLogger(HeartBeatTask.class);

    private ChannelProvider channelProvider;

    private int heartbeat; //心跳检测时间间隔，比如60000，一分钟检查一次

    private int heartbeatTimeout; //心跳超时时间

    HeartBeatTask(ChannelProvider provider, int heartbeat, int heartbeatTimeout) {
        this.channelProvider = provider;
        this.heartbeat = heartbeat;
        this.heartbeatTimeout = heartbeatTimeout;
    }

    /**
     * 心跳检测运行逻辑
     * 1）遍历通道列表，若通道已关闭，则不处理
     * 2）获取通道最后读lastRead、最后写的时间lastWrite
     * 3）若当前时间距离最后读或最后写的时间超过指定的心跳时间
     *    则构建请求对象Request，设置version（版本号）、twoWay（是否双向通信）、event（事件）
     *    并通过通道channel进行发送send()
     * 4）若最后读的时间不为空并且当前时间距离最后读的时间大于心跳时间
     *    若通道是Client的实例，则进行重连
     */
    public void run() {
        try {
            long now = System.currentTimeMillis();
            for (Channel channel : channelProvider.getChannels()) {
                if (channel.isClosed()) {
                    continue;
                }
                try {
                    Long lastRead = (Long) channel.getAttribute(
                            HeaderExchangeHandler.KEY_READ_TIMESTAMP);
                    Long lastWrite = (Long) channel.getAttribute(
                            HeaderExchangeHandler.KEY_WRITE_TIMESTAMP);
                    if ((lastRead != null && now - lastRead > heartbeat)
                            || (lastWrite != null && now - lastWrite > heartbeat)) { //判断是否达到应该读写的时间
                        Request req = new Request();
                        req.setVersion("2.0.0");
                        req.setTwoWay(true);
                        req.setEvent(Request.HEARTBEAT_EVENT);
                        channel.send(req);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Send heartbeat to remote channel " + channel.getRemoteAddress()
                                    + ", cause: The channel has no data-transmission exceeds a heartbeat period: " + heartbeat + "ms");
                        }
                    }
                    if (lastRead != null && now - lastRead > heartbeatTimeout) {
                        logger.warn("Close channel " + channel
                                + ", because heartbeat read idle time out: " + heartbeatTimeout + "ms");
                        if (channel instanceof Client) {
                            try {
                                ((Client) channel).reconnect();
                            } catch (Exception e) {
                                //do nothing
                            }
                        } else {
                            channel.close();
                        }
                    }
                } catch (Throwable t) {
                    logger.warn("Exception when heartbeat to remote channel " + channel.getRemoteAddress(), t);
                }
            }
        } catch (Throwable t) {
            logger.warn("Unhandled exception when heartbeat, cause: " + t.getMessage(), t);
        }
    }

    /**
     * 内部接口（通道提供者）
     * 获取通道集合列表
     */
    interface ChannelProvider {
        Collection<Channel> getChannels();
    }

}

