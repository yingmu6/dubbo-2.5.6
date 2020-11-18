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
 * A random and sequential accessible sequence of zero or more bytes (octets 八进制).
 * This interface provides an abstract view for one or more primitive(原始的) byte
 * arrays ({@code byte[]}) and {@linkplain ByteBuffer NIO buffers}.
 * <p/>
 * <h3>Creation of a buffer</h3>
 * <p/>
 * It is recommended to create a new buffer using the helper methods in {@link
 * ChannelBuffers} rather than（而不是） calling an individual(独特的) implementation's    //可以使用ChannelBuffers来创建ChannelBuffer
 * constructor.
 *        channelBuffer创建步骤
 *        1)调用ChannelBuffers的方法，比如dynamicBuffer、wrappedBuffer等
 *        2）调用工厂方法创建channelBuffer
 *
 * <p/>
 * <h3>Random Access Indexing</h3>
 * <p/>
 * Just like an ordinary primitive byte array, {@link ChannelBuffer} uses <a
 * href="http://en.wikipedia.org/wiki/Index_(information_technology)#Array_element_identifier">zero-based
 * indexing</a>. It means the index of the first byte is always {@code 0} and
 * the index of the last byte is always {@link #capacity() capacity - 1}.  For
 * example, to iterate all bytes of a buffer, you can do the following,
 * regardless（不顾的） of its internal implementation:
 * <p/>
 * <pre>
 * {@link ChannelBuffer} buffer = ...;
 * for (int i = 0; i &lt; buffer.capacity(); i ++</strong>) {
 *     byte b = buffer.getByte(i);
 *     System.out.println((char) b);
 * }
 * </pre>
 * <p/>
 * <h3>Sequential Access Indexing</h3>
 * <p/>
 * {@link ChannelBuffer} provides two pointer variables to support sequential（提供两个指针支持顺序读和写）
 * read and write operations - {@link #readerIndex() readerIndex} for a read
 * operation and {@link #writerIndex() writerIndex} for a write operation
 * respectively.  The following diagram shows how a buffer is segmented（分段的） into
 * three areas by the two pointers:
 * <p/>
 * <pre> 可废弃的 discardable
 *      +-------------------+------------------+------------------+
 *      | discardable bytes |  readable bytes  |  writable bytes  |
 *      |                   |     (CONTENT)    |                  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 * </pre>
 * <p/>
 * <h4>Readable bytes (the actual content)</h4>
 * <p/>
 * This segment is where the actual data is stored.  Any operation whose name
 * starts with {@code read} or {@code skip} will get or skip the data at the
 * current {@link #readerIndex() readerIndex} and increase it by the number of
 * read bytes.  If the argument of the read operation is also a {@link
 * ChannelBuffer} and no destination index is specified, the specified buffer's
 * {@link #readerIndex() readerIndex} is increased together.
 * <p/>
 * If there's not enough content left, {@link IndexOutOfBoundsException} is
 * raised.  The default value of newly allocated, wrapped or copied buffer's
 * {@link #readerIndex() readerIndex} is {@code 0}.
 * <p/>
 * <pre>
 * // Iterates the readable bytes of a buffer.
 * {@link ChannelBuffer} buffer = ...;
 * while (buffer.readable()) {//读取channelbuffer的内容，readindex会增加
 *     System.out.println(buffer.readByte());
 * }
 * </pre>
 * <p/>
 * <h4>Writable bytes</h4>
 * <p/>
 * This segment is a undefined space which needs to be filled.  Any operation
 * whose name ends with {@code write} will write the data at the current {@link
 * #writerIndex() writerIndex} and increase it by the number of written bytes.
 * If the argument of the write operation is also a {@link ChannelBuffer}, and
 * no source index is specified, the specified buffer's {@link #readerIndex()
 * readerIndex} is increased together.
 * <p/>
 * If there's not enough writable bytes left, {@link IndexOutOfBoundsException}
 * is raised.  The default value of newly allocated buffer's {@link
 * #writerIndex() writerIndex} is {@code 0}.  The default value of wrapped or
 * copied buffer's {@link #writerIndex() writerIndex} is the {@link #capacity()
 * capacity} of the buffer.
 * <p/>
 * <pre>
 * // Fills the writable bytes of a buffer with random integers.
 * {@link ChannelBuffer} buffer = ...;
 * while (buffer.writableBytes() >= 4) {//写入的单位是byte，如果写入int类型，自然要超过4字节
 *     buffer.writeInt(random.nextInt());
 * }
 * </pre>
 * <p/>
 * <h4>Discardable bytes</h4>
 * <p/>
 * This segment contains the bytes which were read already by a read operation.
 * Initially, the size of this segment is {@code 0}, but its size increases up
 * to the {@link #writerIndex() writerIndex} as read operations are executed.
 * The read bytes can be discarded by calling {@link #discardReadBytes()} to
 * reclaim unused area as depicted by the following diagram:
 * <p/>
 * <pre>
 *
 *  BEFORE discardReadBytes()
 *
 *      +-------------------+------------------+------------------+
 *      | discardable bytes |  readable bytes  |  writable bytes  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 *
 *
 *  AFTER discardReadBytes()   //可写的字节数变大了，可读的字节数不变
 *
 *      +------------------+--------------------------------------+
 *      |  readable bytes  |    writable bytes (got more space)   |
 *      +------------------+--------------------------------------+
 *      |                  |                                      |
 * readerIndex (0) <= writerIndex (decreased)        <=        capacity
 * </pre>
 * <p/>注意之处 废弃之后可能会出现错误
 * Please note that there is no guarantee about the content of writable bytes
 * after calling {@link #discardReadBytes()}.  The writable bytes will not be
 * moved in most cases and could even be filled with completely different data
 * depending on the underlying buffer implementation.
 * <p/>
 * <h4>Clearing the buffer indexes</h4>
 * <p/>
 * You can set both {@link #readerIndex() readerIndex} and {@link #writerIndex()
 * writerIndex} to {@code 0} by calling {@link #clear()}. It does not clear the
 * buffer content (e.g. filling with {@code 0}) but just clears the two
 * pointers.  Please also note that the semantic of this operation is different
 * from {@link ByteBuffer#clear()}.
 * <p/>
 * <pre>
 *  BEFORE clear()
 *
 *      +-------------------+------------------+------------------+
 *      | discardable bytes |  readable bytes  |  writable bytes  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 *
 *
 *  AFTER clear()
 *
 *      +---------------------------------------------------------+
 *      |             writable bytes (got more space)             |
 *      +---------------------------------------------------------+
 *      |                                                         |
 *      0 = readerIndex = writerIndex            <=            capacity
 * </pre>
 * <p/>
 * <h3>Mark and reset</h3>
 * <p/>
 * There are two marker indexes in every buffer. One is for storing {@link
 * #readerIndex() readerIndex} and the other is for storing {@link
 * #writerIndex() writerIndex}.  You can always reposition one of the two
 * indexes by calling a reset method.  It works in a similar fashion to the mark
 * and reset methods in {@link InputStream} except that there's no {@code
 * readlimit}.
 * <p/>
 * <h3>Conversion to existing JDK types</h3>  //转换类型到byte数组
 * <p/>
 * <h4>Byte array</h4>
 * <p/>
 * If a {@link ChannelBuffer} is backed by a byte array (i.e. {@code byte[]}),
 * you can access it directly via the {@link #array()} method.  To determine if
 * a buffer is backed by a byte array, {@link #hasArray()} should be used.
 * <p/>
 * <h4>NIO Buffers</h4>   //转换到java NIO中的ByteBuffer
 * <p/>
 * Various {@link #toByteBuffer()}  methods convert a {@link ChannelBuffer} into
 * one or more NIO buffers.  These methods avoid buffer allocation and memory
 * copy whenever possible, but there's no guarantee that memory copy will not be
 * involved.
 * <p/>
 * <h4>I/O Streams</h4>
 *       重写Java InputStream、OutputSteam中的内容
 * <p/>
 * Please refer to {@link ChannelBufferInputStream} and {@link
 * ChannelBufferOutputStream}.
 *
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
//有几个方法方法中的参数就是当前的接口，接口中含有当前接口的引用，这个属于什么设计模式？泛型的声明方式： 限定类型范围

//对外以接口的形式提供出去，外部调用，内部实现接口
//点击实现的时候，会把直接实现类和间接实现类展示出来

//数据缓冲区、类似java 中ByteBuffer

/**
 * 1）Java Buffer使用 2）Netty Buffer使用 3）Dubbo Buffer使用
 * 1.1) https://tech.meituan.com/2016/11/04/nio.html  http://ifeve.com/java-nio-all/  java NIO教程
 * I/O与NIO的比较
 * 1.1.1) IO面向流，只能逐字节读取，不能前后移动，NIO是面向缓冲区的，可以前后移动
 * 1.1.2）IO是阻塞的，当线程调用read()或write()时，线程会被阻塞，直到读取或写入完成
 * 1.1.3）Java NIO的选择器允许一个单独的线程来监视多个输入通道，你可以注册多个通道使用一个选择器，使用选择器“选择”通道。
 *
 * Buffer缓冲区：本质上是一块既能写入又能从中读取的内存块。包含几个参数capacity（容量），position（从0开始，读或写后向前移动到下一个位置）和limit（限制数）
 * flip()方法：翻转方法，将position置为0，写模式与读模式切换。 写或读可以通过put()、get()，也可以从通道channel里读取或往通道里写入，
 * ByteBuffer.allocate(num) 使用allocate分配
 * 通道Channel：用来处理Buffer，和Buffer进行数据交互。类似于流，但Channel是双向的
 * 从通道中读取数据read()写到缓冲区buffer，把缓冲区的数据写到write() 通道
 */
//模拟Netty的ChannelBuffer，基于Java NIO的ByteBuffer，实现传输数据的管理，可以实现转换 ByteBuffer toByteBuffer()
public interface ChannelBuffer extends Comparable<ChannelBuffer> {

    /**AbstractChannelBuffer 内容是使用ByteBuffer、索引使用的是readIndex、writeIndex
    也就是既使用了Java NIO中的ByteBuffer、又使用了Netty NIO的readIndex、writeIndex，
    那么position是怎样和readIndex、writeIndex转换的 **/

    /**
     * Returns the number of bytes (octets) this buffer can contain.
     */
    int capacity();//容量，以字节为单位

    /**
     * 将readerIndex、writerIndex索引下标置为0，内容并没处理
     * Sets the {@code readerIndex} and {@code writerIndex} of this buffer to
     * {@code 0}. This method is identical to {@link #setIndex(int, int)
     * setIndex(0, 0)}.
     * <p/>
     *
     * 提示信息说与NIO的行为不同？不同在哪
     * 解：上下文的意思是Java NIO的clear是处理limit、capacity，而dubbo中自定义的ChannelBuffer是处理readerIndex、writerIndex
     *
     *
     * Please note that the behavior of this method is different from that of
     * NIO buffer, which sets the {@code limit} to the {@code capacity} of the
     * buffer.
     */
    void clear();

    /**
     * Returns a copy of this buffer's readable bytes（可读字节的拷贝）.  Modifying the content of
     * the returned buffer or this buffer does not affect each other at all（不会对被拷贝的buffer有任何影响）.
     * This method is identical to {@code buf.copy(buf.readerIndex(),
     * buf.readableBytes())}. This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     */
    //文档中的@code 表示代码内容
    ChannelBuffer copy();

    /**
     * Returns a copy of this buffer's sub-region.  Modifying the content of the
     * returned buffer or this buffer does not affect each other at all. This
     * method does not modify {@code readerIndex} or {@code writerIndex} of this
     * buffer.
     */
    ChannelBuffer copy(int index, int length);//从原ChannelBuffer指定下标，拷贝指定长度的内容

    /**
     * Discards（放弃） the bytes between the 0th index and {@code readerIndex}. It
     * moves the bytes between {@code readerIndex} and {@code writerIndex} to
     * the 0th index, and sets {@code readerIndex} and {@code writerIndex} to
     * {@code 0} and {@code oldWriterIndex - oldReaderIndex} respectively.
     * <p/>
     * Please refer to the class documentation for more detailed explanation.
     *
     * 区间的读写下标改变，增大了可写的区域
     * newReadIndex = (0 => (oldWriteIndex-OldReadIndex))
     * newWriteIndex = (0 => capacity - newReadIndex)
     */
    void discardReadBytes();

    /**
     * Makes sure the number of {@linkplain #writableBytes() the writable bytes}
     * is equal to or greater than the specified value.  If there is enough
     * writable bytes in this buffer, this method returns with no side effect.
     * Otherwise: <ul> <li>a non-dynamic buffer will throw an {@link
     * IndexOutOfBoundsException}.</li> <li>a dynamic buffer will expand its
     * capacity so that the number of the {@link #writableBytes() writable
     * bytes} becomes equal to or greater than the specified value. The
     * expansion involves the reallocation of the internal buffer and
     * consequently memory copy.</li> </ul>
     *
     * @param writableBytes the expected minimum number of writable bytes
     * @throws IndexOutOfBoundsException if {@linkplain #writableBytes() the
     *                                   writable bytes} of this buffer is less
     *                                   than the specified value and if this
     *                                   buffer is not a dynamic buffer
     */
    void ensureWritableBytes(int writableBytes);

    /**
     * Determines if the content of the specified buffer is identical to the
     * content of this array.  'Identical'（相同的） here means: <ul> <li>the size of the
     * contents of the two buffers are same and</li> <li>every single byte of
     * the content of the two buffers are same.</li> </ul> Please note that it
     * does not compare {@link #readerIndex()} nor {@link #writerIndex()}.  This
     * method also returns {@code false} for {@code null} and an object which is
     * not an instance of {@link ChannelBuffer} type.
     */
    public boolean equals(Object o);

    /**
     * Returns the factory which creates a {@link ChannelBuffer} whose type and
     * default {@link java.nio.ByteOrder} are same with this buffer.
     * ByteOrder 字节顺序的类型安全枚举
     * BIG_ENDIAN：常量表示大字节字节顺序，从最高有效位到最低有效位排序
     * LITTLE_ENDIAN：常量表示小端字节顺序，从最低有效到最高有序排序
     */
    ChannelBufferFactory factory();

    /**
     * Gets a byte at the specified absolute {@code index} in this buffer. This
     * method does not modify {@code readerIndex} or {@code writerIndex} of this
     * buffer.
     * index是ByteBuffer里的下标，不会影响readerIndex、writerIndex
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less
     *                                   than {@code 0} or {@code index + 1} is
     *                                   greater than {@code this.capacity}
     */
    //从ChannelBuffer获取数据，有几种形式
    //1.单字节读取 2.或取到byte[] 字节数据
    // 3.获取到数据到ChannelBuffer 4.获取数据写到OutputStream

    //get、read区别：get获取数据，不会改变readIndex和writerIndex，而read会对redeaIndex改变
    byte getByte(int index);

    /**
     * Transfers this buffer's data to the specified destination starting at the
     * specified absolute {@code index}. This method does not modify {@code
     * readerIndex} or {@code writerIndex} of this buffer
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less
     *              目标                     than（小于） {@code 0} or if {@code index +
     *                                   dst.length} is greater than（大于） {@code
     *                                   this.capacity}
     */
    //获取从index开始的数据到指定的数组
    void getBytes(int index, byte[] dst);

    /**
     * Transfers this buffer's data to the specified destination starting at the
     * specified absolute {@code index}. This method does not modify {@code
     * readerIndex} or {@code writerIndex} of this buffer.
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less
     *                                   than {@code 0}, if the specified {@code
     *                                   dstIndex} is less than {@code 0}, if
     *                                   {@code index + length} is greater than
     *                                   {@code this.capacity}, or if {@code
     *                                   dstIndex + length} is greater than
     *                                   {@code dst.length}
     */
    //从当前的ChannelBuffer读取内容到目标数组dst中
    void getBytes(int index, byte[] dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at the
     * specified absolute {@code index} until the destination's position reaches
     * its limit. This method does not modify {@code readerIndex} or {@code
     * writerIndex} of this buffer while the destination's {@code position} will
     * be increased.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less
     *                                   than {@code 0} or if {@code index +
     *                                   dst.remaining()} is greater than {@code
     *                                   this.capacity}
     */
    //把从index读取到的数据，用来构造ByteBuffer
    void getBytes(int index, ByteBuffer dst);

    /**
     * Transfers this buffer's data to the specified destination starting at the
     * specified absolute {@code index} until the destination becomes
     * non-writable.  This method is basically same with {@link #getBytes(int,
     * ChannelBuffer, int, int)}, except that this method increases the {@code
     * writerIndex} of the destination by the number of the transferred bytes
     * while {@link #getBytes(int, ChannelBuffer, int, int)} does not. This
     * method does not modify {@code readerIndex} or {@code writerIndex} of the
     * source buffer (i.e. {@code this}).
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less
     *                                   than {@code 0} or if {@code index +
     *                                   dst.writableBytes} is greater than
     *                                   {@code this.capacity}
     */
    void getBytes(int index, ChannelBuffer dst);

    /**
     * Transfers this buffer's data to the specified destination starting at the
     * specified absolute {@code index}.  This method is basically same with
     * {@link #getBytes(int, ChannelBuffer, int, int)}, except that this method
     * increases the {@code writerIndex} of the destination by the number of the
     * transferred bytes while {@link #getBytes(int, ChannelBuffer, int, int)}
     * does not. This method does not modify {@code readerIndex} or {@code
     * writerIndex} of the source buffer (i.e. {@code this}).
     *
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less
     *                                   than {@code 0}, if {@code index +
     *                                   length} is greater than {@code
     *                                   this.capacity}, or if {@code length} is
     *                                   greater than {@code dst.writableBytes}
     */
    void getBytes(int index, ChannelBuffer dst, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at the
     * specified absolute {@code index}. This method does not modify {@code
     * readerIndex} or {@code writerIndex} of both the source (i.e. {@code
     * this}) and the destination.
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less
     *                                   than {@code 0}, if the specified {@code
     *                                   dstIndex} is less than {@code 0}, if
     *                                   {@code index + length} is greater than
     *                                   {@code this.capacity}, or if {@code
     *                                   dstIndex + length} is greater than
     *                                   {@code dst.capacity}
     */
    void getBytes(int index, ChannelBuffer dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified stream starting at the
     * specified absolute {@code index}. This method does not modify {@code
     * readerIndex} or {@code writerIndex} of this buffer.
     *
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less
     *                                   than {@code 0} or if {@code index +
     *                                   length} is greater than {@code
     *                                   this.capacity}
     * @throws IOException               if the specified stream threw an
     *                                   exception during I/O
     */
    void getBytes(int index, OutputStream dst, int length) throws IOException;

    /**
     * Returns {@code true} if and only if this buffer is backed by an NIO
     * direct buffer.
     */
    boolean isDirect();//是否是直接缓冲区

    /**
     * Marks the current {@code readerIndex} in this buffer.  You can reposition
     * the current {@code readerIndex} to the marked {@code readerIndex} by
     * calling {@link #resetReaderIndex()}. The initial value of the marked
     * {@code readerIndex} is {@code 0}.
     */
    void markReaderIndex();//对当前readerIndex进行标记

    /**
     * Marks the current {@code writerIndex} in this buffer.  You can reposition
     * the current {@code writerIndex} to the marked {@code writerIndex} by
     * calling {@link #resetWriterIndex()}. The initial value of the marked
     * {@code writerIndex} is {@code 0}.
     */
    void markWriterIndex();

    /**
     * Returns {@code true} if and only if {@code (this.writerIndex -
     * this.readerIndex)} is greater than {@code 0}.
     */
    boolean readable();

    /**
     * Returns the number of readable bytes which is equal to {@code
     * (this.writerIndex - this.readerIndex)}.
     */
    int readableBytes();

    /**
     * Gets a byte at the current {@code readerIndex} and increases the {@code
     * readerIndex} by {@code 1} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less
     *                                   than {@code 1}
     */

    //readByte类似getByte，只不过它会更改readIndex
    //都能单字节读取；多字节时，能写到byte[]、channelBuffer、outputStream
    byte readByte();

    /**
     * Transfers this buffer's data to the specified destination starting at the
     * current {@code readerIndex} and increases the {@code readerIndex} by the
     * number of the transferred bytes (= {@code dst.length}).
     *
     * @throws IndexOutOfBoundsException if {@code dst.length} is greater than
     *                                   {@code this.readableBytes}
     */
    //从当前readerIndex开始，读取dst.length长度的字节到目标数组dst
    void readBytes(byte[] dst);

    /**
     * Transfers this buffer's data to the specified destination starting at the
     * current {@code readerIndex} and increases the {@code readerIndex} by the
     * number of the transferred bytes (= {@code length}).
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code dstIndex} is
     *                                   less than {@code 0}, if {@code length}
     *                                   is greater than {@code this.readableBytes},
     *                                   or if {@code dstIndex + length} is
     *                                   greater than {@code dst.length}
     */
    void readBytes(byte[] dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at the
     * current {@code readerIndex} until the destination's position reaches its
     * limit, and increases the {@code readerIndex} by the number of the
     * transferred bytes.
     *
     * @throws IndexOutOfBoundsException if {@code dst.remaining()} is greater
     *                                   than {@code this.readableBytes}
     */
    void readBytes(ByteBuffer dst);

    /**
     * Transfers this buffer's data to the specified destination starting at the
     * current {@code readerIndex} until the destination becomes non-writable,
     * and increases the {@code readerIndex} by the number of the transferred
     * bytes.  This method is basically（主要地） same with {@link
     * #readBytes(ChannelBuffer, int, int)}, except that this method increases
     * the {@code writerIndex} of the destination by the number of the
     * transferred bytes while {@link #readBytes(ChannelBuffer, int, int)} does
     * not.
     *
     * @throws IndexOutOfBoundsException if {@code dst.writableBytes} is greater
     *                                   than {@code this.readableBytes}
     */
    void readBytes(ChannelBuffer dst);

    /**
     * Transfers this buffer's data to the specified destination starting at the
     * current {@code readerIndex} and increases the {@code readerIndex} by the
     * number of the transferred bytes (= {@code length}).  This method is
     * basically same with {@link #readBytes(ChannelBuffer, int, int)}, except
     * that this method increases the {@code writerIndex} of the destination by
     * the number of the transferred bytes (= {@code length}) while {@link
     * #readBytes(ChannelBuffer, int, int)} does not.
     *
     * @throws IndexOutOfBoundsException if {@code length} is greater than
     *                                   {@code this.readableBytes} or if {@code
     *                                   length} is greater than {@code
     *                                   dst.writableBytes}
     */
    void readBytes(ChannelBuffer dst, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at the
     * current {@code readerIndex} and increases the {@code readerIndex} by the
     * number of the transferred bytes (= {@code length}).
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code dstIndex} is
     *                                   less than {@code 0}, if {@code length}
     *                                   is greater than {@code this.readableBytes},
     *                                   or if {@code dstIndex + length} is
     *                                   greater than {@code dst.capacity}
     */
    void readBytes(ChannelBuffer dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to a newly created buffer starting at the
     * current {@code readerIndex} and increases the {@code readerIndex} by the
     * number of the transferred bytes (= {@code length}). The returned buffer's
     * {@code readerIndex} and {@code writerIndex} are {@code 0} and {@code
     * length} respectively.
     *
     * @param length the number of bytes to transfer
     * @return the newly created buffer which contains the transferred bytes
     * @throws IndexOutOfBoundsException if {@code length} is greater than
     *                                   {@code this.readableBytes}
     */
    ChannelBuffer readBytes(int length);

    /**
     * Repositions the current {@code readerIndex} to the marked {@code
     * readerIndex} in this buffer.
     *
     * @throws IndexOutOfBoundsException if the current {@code writerIndex} is
     *                                   less than the marked {@code
     *                                   readerIndex}
     */
    //readerIndex重置到markReaderIndex
    void resetReaderIndex();

    /**
     * Marks the current {@code writerIndex} in this buffer.  You can reposition
     * the current {@code writerIndex} to the marked {@code writerIndex} by
     * calling {@link #resetWriterIndex()}. The initial value of the marked
     * {@code writerIndex} is {@code 0}.
     */
    void resetWriterIndex();

    /**
     * Returns the {@code readerIndex} of this buffer.
     */
    int readerIndex();

    /**
     * Sets the {@code readerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code readerIndex} is
     *                                   less than {@code 0} or greater than
     *                                   {@code this.writerIndex}
     */
    //更改设置readerIndex
    void readerIndex(int readerIndex);

    /**
     * Transfers this buffer's data to the specified stream starting at the
     * current {@code readerIndex}.
     *
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if {@code length} is greater than
     *                                   {@code this.readableBytes}
     * @throws IOException               if the specified stream threw an
     *                                   exception during I/O
     */
    void readBytes(OutputStream dst, int length) throws IOException;

    /**
     * Sets the specified byte at the specified absolute {@code index} in this
     * buffer.  The 24 high-order bits of the specified value are ignored. This
     * method does not modify {@code readerIndex} or {@code writerIndex} of this
     * buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less
     *                                   than {@code 0} or {@code index + 1} is
     *                                   greater than {@code this.capacity}
     */
    //将channelBuffer指定index的字节值，替换为value值
    //set方法不会更改readerIndex，writerIndex，只是对内容进行更改
    //setByte虽然不改变readerIndex，writerIndex，但改变ByteBuffer中的position，加入多次set，会不会出现越界？ 并没有看到会影响position的地方
    void setByte(int index, int value);

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the specified absolute {@code index}. This method does not modify {@code
     * readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less
     *                                   than {@code 0} or if {@code index +
     *                                   src.length} is greater than {@code
     *                                   this.capacity}
     */
    //从index开始将源数组src的内容设置到当前channelBuffer
    void setBytes(int index, byte[] src);

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the specified absolute {@code index}. This method does not modify {@code
     * readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less
     *                                   than {@code 0}, if the specified {@code
     *                                   srcIndex} is less than {@code 0}, if
     *                                   {@code index + length} is greater than
     *                                   {@code this.capacity}, or if {@code
     *                                   srcIndex + length} is greater than
     *                                   {@code src.length}
     */
    void setBytes(int index, byte[] src, int srcIndex, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index} until the source buffer's position
     * reaches its limit. This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less
     *                                   than {@code 0} or if {@code index +
     *                                   src.remaining()} is greater than {@code
     *                                   this.capacity}
     */
    //从ByteBuffer取内容设置到当前buffer
    void setBytes(int index, ByteBuffer src);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index} until the source buffer becomes
     * unreadable.  This method is basically same with {@link #setBytes(int,
     * ChannelBuffer, int, int)}, except that this method increases the {@code
     * readerIndex} of the source buffer by the number of the transferred bytes
     * while {@link #setBytes(int, ChannelBuffer, int, int)} does not. This
     * method does not modify {@code readerIndex} or {@code writerIndex} of the
     * source buffer (i.e. {@code this}).
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less
     *                                   than {@code 0} or if {@code index +
     *                                   src.readableBytes} is greater than
     *                                   {@code this.capacity}
     */
    void setBytes(int index, ChannelBuffer src);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index}.  This method is basically same with
     * {@link #setBytes(int, ChannelBuffer, int, int)}, except that this method
     * increases the {@code readerIndex} of the source buffer by the number of
     * the transferred bytes while {@link #setBytes(int, ChannelBuffer, int,
     * int)} does not. This method does not modify {@code readerIndex} or {@code
     * writerIndex} of the source buffer (i.e. {@code this}).
     *
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less
     *                                   than {@code 0}, if {@code index +
     *                                   length} is greater than {@code
     *                                   this.capacity}, or if {@code length} is
     *                                   greater than {@code src.readableBytes}
     */
    void setBytes(int index, ChannelBuffer src, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index}. This method does not modify {@code
     * readerIndex} or {@code writerIndex} of both the source (i.e. {@code
     * this}) and the destination.
     *
     * @param srcIndex the first index of the source
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less
     *                                   than {@code 0}, if the specified {@code
     *                                   srcIndex} is less than {@code 0}, if
     *                                   {@code index + length} is greater than
     *                                   {@code this.capacity}, or if {@code
     *                                   srcIndex + length} is greater than
     *                                   {@code src.capacity}
     */
    //从原来的ChannelBuffer中的srcIndex开始复制length字节长度的内容，到目标ChannelBuffer，并从目标的index开始，
    void setBytes(int index, ChannelBuffer src, int srcIndex, int length);

    /**
     * Transfers the content of the specified source stream to this buffer
     * starting at the specified absolute {@code index}. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @param length the number of bytes to transfer
     * @return the actual number of bytes read in from the specified channel.
     * {@code -1} if the specified channel is closed.
     * @throws IndexOutOfBoundsException if the specified {@code index} is less
     *                                   than {@code 0} or if {@code index +
     *                                   length} is greater than {@code
     *                                   this.capacity}
     * @throws IOException               if the specified stream threw an
     *                                   exception during I/O
     */
    int setBytes(int index, InputStream src, int length) throws IOException;

    /**
     * Sets the {@code readerIndex} and {@code writerIndex} of this buffer in
     * one shot.  This method is useful when you have to worry about the
     * invocation order of {@link #readerIndex(int)} and {@link
     * #writerIndex(int)} methods.  For example, the following code will fail:
     * <p/>
     * <pre>
     * // Create a buffer whose readerIndex, writerIndex and capacity are
     * // 0, 0 and 8 respectively.
     * {@link ChannelBuffer} buf = {@link ChannelBuffers}.buffer(8);
     *
     * // IndexOutOfBoundsException is thrown because the specified
     * // readerIndex (2) cannot be greater than the current writerIndex (0).
     * buf.readerIndex(2);
     * buf.writerIndex(4);
     * </pre>
     * <p/>
     * The following code will also fail:
     * <p/>
     * <pre>
     * // Create a buffer whose readerIndex, writerIndex and capacity are
     * // 0, 8 and 8 respectively.
     * {@link ChannelBuffer} buf = {@link ChannelBuffers}.wrappedBuffer(new
     * byte[8]);
     *
     * // readerIndex becomes 8.
     * buf.readLong();
     *
     * // IndexOutOfBoundsException is thrown because the specified
     * // writerIndex (4) cannot be less than the current readerIndex (8).
     * buf.writerIndex(4);
     * buf.readerIndex(2);
     * </pre>
     * <p/>
     * By contrast, {@link #setIndex(int, int)} guarantees that it never throws
     * an {@link IndexOutOfBoundsException} as long as the specified indexes
     * meet basic constraints, regardless what the current index values of the
     * buffer are:
     * <p/>
     * <pre>
     * // No matter what the current state of the buffer is, the following
     * // call always succeeds as long as the capacity of the buffer is not
     * // less than 4.
     * buf.setIndex(2, 4);
     * </pre>
     *
     * @throws IndexOutOfBoundsException if the specified {@code readerIndex} is
     *                                   less than 0, if the specified {@code
     *                                   writerIndex} is less than the specified
     *                                   {@code readerIndex} or if the specified
     *                                   {@code writerIndex} is greater than
     *                                   {@code this.capacity}
     */
    //手动设置读下标、写下标，避免越界异常
    void setIndex(int readerIndex, int writerIndex);

    /**
     * Increases the current {@code readerIndex} by the specified {@code length}
     * in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code length} is greater than
     *                                   {@code this.readableBytes}
     */
    //将readerIndex跳过指定的长度
    void skipBytes(int length);

    /**
     * Converts this buffer's readable bytes into a NIO buffer.  The returned
     * buffer might or might not share the content with this buffer, while they
     * have separate indexes and marks.  This method is identical to {@code
     * buf.toByteBuffer(buf.readerIndex(), buf.readableBytes())}. This method
     * does not modify {@code readerIndex} or {@code writerIndex} of this
     * buffer.
     *
     * 将Dubbo的ChannelBuffer转换为Netty中的ByteBuffer
     */
    ByteBuffer toByteBuffer();

    /**
     * Converts this buffer's sub-region into a NIO buffer.  The returned buffer
     * might or might not share the content with this buffer, while they have
     * separate indexes and marks. This method does not modify {@code
     * readerIndex} or {@code writerIndex} of this buffer.
     */
    ByteBuffer toByteBuffer(int index, int length);

    /**
     * Returns {@code true} if and only if {@code (this.capacity -
     * this.writerIndex)} is greater than {@code 0}.
     */
    boolean writable();

    /**
     * Returns the number of writable bytes which is equal to {@code
     * (this.capacity - this.writerIndex)}.
     */
    int writableBytes();

    /**
     * Sets the specified byte at the current {@code writerIndex} and increases
     * the {@code writerIndex} by {@code 1} in this buffer. The 24 high-order
     * bits of the specified value are ignored.
     *
     * @throws IndexOutOfBoundsException if {@code this.writableBytes} is less
     *                                   than {@code 1}
     */
    void writeByte(int value);

    /**
     * Transfers（传输） the specified source array's data to this buffer starting at
     * the current {@code writerIndex} and increases（增加） the {@code writerIndex} by
     * the number of the transferred bytes (= {@code src.length}).
     *
     * @throws IndexOutOfBoundsException if {@code src.length} is greater than
     *                                   {@code this.writableBytes}
     */
    void writeBytes(byte[] src);

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex} by
     * the number of the transferred bytes (= {@code length}).
     *
     * @param index  the first index of the source
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code srcIndex} is
     *                                   less than {@code 0}, if {@code srcIndex
     *                                   + length} is greater than {@code
     *                                   src.length}, or if {@code length} is
     *                                   greater than {@code this.writableBytes}
     */
    //将指定的数组传输给buffer，并且从writerIndex指定的下边index，偏移指定长度
    void writeBytes(byte[] src, int index, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} until the source buffer's position
     * reaches its limit, and increases the {@code writerIndex} by the number of
     * the transferred bytes.
     *
     * @throws IndexOutOfBoundsException if {@code src.remaining()} is greater
     *                                   than {@code this.writableBytes}
     */
    void writeBytes(ByteBuffer src);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} until the source buffer becomes
     * unreadable, and increases the {@code writerIndex} by the number of the
     * transferred bytes.  This method is basically same with {@link
     * #writeBytes(ChannelBuffer, int, int)}, except that this method increases
     * the {@code readerIndex} of the source buffer by the number of the
     * transferred bytes while {@link #writeBytes(ChannelBuffer, int, int)} does
     * not.
     *
     * @throws IndexOutOfBoundsException if {@code src.readableBytes} is greater
     *                                   than {@code this.writableBytes}
     */
    void writeBytes(ChannelBuffer src);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex} by
     * the number of the transferred bytes (= {@code length}).  This method is
     * basically same with {@link #writeBytes(ChannelBuffer, int, int)}, except
     * that this method increases the {@code readerIndex} of the source buffer
     * by the number of the transferred bytes (= {@code length}) while {@link
     * #writeBytes(ChannelBuffer, int, int)} does not.
     *
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if {@code length} is greater than
     *                                   {@code this.writableBytes} or if {@code
     *                                   length} is greater then {@code
     *                                   src.readableBytes}
     */
    void writeBytes(ChannelBuffer src, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex} by
     * the number of the transferred bytes (= {@code length}).
     *
     * @param srcIndex the first index of the source
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code srcIndex} is
     *                                   less than {@code 0}, if {@code srcIndex
     *                                   + length} is greater than {@code
     *                                   src.capacity}, or if {@code length} is
     *                                   greater than {@code this.writableBytes}
     */
    //把原ChannelBuffer的内容写到当前的对象中
    void writeBytes(ChannelBuffer src, int srcIndex, int length);

    /**
     * Transfers the content of the specified stream to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex} by
     * the number of the transferred bytes.
     *
     * @param length the number of bytes to transfer
     * @return the actual number of bytes read in from the specified stream
     * @throws IndexOutOfBoundsException if {@code length} is greater than
     *                                   {@code this.writableBytes}
     * @throws IOException               if the specified stream threw an
     *                                   exception during I/O
     */
    int writeBytes(InputStream src, int length) throws IOException;

    /**
     * Returns the {@code writerIndex} of this buffer.
     */
    int writerIndex();

    /**
     * Sets the {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code writerIndex} is
     *                                   less than {@code this.readerIndex} or
     *                                   greater than {@code this.capacity}
     */
    void writerIndex(int writerIndex);

    /**
     * Returns the backing byte array of this buffer.
     *
     * @throws UnsupportedOperationException if there no accessible backing byte
     *                                       array
     */
    byte[] array(); //返回buffer数组

    /**
     * Returns {@code true} if and only if this buffer has a backing byte array.
     * If this method returns true, you can safely call {@link #array()} and
     * {@link #arrayOffset()}.
     */
    boolean hasArray();

    /**
     * Returns the offset of the first byte within the backing byte array of
     * this buffer.
     *
     * @throws UnsupportedOperationException if there no accessible backing byte
     *                                       array
     */
    int arrayOffset();
}
