package com.alibaba.dubbo.remoting.zookeeper.support;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.zookeeper.ChildListener;
import com.alibaba.dubbo.remoting.zookeeper.StateListener;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperClient;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class AbstractZookeeperClient<TargetChildListener> implements ZookeeperClient {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractZookeeperClient.class);

    private final URL url;

    private final Set<StateListener> stateListeners = new CopyOnWriteArraySet<StateListener>();

    private final ConcurrentMap<String, ConcurrentMap<ChildListener, TargetChildListener>> childListeners = new ConcurrentHashMap<String, ConcurrentMap<ChildListener, TargetChildListener>>();

    private volatile boolean closed = false;

    public AbstractZookeeperClient(URL url) {
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }

    /**
     * zookeeper创建节点逻辑
     * 1）将暴露的路径从后到前递归拆分路径，直到不能拆分为止
     * 2）从前到后，即从根节点开始创建节点，前面的节点都是持久节点，最后一个节点是临时节点
     * 3）但提供接口不再暴露时，临时节点会被删除，但是持久会被保存，除非主动删除
     *
     * @param path  创建的路径，如：/dubbo/com.alibaba.dubbo.demo.ApiDemo/providers/dubbo%3A%2F%2F10.118.32.189...
     * @param ephemeral 是否是临时节点，最后一个节点是临时节点，前面的节点都是持久节点
     */
    public void create(String path, boolean ephemeral) { //path的值如：/dubbo/com.alibaba.dubbo.demo.ApiDemo/providers/dubbo%3A%2F%2F10.118.32.189
        int i = path.lastIndexOf('/'); //查找最后一个符号出现的位置
        if (i > 0) { //从后面向前递归查拆分，当i=0时，即path为/dubbo时，跳出递归，执行后面操作
            create(path.substring(0, i), false); //递归创建目录
        }
        if (ephemeral) {
            createEphemeral(path); //最后一个结点的时候，ephemeral为true了
        } else {
            createPersistent(path); //ephemeral为false时（非临时节点），表示创建持久节点
        }
    }

    public void addStateListener(StateListener listener) {
        stateListeners.add(listener);
    }

    public void removeStateListener(StateListener listener) {
        stateListeners.remove(listener);
    }

    public Set<StateListener> getSessionListeners() {
        return stateListeners;
    }

    /**
     * 添加子监听者
     * 1）获取path对应的子监听者和目标监听这的映射Map
     * 2）若映射Map为空，则做初始化处理
     * 3）
     */
    public List<String> addChildListener(String path, final ChildListener listener) {
        ConcurrentMap<ChildListener, TargetChildListener> listeners = childListeners.get(path);
        if (listeners == null) {
            childListeners.putIfAbsent(path, new ConcurrentHashMap<ChildListener, TargetChildListener>());
            listeners = childListeners.get(path);
        }
        TargetChildListener targetListener = listeners.get(listener);
        if (targetListener == null) {
            listeners.putIfAbsent(listener, createTargetChildListener(path, listener));
            targetListener = listeners.get(listener);
        }
        return addTargetChildListener(path, targetListener);
    }

    /**
     * 移除path对应的节点
     *  1）获取path对应的ChildListener与TargetChildListener的映射Map
     *  2）若映射不为空，则移除listener对应的值，返回移除之前关联的值TargetChildListener
     *  3）若TargetChildListener不为空，则移除目标监听者removeTargetChildListener
     */
    public void removeChildListener(String path, ChildListener listener) {
        ConcurrentMap<ChildListener, TargetChildListener> listeners = childListeners.get(path);
        if (listeners != null) {
            TargetChildListener targetListener = listeners.remove(listener);
            if (targetListener != null) {
                removeTargetChildListener(path, targetListener);
            }
        }
    }

    protected void stateChanged(int state) {
        for (StateListener sessionListener : getSessionListeners()) {
            sessionListener.stateChanged(state);
        }
    }

    /**
     * 1)创建持久化节点，连接关闭后节点依然存在
     * 2)创建非持久化节点，连接关闭后节点将被删除
     */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            doClose();
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    protected abstract void doClose();

    protected abstract void createPersistent(String path); //创建持久节点

    protected abstract void createEphemeral(String path); //创建临时节点

    protected abstract TargetChildListener createTargetChildListener(String path, ChildListener listener);

    protected abstract List<String> addTargetChildListener(String path, TargetChildListener listener);

    protected abstract void removeTargetChildListener(String path, TargetChildListener listener);

}
