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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class HeapChannelBuffer extends AbstractChannelBuffer {
    //模仿Netty中的HeapChannelBuffer，固定大小的buffer

    //这个类与用途？
    //解：分配堆内buffer
    /**
     * The underlying(潜在的、在下面的) heap byte array that this buffer is wrapping.
     */
    //底层数据结构是固定大小的数组array
    protected final byte[] array;

    /**
     * Creates a new heap buffer with a newly allocated byte array.
     *
     * @param length the length of the new byte array
     */
    public HeapChannelBuffer(int length) {//构造函数重载，不完整参数的函数最终归并到一个完整的函数
        this(new byte[length], 0, 0);
    }

    /**
     * heap buffer有啥不同？
     * 解：固定大小的buffer
     * Creates a new heap buffer with an existing byte array.
     *
     * @param array the byte array to wrap
     */
    public HeapChannelBuffer(byte[] array) {
        this(array, 0, array.length);//this() 调用当前函数，super()调用父类函数
    }

    /**
     * Creates a new heap buffer with an existing byte array.
     *函数
     * @param array       the byte array to wrap
     * @param readerIndex the initial reader index of this buffer
     * @param writerIndex the initial writer index of this buffer
     */
    protected HeapChannelBuffer(byte[] array, int readerIndex, int writerIndex) {
        if (array == null) {
            throw new NullPointerException("array");
        }
        this.array = array;
        setIndex(readerIndex, writerIndex);//索引的处理，使用父类AbstractChannelBuffer
    }

    public boolean isDirect() {
        return false;
    }

    public int capacity() {
        return array.length;
    }

    public boolean hasArray() {
        return true;
    }

    public byte[] array() {
        return array;
    }

    public int arrayOffset() {
        return 0;
    }

    public byte getByte(int index) {//单个获取字节
        return array[index];
    }

    //子类赋值给父类，向上转型；封装类不能互相强制转换，比如Long不能转换Integer
    public void getBytes(int index, ChannelBuffer dst, int dstIndex, int length) {
        if (dst instanceof HeapChannelBuffer) {//如果是当前对象的实例，就在当前类中实现
            getBytes(index, ((HeapChannelBuffer) dst).array, dstIndex, length);
        } else {                               //如果是其它对象的实例，就由对应的类执行
            dst.setBytes(dstIndex, array, index, length);
        }
    }

    public void getBytes(int index, byte[] dst, int dstIndex, int length) {
        //使用System复制数组，将指定数组从原数组指定位置开始，复制执行长度到目标数组，目标数组从dstIndex开始
        System.arraycopy(array, index, dst, dstIndex, length);
    }

    public void getBytes(int index, ByteBuffer dst) {
        dst.put(array, index, Math.min(capacity() - index, dst.remaining()));
    }

    public void getBytes(int index, OutputStream out, int length)
            throws IOException {
        out.write(array, index, length);
    }

    public int getBytes(int index, GatheringByteChannel out, int length)
            throws IOException {
        return out.write(ByteBuffer.wrap(array, index, length));
    }

    public void setByte(int index, int value) {
        array[index] = (byte) value;
    }

    public void setBytes(int index, ChannelBuffer src, int srcIndex, int length) {
        //判断是否是当前类的对象实例，如果是，就在当前类中实现，如果不是，交由对应的类实现
        if (src instanceof HeapChannelBuffer) {
            setBytes(index, ((HeapChannelBuffer) src).array, srcIndex, length);
        } else {
            src.getBytes(srcIndex, array, index, length);
        }
    }

    //把数组src的值设置给当前对象
    public void setBytes(int index, byte[] src, int srcIndex, int length) {
        //将指定源数组中的数组从指定位置复制到目标数组的指定位置
        System.arraycopy(src, srcIndex, array, index, length);
    }

    public void setBytes(int index, ByteBuffer src) {
        src.get(array, index, src.remaining());
    }

    public int setBytes(int index, InputStream in, int length) throws IOException {
        int readBytes = 0;
        do {
            int localReadBytes = in.read(array, index, length);
            if (localReadBytes < 0) {
                if (readBytes == 0) {
                    return -1;
                } else {
                    break;
                }
            }
            readBytes += localReadBytes;
            index += localReadBytes;
            length -= localReadBytes;
        } while (length > 0);

        return readBytes;
    }

    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(array, index, length);
        int readBytes = 0;

        do {
            int localReadBytes;
            try {
                localReadBytes = in.read(buf);
            } catch (ClosedChannelException e) {
                localReadBytes = -1;
            }
            if (localReadBytes < 0) {
                if (readBytes == 0) {
                    return -1;
                } else {
                    break;
                }
            } else if (localReadBytes == 0) {
                break;
            }
            readBytes += localReadBytes;
        } while (readBytes < length);

        return readBytes;
    }

    public ChannelBuffer copy(int index, int length) {
        if (index < 0 || length < 0 || index + length > array.length) {
            throw new IndexOutOfBoundsException();
        }

        byte[] copiedArray = new byte[length];
        System.arraycopy(array, index, copiedArray, 0, length);
        return new HeapChannelBuffer(copiedArray);
    }

    //选择对应的工厂创建实例
    public ChannelBufferFactory factory() {
        return HeapChannelBufferFactory.getInstance();
    }

    public ByteBuffer toByteBuffer(int index, int length) {
        return ByteBuffer.wrap(array, index, length);
    }
}
