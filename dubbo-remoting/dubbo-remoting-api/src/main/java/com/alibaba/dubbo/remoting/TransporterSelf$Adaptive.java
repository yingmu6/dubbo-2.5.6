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
                                                     com.alibaba.dubbo.rpc.Invocation arg2 )
                                                            throws com.alibaba.dubbo.remoting.RemotingException {
        if(arg0 == null)
            throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg0;
        if(arg2 == null)
            throw new IllegalArgumentException(" invocation = null");String methodName = arg2.getMethodName();
        String extName = null;
        extName = url.getMethodParameter(methodName, "grizzlySelf", "nettySelf");
        if (extName == null || extName.equals(""))
            throw new IllegalStateException ("com.alibaba.dubbo.remoting.TransporterSelf 的扩展名为空 " );
        com.alibaba.dubbo.remoting.TransporterSelf extension =
                ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.remoting.TransporterSelf.class ).getExtension(extName);
        return extension.connect(arg0,arg1,arg2);
    }

    public com.alibaba.dubbo.remoting.Server testUrl(com.alibaba.dubbo.common.Node arg0 )  {
        throw new UnsupportedOperationException("com.alibaba.dubbo.remoting.TransporterSelf中方法testUrl没有带有@Adaptive");
    }

    public com.alibaba.dubbo.remoting.Server bind(com.alibaba.dubbo.common.URL arg0,
                                                  com.alibaba.dubbo.remoting.ChannelHandler arg1 )
            throws com.alibaba.dubbo.remoting.RemotingException {

        if(arg0 == null) throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg0;
        String extName = null;
        extName = (url.getProtocol() == null || url.getProtocol() == "")? (url.getParameter("nettySelf", (url.getProtocol() == null || url.getProtocol() == "") ? ("nettySelf") : url.getProtocol())) : url.getProtocol();
        if (extName == null || extName.equals("")) throw new IllegalStateException ("com.alibaba.dubbo.remoting.TransporterSelf 的扩展名为空 " );
        com.alibaba.dubbo.remoting.TransporterSelf extension = ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.remoting.TransporterSelf.class ).getExtension(extName);
        return extension.bind(arg0,arg1); //之前手动的变更了动态生成类的代码，返回null，出现server == null
    }
}

/**
 * 动态生成的代理类，把它放在指定的目录下就可以调试了，
 * 并且如果显示声明了类文件，就以声明的类文件为准，不会再取动态生成的类。
 * 所以之前出现一个错误，报server == null 是因为之前把同名的Transporter$Adaptive放在指定的目录下，但当时在bind方法中直接返回null，所以导致server为null
 *
 */
