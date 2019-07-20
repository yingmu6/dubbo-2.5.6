package com.alibaba.dubbo.rpc;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
/**
 * 测试Protocol动态适配类
 * @author chensy
 * @date 2019-06-27 21:07
 */
public class Protocol$Adaptive implements com.alibaba.dubbo.rpc.Protocol {
    public void destroy() {
         throw new UnsupportedOperationException("com.alibaba.dubbo.rpc.Protocol中方法destroy没有带有@Adaptive");
    }
    public int getDefaultPort( )  {
        throw new UnsupportedOperationException("com.alibaba.dubbo.rpc.Protocol中方法getDefaultPort没有带有@Adaptive");
    }
    public com.alibaba.dubbo.rpc.Exporter export(com.alibaba.dubbo.rpc.Invoker arg0 )  throws com.alibaba.dubbo.rpc.RpcException {
        if(arg0 == null)
            throw new IllegalArgumentException("arg0 == null");

        if(arg0.getUrl() == null)
            throw new IllegalArgumentException("arg0.getUrl() == null");

        com.alibaba.dubbo.common.URL url = arg0.getUrl();
        String extName = null;
        extName = url.getProtocol() == null ? ("dubbo") : url.getProtocol();
        if (extName == null || extName.equals(""))
            throw new IllegalStateException ("com.alibaba.dubbo.rpc.Protocol 的扩展名为空 " );

        com.alibaba.dubbo.rpc.Protocol extension =
                ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.Protocol.class ).getExtension(extName);
        return extension.export(arg0);
    }

    public com.alibaba.dubbo.rpc.Invoker refer(java.lang.Class arg0,com.alibaba.dubbo.common.URL arg1 )
            throws com.alibaba.dubbo.rpc.RpcException {
        if(arg1 == null)
            throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg1;
        String extName = null;
        extName = url.getProtocol() == null ? ("dubbo") : url.getProtocol();
        if (extName == null || extName.equals(""))
            throw new IllegalStateException ("com.alibaba.dubbo.rpc.Protocol 的扩展名为空 " );
        com.alibaba.dubbo.rpc.Protocol extension =
                ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.Protocol.class ).getExtension(extName);
        return extension.refer(arg0,arg1);}}