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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.Exchangers;
import com.alibaba.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;

import junit.framework.TestCase;
import org.junit.Test;

/**
 * ChanelHandlerTest
 * 测试用例：（带着问题去测试）
 * 1）预期输出什么
 * 2）实际输出什么
 * 3）结果比较以及数据分析
 * <p>
 * mvn clean test -Dtest=*PerformanceClientTest -Dserver=10.20.153.187:9911
 *
 * @author william.liangf
 */
public class ChanelHandlerTest extends TestCase {

    private static final Logger logger = LoggerFactory.getLogger(ChanelHandlerTest.class);

    public static ExchangeClient initClient(String url) {
        // 创建客户端
        ExchangeClient exchangeClient = null;
        PeformanceTestHandler handler = new PeformanceTestHandler(url);
        boolean run = true;
        while (run) {
            try {
                exchangeClient = Exchangers.connect(url, handler);
            } catch (Throwable t) {

                if (t != null && t.getCause() != null && t.getCause().getClass() != null && (t.getCause().getClass() == java.net.ConnectException.class
                        || t.getCause().getClass() == java.net.ConnectException.class)) {

                } else {
                    t.printStackTrace();
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (exchangeClient != null) {
                run = false;
            }
        }
        return exchangeClient;
    }

    public static void closeClient(ExchangeClient client) {
        if (client.isConnected()) {
            client.close();
        }
    }

    /**
     * 测试客户端连接
     * 预期结果：客户端能连上服务端
     * 问题集：1）服务端什么时候被启动的？ 2）怎么体现连接成功的？
     */
    @Test
    public void testClient() throws Throwable {
        System.setProperty("server", "127.0.0.1:9911");

        // 读取参数
        if (PerformanceUtils.getProperty("server", null) == null) {
            logger.warn("Please set -Dserver=127.0.0.1:9911");
            return;
        }
        final String server = System.getProperty("server", "127.0.0.1:9911");
        final String transporter = PerformanceUtils.getProperty(Constants.TRANSPORTER_KEY, Constants.DEFAULT_TRANSPORTER);
        final String serialization = PerformanceUtils.getProperty(Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION);
        final int timeout = PerformanceUtils.getIntProperty(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        int sleep = PerformanceUtils.getIntProperty("sleep", 60 * 1000 * 60);

        final String url = "exchange://" + server + "?transporter=" + transporter + "&serialization=" + serialization + "&timeout=" + timeout;
        ExchangeClient exchangeClient = initClient(url); // url的值 exchange://127.0.0.1:9911?transporter=netty&serialization=hessian2&timeout=1000
        Thread.sleep(sleep);
        closeClient(exchangeClient);
    }

    /**
     * 启动testClient 报查找不到扩展的问题？
     * java.lang.IllegalStateException: No such extension com.alibaba.dubbo.remoting.Transporter by name netty
     * 	at com.alibaba.dubbo.common.extension.ExtensionLoader.findException(ExtensionLoader.java:662)
     * 	at com.alibaba.dubbo.common.extension.ExtensionLoader.createExtension(ExtensionLoader.java:732)
     * 	at com.alibaba.dubbo.common.extension.ExtensionLoader.getExtension(ExtensionLoader.java:511)
     *
     *  推测：加载不了目录，读取不了文件，已debug，是加载不了文件
     *  解决方案：1）对比其他能加载的SPI扩展 2）按正常启动，看是否能正常通讯  3）是不是没有maven clean test 没有编译成功，看Class目录情况
     *
     *  此处用原生的代码跑也是同样的错，表明是环境问题或是代码本身的bug
     */

    static class PeformanceTestHandler extends ExchangeHandlerAdapter {
        String url = "";

        /**
         * @param url
         */
        public PeformanceTestHandler(String url) {
            this.url = url;
        }

        public void connected(Channel channel) throws RemotingException {
            System.out.println("connected event,chanel;" + channel);
        }

        public void disconnected(Channel channel) throws RemotingException {
            System.out.println("disconnected event,chanel;" + channel);
            initClient(url);
        }

        /* (non-Javadoc)
         * @see com.alibaba.dubbo.remoting.transport.support.ChannelHandlerAdapter#caught(com.alibaba.dubbo.remoting.Channel, java.lang.Throwable)
         */
        @Override
        public void caught(Channel channel, Throwable exception) throws RemotingException {
//            System.out.println("caught event:"+exception);
        }


    }
}