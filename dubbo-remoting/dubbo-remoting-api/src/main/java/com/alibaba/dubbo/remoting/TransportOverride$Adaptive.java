package com.alibaba.dubbo.remoting;

/**
 * 重写产生的自适应类
 * @author chensy
 * @date 2019-06-21 15:22
 */
import com.alibaba.dubbo.common.extension.ExtensionLoader;
public class TransportOverride$Adaptive  implements com.alibaba.dubbo.remoting.TransporterSelf {
    public com.alibaba.dubbo.remoting.Client connect(com.alibaba.dubbo.common.URL arg0,
                                                     com.alibaba.dubbo.remoting.ChannelHandler arg1,
                                                     com.alibaba.dubbo.rpc.Invocation arg2 )
            throws com.alibaba.dubbo.remoting.RemotingException {
        if(arg0 == null)
            throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg0;
        if(arg2 == null)
            throw new IllegalArgumentException(" invocation = null");
        String methodName = arg2.getMethodName();
        String extName = url.getMethodParameter(methodName, "grizzlySelf", "nettySelf");
        if (extName == null || extName.equals(""))
            throw new IllegalStateException ("com.alibaba.dubbo.remoting.TransporterSelf 的扩展名为空 " );
        com.alibaba.dubbo.remoting.TransporterSelf extension = ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.remoting.TransporterSelf.class ).getExtension(extName);
        return extension.connect(arg0,arg1,arg2);
    }

    public com.alibaba.dubbo.remoting.Server bind(com.alibaba.dubbo.common.URL arg0,com.alibaba.dubbo.remoting.ChannelHandler arg1 ) throws com.alibaba.dubbo.remoting.RemotingException {

        if(arg0 == null)
            throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg0;
        String extName = url.getProtocol() == null ? (url.getParameter("nettySelf", url.getProtocol() == null ? ("nettySelf") : url.getProtocol())) : url.getProtocol();
        if (extName == null || extName.equals(""))
            throw new IllegalStateException ("com.alibaba.dubbo.remoting.TransporterSelf 的扩展名为空 " );
        com.alibaba.dubbo.remoting.TransporterSelf extension =
                ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.remoting.TransporterSelf.class ).getExtension(extName);
        return extension.bind(arg0,arg1);

    }
    public com.alibaba.dubbo.remoting.Server testUrl(com.alibaba.dubbo.common.Node arg0 ) {
        throw new UnsupportedOperationException("com.alibaba.dubbo.remoting.TransporterSelf中方法testUrl没有带有@Adaptive");
    }

}