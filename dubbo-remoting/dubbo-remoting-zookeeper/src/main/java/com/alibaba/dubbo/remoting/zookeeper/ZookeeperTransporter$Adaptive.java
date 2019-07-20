package com.alibaba.dubbo.remoting.zookeeper;
import com.alibaba.dubbo.common.extension.ExtensionLoader;

/**
 * 自适应扩展类（动态代理类）
 */
public class ZookeeperTransporter$Adaptive implements com.alibaba.dubbo.remoting.zookeeper.ZookeeperTransporter {
    public com.alibaba.dubbo.remoting.zookeeper.ZookeeperClient connect(com.alibaba.dubbo.common.URL arg0 )  {
        if(arg0 == null)
            throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg0;
        String extName = null;
        extName = url.getParameter("client", url.getParameter("transporter", "zkclient"));
        if (extName == null || extName.equals(""))
            throw new IllegalStateException ("com.alibaba.dubbo.remoting.zookeeper.ZookeeperTransporter 的扩展名为空 " );
        com.alibaba.dubbo.remoting.zookeeper.ZookeeperTransporter extension =
                ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.remoting.zookeeper.ZookeeperTransporter.class ).getExtension(extName);
        return extension.connect(arg0);
    }
}
