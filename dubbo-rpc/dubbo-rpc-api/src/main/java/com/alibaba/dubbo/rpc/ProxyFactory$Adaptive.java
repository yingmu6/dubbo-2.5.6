package com.alibaba.dubbo.rpc;
import com.alibaba.dubbo.common.extension.ExtensionLoader;

/**
 * 测试动态生成的扩展类
 * @author chensy
 * @date 2019-06-27 20:45
 */
public class ProxyFactory$Adaptive implements com.alibaba.dubbo.rpc.ProxyFactory {
    public java.lang.Object getProxy(com.alibaba.dubbo.rpc.Invoker arg0 )  throws com.alibaba.dubbo.rpc.RpcException {
        if(arg0 == null)
            throw new IllegalArgumentException("arg0 == null");
        if(arg0.getUrl() == null)
            throw new IllegalArgumentException("arg0.getUrl() == null");
        com.alibaba.dubbo.common.URL url = arg0.getUrl();
        String extName = null;
        extName = url.getParameter("proxy", "javassist");
        if (extName == null || extName.equals(""))
            throw new IllegalStateException ("com.alibaba.dubbo.rpc.ProxyFactory 的扩展名为空 " );
        com.alibaba.dubbo.rpc.ProxyFactory extension =
                ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.ProxyFactory.class ).getExtension(extName);
        return extension.getProxy(arg0);
    }

    public com.alibaba.dubbo.rpc.Invoker getInvoker(java.lang.Object arg0,java.lang.Class arg1,com.alibaba.dubbo.common.URL arg2 )
            throws com.alibaba.dubbo.rpc.RpcException {
        if(arg2 == null)
            throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg2;
        String extName = null;
        extName = url.getParameter("proxy", "javassist");
        if (extName == null || extName.equals(""))
            throw new IllegalStateException ("com.alibaba.dubbo.rpc.ProxyFactory 的扩展名为空 " );
        com.alibaba.dubbo.rpc.ProxyFactory extension =
                ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.ProxyFactory.class ).getExtension(extName);
        return extension.getInvoker(arg0,arg1,arg2);}
}
