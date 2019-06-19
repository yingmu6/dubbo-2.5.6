package com.alibaba.dubbo.remoting;
import com.alibaba.dubbo.common.extension.ExtensionLoader;

/**
 * 动态生成的代理类（接口形式）
 * 两个方法：
 * 1）一个带有注解@Adaptive，一个没有带；
 * 2）两个方法中都带有URL
 * 3）一个方法的参数带有com.alibaba.dubbo.rpc.Invocation
 *
 * Server bind(URL url, ChannelHandler handler) throws RemotingException;
 *
 * @Adaptive
 * Client connect(URL url, ChannelHandler handler, Invocation invocation) throws RemotingException;
 */
public class TransporterSelf$Adaptive implements com.alibaba.dubbo.remoting.TransporterSelf {

    public com.alibaba.dubbo.remoting.Client connect(com.alibaba.dubbo.common.URL arg0, com.alibaba.dubbo.remoting.ChannelHandler arg1,
                                                     com.alibaba.dubbo.rpc.Invocation arg2) throws com.alibaba.dubbo.remoting.RemotingException {
        if (arg0 == null)
            throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg0;
        if (arg2 == null)
            throw new IllegalArgumentException("invocation == null");
        //TODO invocation中的方法名是怎样的？
        String methodName = arg2.getMethodName();
        //从url中动态获取扩展名，若没有扩展名，就使用默认的
        String extName = url.getMethodParameter(methodName, "transporter.self", "nettySelf");
        if(extName == null)
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.remoting.TransporterSelf) name from url(" + url.toString() + ") " +
                    "use keys([transporter.self])");
        com.alibaba.dubbo.remoting.TransporterSelf extension =
                (com.alibaba.dubbo.remoting.TransporterSelf)ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.remoting.TransporterSelf.class).getExtension(extName);
        return extension.connect(arg0, arg1, arg2);
    }

    public com.alibaba.dubbo.remoting.Server bind(com.alibaba.dubbo.common.URL arg0, com.alibaba.dubbo.remoting.ChannelHandler arg1)
            throws com.alibaba.dubbo.remoting.RemotingException {
        throw new UnsupportedOperationException("method public abstract com.alibaba.dubbo.remoting.Server" +
                " com.alibaba.dubbo.remoting.TransporterSelf.bind(com.alibaba.dubbo.common.URL,com.alibaba.dubbo.remoting.ChannelHandler)" +
                " throws com.alibaba.dubbo.remoting.RemotingException of " +
                "interface com.alibaba.dubbo.remoting.TransporterSelf is not adaptive method!");
    }
}
