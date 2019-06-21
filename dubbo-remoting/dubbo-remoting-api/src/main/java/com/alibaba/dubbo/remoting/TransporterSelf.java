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
    //@Adaptive(value = {"nettySelf", "minaSelf", "grizzlySelf"})
    Server testUrl(Node node);
}

/**
 * 扩展表达式（含有protocol属性）
 * Server bind(URL url, ChannelHandler handler)
 *
 * 第一步：获取第三个值（是protocal属性）
 * url.getProtocol() == null ? "nettySelf" : url.getProtocal()
 *
 * 第二步：获取第二个值（普通属性,第三个参数的值最为第二个参数的默认值）
 * url.getParameter("nettySelf", url.getProtocal() == null ? "nettySelf" : url.getProtocal())
 *
 * 第三步：获取第一个值（是protocal属性）
 * url.getProtocol() == null ? (url.getParameter("nettySelf", (url.getProtocal() == null ? "nettySelf" : url.getProtocal() ))) : url.getProtocal());
 *
 * 最终表达式
 * url.getProtocol() == null ? (url.getParameter("nettySelf", (url.getProtocol() == null ? "nettySelf" : url.getProtocol() ))) : url.getProtocol();
 */

/**
 * 获取扩展名表达式 2 （包含invocation参数，从右到左知识key改变，默认值不变）
 * @Adaptive(value = {"grizzlySelf", "minaSelf"})  //带有invocation参数
 * Client connect(URL url, ChannelHandler handler, Invocation invocation)
 *
 * 包含invocation
 * 从右到左：
 * 第一步：第二个参数取值
 * url.getMethodParameter(methodName, "minaSelf", "nettySelf");
 *
 * 第二部：第一个参数取值
 * url.getMethodParameter(methodName, "grizzlySelf", "nettySelf");
 *
 * 最终表达式
 * url.getMethodParameter(methodName, "grizzlySelf", "nettySelf");
 */

/**
 *
 * 获取扩展名表达式 3 （普通类型，从右到左依次取值作为前一个的默认值）
 * @Adaptive(value = {"nettySelf", "minaSelf", "grizzlySelf"})
 * Server testUrl(Node node);
 *
 * 从右到左，依次作为前一个默认值（"protocal"类型以及非"invocation"类型）
 *
 * 第一步：第三个参数取值
 * url.getParameter("grizzlySelf","nettySelf")
 *
 * 第二步：将第三个参数作为第二个参数默认值
 * url.getParameter("minaSelf",url.getParameter("grizzlySelf","nettySelf"))
 *
 * 第三部：将第二个参数作为第一个参数默认值
 * url.getParameter("nettySelf",url.getParameter("minaSelf",url.getParameter("grizzlySelf","nettySelf"))) ;
 *
 * 最终表达式
 * url.getParameter("nettySelf",url.getParameter("minaSelf",url.getParameter("grizzlySelf","nettySelf"))) ;
 *
 * */
