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

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 * Backed 支持
 * ByteBufferBackedChannelBuffer  使用ByteBuffer支持ChannelBuffer
 */
// 重点调试、涉及java NIO
//用java ByteBuffer支持ChannelBuffer的构造
public class ByteBufferBackedChannelBuffer extends AbstractChannelBuffer {
    //底层的数据结构是Java NIO 的ByteBuffer
    //两部分组成：1）使用ByteBuffer存储内容、2）使用AbstractChannelBuffer处理下标

    //ByteBuffer 标记，位置，极限和容量值的以下不变量保持不变：
    //0 <= mark <= position <= limit <= capacity
    //remaining（剩余的元素）= limit - position 读和写都会改变position的位置
    private final ByteBuffer buffer;

    private final int capacity;

    //注意：构造的时候，writeIndex等值于capacity，所以是不可写状态，如果要写入数据，需要改变写下边，不然会越界异常
    //调用setIndex(readIndex,writeIndex);
    public ByteBufferBackedChannelBuffer(ByteBuffer buffer) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        //创建一个新的字节缓冲区，共享此缓冲区的内容。
        // 比较模糊，需要test数据显示？
        // 解决：slice是共享一段序列，新的开始位置是老的当前位置，容量是老的剩余容量
        this.buffer = buffer.slice();//内容共享、下标、位置等各自维护
        //返回当前位置和限制之间的元素数。
        capacity = buffer.remaining();
        // 此处不用管读索引吗？莫非已经有了
        // 解答：读索引默认为0，只要把写索引改为capacity，表明能写的字节数
        writerIndex(capacity);
    }

    //子类继承了父类中除构造函数的所有方法，所以可以直接使用父类的方法
    public ByteBufferBackedChannelBuffer(ByteBufferBackedChannelBuffer buffer) {
        this.buffer = buffer.buffer;
        capacity = buffer.capacity;
        setIndex(buffer.readerIndex(), buffer.writerIndex());
    }

    public ChannelBufferFactory factory() {
        //直接与非直接缓冲区
        if (buffer.isDirect()) {
            return DirectChannelBufferFactory.getInstance();
        } else {
            return HeapChannelBufferFactory.getInstance();
        }
    }


    public int capacity() {
        return capacity;
    }


    public ChannelBuffer copy(int index, int length) {
        ByteBuffer src;
        try {
            //todo @csy-h1 待数据分析  从当前buffer拷贝数据吗？
            src = (ByteBuffer) buffer.duplicate().position(index).limit(index + length);
        } catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException();
        }

        ByteBuffer dst = buffer.isDirect()
                ? ByteBuffer.allocateDirect(length)
                : ByteBuffer.allocate(length);
        dst.put(src);
        dst.clear();
        return new ByteBufferBackedChannelBuffer(dst);
    }


    //获取指定下标的值
    public byte getByte(int index) {
        return buffer.get(index);
    }


    public void getBytes(int index, byte[] dst, int dstIndex, int length) {
        ByteBuffer data = buffer.duplicate();
        try {
            //NIO 中limit，duplicate API调试调用?
            //因为duplicate复用了之前的索引，需要重新设置一下
            data.limit(index + length).position(index);
        } catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException();
        }
        data.get(dst, dstIndex, length);
    }


    public void getBytes(int index, ByteBuffer dst) {
        ByteBuffer data = buffer.duplicate();
        int bytesToCopy = Math.min(capacity() - index, dst.remaining());
        try {
            //设置limit、position
            data.limit(index + bytesToCopy).position(index);
        } catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException();
        }
        dst.put(data);
    }

    //todo @csy-h1 待调试
    public void getBytes(int index, ChannelBuffer dst, int dstIndex, int length) {
        if (dst instanceof ByteBufferBackedChannelBuffer) {
            ByteBufferBackedChannelBuffer bbdst = (ByteBufferBackedChannelBuffer) dst;
            ByteBuffer data = bbdst.buffer.duplicate();

            data.limit(dstIndex + length).position(dstIndex);
            getBytes(index, data);
        } else if (buffer.hasArray()) {
            dst.setBytes(dstIndex, buffer.array(), index + buffer.arrayOffset(), length);
        } else {
            dst.setBytes(dstIndex, this, index, length);
        }
    }


    public void getBytes(int index, OutputStream out, int length) throws IOException {
        if (length == 0) {
            return;
        }

        if (buffer.hasArray()) {
            out.write(
                    buffer.array(),
                    index + buffer.arrayOffset(),
                    length);
        } else {
            byte[] tmp = new byte[length];
            ((ByteBuffer) buffer.duplicate().position(index)).get(tmp);
            out.write(tmp);
        }
    }


    public boolean isDirect() {
        return buffer.isDirect();
    }


    //往buffer设置值
    public void setByte(int index, int value) {
        buffer.put(index, (byte) value);
    }


    public void setBytes(int index, byte[] src, int srcIndex, int length) {
        //此处是复制缓冲区，但是既没有返回，也没有引用传递，怎么使用的？
        //解决：此处复制的缓冲区，老的改动、新的也会改动，新的改动，老的缓冲区也会改动
        ByteBuffer data = buffer.duplicate();//局部变量
        data.limit(index + length).position(index);
        data.put(src, srcIndex, length);
    }


    public void setBytes(int index, ByteBuffer src) {
        ByteBuffer data = buffer.duplicate();
        data.limit(index + src.remaining()).position(index);
        data.put(src);
    }


    //todo @csy-h1 此处待推理运算
    public void setBytes(int index, ChannelBuffer src, int srcIndex, int length) {
        if (src instanceof ByteBufferBackedChannelBuffer) {
            ByteBufferBackedChannelBuffer bbsrc = (ByteBufferBackedChannelBuffer) src;
            ByteBuffer data = bbsrc.buffer.duplicate();

            data.limit(srcIndex + length).position(srcIndex);
            setBytes(index, data);
        } else if (buffer.hasArray()) {
            src.getBytes(srcIndex, buffer.array(), index + buffer.arrayOffset(), length);
        } else {
            src.getBytes(srcIndex, this, index, length);
        }
    }


    public ByteBuffer toByteBuffer(int index, int length) {
        if (index == 0 && length == capacity()) {
            return buffer.duplicate();
        } else {
            return ((ByteBuffer) buffer.duplicate().position(
                    index).limit(index + length)).slice();
        }
    }


    //todo @csy-h1 待调试
    public int setBytes(int index, InputStream in, int length) throws IOException {
        int readBytes = 0;

        if (buffer.hasArray()) {
            index += buffer.arrayOffset();
            do {
                int localReadBytes = in.read(buffer.array(), index, length);
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
        } else {
            byte[] tmp = new byte[length];
            int i = 0;
            do {
                int localReadBytes = in.read(tmp, i, tmp.length - i);
                if (localReadBytes < 0) {
                    if (readBytes == 0) {
                        return -1;
                    } else {
                        break;
                    }
                }
                readBytes += localReadBytes;
                i += readBytes;
            } while (i < tmp.length);
            ((ByteBuffer) buffer.duplicate().position(index)).put(tmp);
        }

        return readBytes;
    }


    public byte[] array() {
        return buffer.array();
    }


    public boolean hasArray() {
        return buffer.hasArray();
    }


    public int arrayOffset() {
        return buffer.arrayOffset();
    }
}
