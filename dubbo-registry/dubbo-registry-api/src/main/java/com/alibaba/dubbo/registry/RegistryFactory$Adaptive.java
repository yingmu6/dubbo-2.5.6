package com.alibaba.dubbo.registry;
import com.alibaba.dubbo.common.extension.ExtensionLoader;

/**
 * 注册协议动态生成类，自适应扩展类
 */
public class RegistryFactory$Adaptive implements com.alibaba.dubbo.registry.RegistryFactory {
    public com.alibaba.dubbo.registry.Registry getRegistry(com.alibaba.dubbo.common.URL arg0) {
        if(arg0 == null)
            throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg0;
        String extName = null;
        extName = (url.getProtocol() == null || url.getProtocol() == "") ? ("dubbo") : url.getProtocol();
        if (extName == null || extName.equals(""))
            throw new IllegalStateException ("com.alibaba.dubbo.registry.RegistryFactory 的扩展名为空 " );
        com.alibaba.dubbo.registry.RegistryFactory extension =
                ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.registry.RegistryFactory.class).getExtension(extName);
        return extension.getRegistry(arg0);
    }
}