package com.alibaba.dubbo.remoting;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
public class TransporterSelf$Adaptive2 implements com.alibaba.dubbo.remoting.TransporterSelf {
    public com.alibaba.dubbo.remoting.Client connect(com.alibaba.dubbo.common.URL arg0, com.alibaba.dubbo.remoting.ChannelHandler arg1, com.alibaba.dubbo.rpc.Invocation arg2) throws com.alibaba.dubbo.remoting.RemotingException {

        if (arg0 == null)
            throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg0;
        if (arg2 == null)
            throw new IllegalArgumentException("invocation == null");
        String methodName = arg2.getMethodName();
        String extName = url.getMethodParameter(methodName, "transporter.self", "nettySelf");
        if(extName == null)
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.remoting.TransporterSelf) name from url(" + url.toString() + ") use keys([transporter.self])");
        com.alibaba.dubbo.remoting.TransporterSelf extension =
                (com.alibaba.dubbo.remoting.TransporterSelf)ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.remoting.TransporterSelf.class).getExtension(extName);
        return extension.connect(arg0, arg1, arg2);
    }


    public com.alibaba.dubbo.remoting.Server bind(com.alibaba.dubbo.common.URL arg0, com.alibaba.dubbo.remoting.ChannelHandler arg1) throws com.alibaba.dubbo.remoting.RemotingException {throw new UnsupportedOperationException("method public abstract com.alibaba.dubbo.remoting.Server com.alibaba.dubbo.remoting.TransporterSelf.bind(com.alibaba.dubbo.common.URL,com.alibaba.dubbo.remoting.ChannelHandler) throws com.alibaba.dubbo.remoting.RemotingException of interface com.alibaba.dubbo.remoting.TransporterSelf is not adaptive method!");
    }


    public com.alibaba.dubbo.remoting.Server testUrl(com.alibaba.dubbo.common.Node arg0) {
        if (arg0 == null)
            throw new IllegalArgumentException("com.alibaba.dubbo.common.Node argument == null");
        if (arg0.getUrl() == null)
            throw new IllegalArgumentException("com.alibaba.dubbo.common.Node argument getUrl() == null");
        com.alibaba.dubbo.common.URL url = arg0.getUrl();
        String extName = url.getParameter("nettySelf", url.getParameter("minaSelf", "nettySelf"));
        if(extName == null)
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.remoting.TransporterSelf) name from url(" + url.toString() + ") use keys([nettySelf, minaSelf])");
        com.alibaba.dubbo.remoting.TransporterSelf extension =
                (com.alibaba.dubbo.remoting.TransporterSelf)ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.remoting.TransporterSelf.class).getExtension(extName);
        return extension.testUrl(arg0);
    }
}
