package com.alibaba.dubbo.remoting.zookeeper.curator;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperClient;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperTransporter;

/**
 * https://www.jianshu.com/p/f430182c2b8e
 * zookeeper （原生、zkclient、curator）三种客户端实战
 */
public class CuratorZookeeperTransporter implements ZookeeperTransporter { //curator（管理者）

    public ZookeeperClient connect(URL url) {
        return new CuratorZookeeperClient(url);
    }

}
