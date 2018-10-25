package com.alibaba.dubbo.remoting.buffer;

import java.nio.ByteBuffer;

/**
 * @author <a href="mailto:gang.lvg@taobao.com">kimi</a>
 */
public class ByteBufferBackedChannelBufferTest extends AbstractChannelBufferTest {

    private ChannelBuffer buffer;

    //
    @Override
    protected ChannelBuffer newBuffer(int capacity) {
        //allocate 分配新的缓冲区
        buffer = new ByteBufferBackedChannelBuffer(ByteBuffer.allocate(capacity));
        return buffer;
    }

    @Override
    protected ChannelBuffer[] components() {
        return new ChannelBuffer[]{buffer};
    }

    public static void main(String[] args) {
        System.out.println(11);
    }
}
