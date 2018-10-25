package com.alibaba.dubbo.remoting.buffer;

import junit.framework.Assert;

/**
 * @author <a href="mailto:gang.lvg@taobao.com">kimi</a>
 */
public class HeapChannelBufferTest extends AbstractChannelBufferTest {

    private ChannelBuffer buffer;

    @Override
    protected ChannelBuffer newBuffer(int capacity) {
        //创建capacity大小的字节数据数组，readIndex、writeIndex下标都为0
        buffer = ChannelBuffers.buffer(capacity);
        Assert.assertEquals(0, buffer.writerIndex());
        return buffer;
    }

    @Override
    protected ChannelBuffer[] components() {
        return new ChannelBuffer[]{buffer};
    }
}
