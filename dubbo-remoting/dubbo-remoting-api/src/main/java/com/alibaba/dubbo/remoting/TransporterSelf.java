package com.alibaba.dubbo.remoting;

import com.alibaba.dubbo.common.Node;
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
    // 含有protocal参数  获取扩展名的表达式（从右到左，依次将右边获取的值，作为左边的默认值）
    // url.getProtocol() == null ? (url.getParameter("nettySelf", ( url.getProtocol() == null ? "nettySelf" : url.getProtocol() ))) : url.getProtocol()
    @Adaptive(value = {"protocol", "nettySelf", "protocol"}) //带有protocol参数
    Server bind(URL url, ChannelHandler handler) throws RemotingException;

    //@Adaptive({Constants.CLIENT_KEY, Constants.TRANSPORTER_KEY})
    // 带有invocation参数，从右到左，一次替换key，默认值不改变
    // url.getMethodParameter(methodName, "grizzlySelf", "nettySelf")
    @Adaptive(value = {"grizzlySelf", "minaSelf"})  //带有invocation参数
    Client connect(URL url, ChannelHandler handler, Invocation invocation) throws RemotingException;

    // 既不含有protocol参数，也没有带有invocation参数
    // url.getParameter("nettySelf", url.getParameter("minaSelf", url.getParameter("grizzlySelf", "nettySelf")))
    @Adaptive(value = {"nettySelf", "minaSelf", "grizzlySelf"})
    Server testUrl(Node node);
}
