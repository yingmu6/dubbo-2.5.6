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
 */
// 抽象类中不一定包含抽象方法，但是有抽象方法的类必定是抽象类。
// 抽象类不能被实例化,必须由它的之类

//把公共的部分提取出来，放在父类实现，个性化的功能放在子类实现，NettyBackedChannelBuffer是完全使用Netty实现，
// 不能共用AbstractChannelBuffer的方法，所以单独实现ChannelBuffer类
public abstract class AbstractChannelBuffer implements ChannelBuffer { //finish understand(完成理解)
    //主要是对ChannelBuffer的读写下标做抽象对象，供子类调用

    // 0 <= readerIndex <= writerIndex <= capacity
    // 0到readerIndex 是discard废弃的
    // rederIndex到writerIndex 是可读的
    // writerIndex到capacity时可写的

    //rederIndex、writerIndex的操作和Netty中的操作思路类似
    private int readerIndex;//读索引位置

    private int writerIndex;//写索引位置

    private int markedReaderIndex;//读索引的标记位置

    private int markedWriterIndex;//写索引的标记位置

    //返回读索引下标（通过有无参数来判断是读和写）
    public int readerIndex() {
        return readerIndex;
    }

    //设置读索引下标（0 <= readerIndex <= writerIndex）
    public void readerIndex(int readerIndex) {
        if (readerIndex < 0 || readerIndex > writerIndex) {
            throw new IndexOutOfBoundsException();
        }
        this.readerIndex = readerIndex;
    }

    //同上读写下标
    public int writerIndex() {
        return writerIndex;
    }

    //（readerIndex <= writerIndex <= capacity）
    // capacity()交由子类实现，因为抽象类不能被实例，必然有一个子类实现，这里就调用子类的方法
    public void writerIndex(int writerIndex) {
        //判断容量以及下表
        if (writerIndex < readerIndex || writerIndex > capacity()) {
            throw new IndexOutOfBoundsException();
        }
        this.writerIndex = writerIndex;
    }

    //设置读和写下标
    public void setIndex(int readerIndex, int writerIndex) {
        //判断read和write的index
        if (readerIndex < 0 || readerIndex > writerIndex || writerIndex > capacity()) {
            throw new IndexOutOfBoundsException();//跑Java中的运行时异常
        }
        this.readerIndex = readerIndex;
        this.writerIndex = writerIndex;
    }

    //清除时，将读写下标置为0，没有将内容显示的调用System.gc(),因为有垃圾回收器
    public void clear() {
        readerIndex = writerIndex = 0;
    }

    //判断是否可读的（看可读的字节是否大于0）
    public boolean readable() {
        return readableBytes() > 0;
    }

    //判断是否可写的（看可写的字节是否大于0）
    public boolean writable() {
        return writableBytes() > 0;
    }

    //返回可读字节数
    public int readableBytes() {
        return writerIndex - readerIndex;
    }

    //返回可写字节数
    public int writableBytes() {
        return capacity() - writerIndex;
    }

    //标记读下标，记住上一次的读下标
    public void markReaderIndex() {
        //将读索引下标复制给标记下标
        markedReaderIndex = readerIndex;
    }

    //重置读下标，返回到上一次的读下标
    public void resetReaderIndex() {//重置读索引下标 ，将readerIndex重置到markedReaderIndex
        readerIndex(markedReaderIndex);
    }

    //与读下标的标记、重置相似
    public void markWriterIndex() {
        markedWriterIndex = writerIndex;
    }

    public void resetWriterIndex() {
        writerIndex = markedWriterIndex;
    }

    //将discard清除
    public void discardReadBytes() {
        if (readerIndex == 0) {
            return;
        }
        //从原来的ChannelBuffer中读取内容，写到ChannelBuffer中
        setBytes(0, this, readerIndex, writerIndex - readerIndex);
        writerIndex -= readerIndex;//writerIndex往前移动writerIndex-readerIndex（可读的字节数）
        // markedReaderIndex与readerIndex大小关系？
        // 解：markedReaderIndex是记住上一次的readerIndex，而在有效范围内可前后移动，比如markedReaderIndex等于3，readerIndex可能为1，
        // 所以清除以后readerIndex=0，markedReaderIndex也往前移动了，为3-1=2
        markedReaderIndex = Math.max(markedReaderIndex - readerIndex, 0);
        markedWriterIndex = Math.max(markedWriterIndex - readerIndex, 0);
        readerIndex = 0;
    }

    //确保是否能写
    public void ensureWritableBytes(int writableBytes) {
        if (writableBytes > writableBytes()) {
            throw new IndexOutOfBoundsException();
        }
    }

    //见ChannelBuffer示意（从当前对象获取内容，写到指定数组）
    public void getBytes(int index, byte[] dst) {
        getBytes(index, dst, 0, dst.length);
    }

    public void getBytes(int index, ChannelBuffer dst) {
        getBytes(index, dst, dst.writableBytes());
    }

    public void getBytes(int index, ChannelBuffer dst, int length) {
        if (length > dst.writableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        getBytes(index, dst, dst.writerIndex(), length);
        dst.writerIndex(dst.writerIndex() + length);
    }

    //把数组内容设置到当前对象
    public void setBytes(int index, byte[] src) {
        setBytes(index, src, 0, src.length);
    }

    public void setBytes(int index, ChannelBuffer src) {
        setBytes(index, src, src.readableBytes());
    }

    public void setBytes(int index, ChannelBuffer src, int length) {
        if (length > src.readableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        //不是递归调用，通过参数来区分函数重载
        setBytes(index, src, src.readerIndex(), length);
        src.readerIndex(src.readerIndex() + length);
    }

    //readByte内部调用getByte方法获取
    public byte readByte() {
        if (readerIndex == writerIndex) {
            throw new IndexOutOfBoundsException();
        }
        //readerIndex变量自增1
        return getByte(readerIndex++);
    }

    //确定对象的引用以及调用的方法来自哪里，是来自当前类还是其它类
    public ChannelBuffer readBytes(int length) {//用ChannelBuffer接收读取的内容
        //检查边界是否会异常
        checkReadableBytes(length);
        if (length == 0) {
            return ChannelBuffers.EMPTY_BUFFER;
        }
        //通过子类选择对应的工厂创建，factory()返回指定的工厂（通过向上转型实现，哪个子类实现，就掉哪个子类的方法）
        ChannelBuffer buf = factory().getBuffer(length);
        //把当前对象this中指定内容写到新创建的ChannelBuffer
        buf.writeBytes(this, readerIndex, length);
        readerIndex += length;
        return buf;
    }

    //使用字节数组接收读取的内容
    public void readBytes(byte[] dst, int dstIndex, int length) {
        checkReadableBytes(length);
        getBytes(readerIndex, dst, dstIndex, length);
        readerIndex += length;
    }

    public void readBytes(byte[] dst) {
        readBytes(dst, 0, dst.length);
    }

    public void readBytes(ChannelBuffer dst) {
        readBytes(dst, dst.writableBytes());
    }

    //使用ChannelBuffer接受读取的内容
    public void readBytes(ChannelBuffer dst, int length) {
        if (length > dst.writableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        readBytes(dst, dst.writerIndex(), length);
        dst.writerIndex(dst.writerIndex() + length);
    }

    public void readBytes(ChannelBuffer dst, int dstIndex, int length) {
        checkReadableBytes(length);
        getBytes(readerIndex, dst, dstIndex, length);
        readerIndex += length;
    }

    //使用java NIO 的ByteBuffer来接收读取的内容
    public void readBytes(ByteBuffer dst) {
        int length = dst.remaining();
        checkReadableBytes(length);
        getBytes(readerIndex, dst);
        readerIndex += length;
    }

    //把读取到的内容写到输出流中
    public void readBytes(OutputStream out, int length) throws IOException {
        checkReadableBytes(length);
        getBytes(readerIndex, out, length);
        readerIndex += length;
    }

    public void skipBytes(int length) {
        int newReaderIndex = readerIndex + length;
        if (newReaderIndex > writerIndex) {
            throw new IndexOutOfBoundsException();
        }
        readerIndex = newReaderIndex;
    }

    //子类若是HeapChannelBuffer，则是数组操作
    //子类若是ByteBufferBackedChannelBuffer,则是java NIO中的ByteBuffer操作
    //子类若是DynamicChannelBuffer 则是动态扩容？  解：是的
    public void writeByte(int value) {
        //使用writerIndex++表达式，不紧可以改变表达式值，也可以改变变量的值
        setByte(writerIndex++, value);
    }

    public void writeBytes(byte[] src, int srcIndex, int length) {
        setBytes(writerIndex, src, srcIndex, length);
        writerIndex += length;
    }

    //把数组内容写到当前ChannelBuffer
    public void writeBytes(byte[] src) {
        writeBytes(src, 0, src.length);
    }

    public void writeBytes(ChannelBuffer src) {
        writeBytes(src, src.readableBytes());
    }

    //todo @csy-h3 待调试，看数值
    public void writeBytes(ChannelBuffer src, int length) {
        if (length > src.readableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        writeBytes(src, src.readerIndex(), length);
        src.readerIndex(src.readerIndex() + length);
    }

    public void writeBytes(ChannelBuffer src, int srcIndex, int length) {
        setBytes(writerIndex, src, srcIndex, length);
        writerIndex += length;
    }

    public void writeBytes(ByteBuffer src) {
        int length = src.remaining();
        setBytes(writerIndex, src);
        writerIndex += length;
    }

    public int writeBytes(InputStream in, int length) throws IOException {
        int writtenBytes = setBytes(writerIndex, in, length);
        if (writtenBytes > 0) {
            writerIndex += writtenBytes;
        }
        return writtenBytes;
    }

    //将当前可读字节复制到新建的ChannelBuffer
    public ChannelBuffer copy() {
        return copy(readerIndex, readableBytes());
    }

    //将当前的对象转换到ByteBuffer
    public ByteBuffer toByteBuffer() {
        return toByteBuffer(readerIndex, readableBytes());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChannelBuffer
                && ChannelBuffers.equals(this, (ChannelBuffer) o);
    }

    //比较两个对象是否相等
    public int compareTo(ChannelBuffer that) {
        return ChannelBuffers.compare(this, that);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' +
                "ridx=" + readerIndex + ", " +
                "widx=" + writerIndex + ", " +
                "cap=" + capacity() +
                ')';
    }

    //检查可读的字节是否正常
    protected void checkReadableBytes(int minimumReadableBytes) {
        if (readableBytes() < minimumReadableBytes) {
            throw new IndexOutOfBoundsException();
        }
    }
}
