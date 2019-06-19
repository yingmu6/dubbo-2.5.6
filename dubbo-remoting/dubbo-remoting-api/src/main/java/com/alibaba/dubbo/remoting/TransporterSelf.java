package com.alibaba.dubbo.remoting;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;
import com.alibaba.dubbo.rpc.Invocation;

/**
 * 模拟实现SPI中Adaptive 动态生成代理类
 * @author chensy
 * @date 2019-06-17 08:15
 */

//@SPI("nettySelf")
@SPI("nettySelf")
public interface TransporterSelf {

    //@Adaptive({Constants.SERVER_KEY, Constants.TRANSPORTER_KEY})
    Server bind(URL url, ChannelHandler handler) throws RemotingException;

    //@Adaptive({Constants.CLIENT_KEY, Constants.TRANSPORTER_KEY})
    @Adaptive
    Client connect(URL url, ChannelHandler handler, Invocation invocation) throws RemotingException;
}
