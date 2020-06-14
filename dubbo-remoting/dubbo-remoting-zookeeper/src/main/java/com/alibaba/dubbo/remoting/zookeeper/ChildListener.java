package com.alibaba.dubbo.remoting.zookeeper;

import java.util.List;

public interface ChildListener {

    /**
     * 当给定的path变化，通知指定的子节点 children列表
     */
    void childChanged(String path, List<String> children);

}
