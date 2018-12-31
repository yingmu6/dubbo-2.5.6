package com.alibaba.dubbo.remoting.zookeeper;

public interface StateListener {/**@c 状态监听器 */

    int DISCONNECTED = 0;

    int CONNECTED = 1;

    int RECONNECTED = 2;

    void stateChanged(int connected);

}
