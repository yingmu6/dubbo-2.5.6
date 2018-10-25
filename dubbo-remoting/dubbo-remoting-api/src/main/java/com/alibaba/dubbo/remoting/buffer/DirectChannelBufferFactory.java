/*
 * Copyright 1999-2012 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.dubbo.remoting.buffer;

import java.nio.ByteBuffer;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class DirectChannelBufferFactory implements ChannelBufferFactory {
    //直接的buffer(非堆内buffer，当进行网络IO时，不需要将数据复制到操作系统内核，也就是不需要中间的缓冲区)
    private static final DirectChannelBufferFactory INSTANCE = new DirectChannelBufferFactory();

    public DirectChannelBufferFactory() {
        super();
    }

    public static ChannelBufferFactory getInstance() {
        return INSTANCE;
    }

    public ChannelBuffer getBuffer(int capacity) {//方法重载，一般都会归并到一个方法
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity: " + capacity);
        }
        if (capacity == 0) {
            return ChannelBuffers.EMPTY_BUFFER;//创建堆buffer 有何不同？解：堆内buffer，在堆内创建和销毁
        }
        return ChannelBuffers.directBuffer(capacity);
    }

    public ChannelBuffer getBuffer(byte[] array, int offset, int length) {//指定了数组值
        if (array == null) {
            throw new NullPointerException("array");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("offset: " + offset);
        }
        if (length == 0) {
            return ChannelBuffers.EMPTY_BUFFER;
        }
        if (offset + length > array.length) {
            throw new IndexOutOfBoundsException("length: " + length);
        }

        ChannelBuffer buf = getBuffer(length);
        buf.writeBytes(array, offset, length);
        return buf;
    }

    public ChannelBuffer getBuffer(ByteBuffer nioBuffer) {
        if (!nioBuffer.isReadOnly() && nioBuffer.isDirect()) {
            return ChannelBuffers.wrappedBuffer(nioBuffer);
        }

        ChannelBuffer buf = getBuffer(nioBuffer.remaining());
        int pos = nioBuffer.position();
        buf.writeBytes(nioBuffer);
        nioBuffer.position(pos);
        return buf;
    }

}
