/*
 * Copyright 1999-2011 Alibaba Group.
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
package com.alibaba.dubbo.common.serialize.support.dubbo;

import com.alibaba.dubbo.common.serialize.DataOutput;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Default data output impl.
 * Not thread-safe.
 *
 * @author qian.lei
 */

public class GenericDataOutput implements DataOutput, GenericDataFlags {
    private static final int CHAR_BUF_SIZE = 256;

    private final byte[] mBuffer, mTemp = new byte[9]; //mBuffer数据存储的数组

    private final char[] mCharBuf = new char[CHAR_BUF_SIZE];

    private final OutputStream mOutput;

    private final int mLimit; //限制数

    private int mPosition = 0; //游标所处位置

    public GenericDataOutput(OutputStream out) { //构造对象时，可以指定字节数，若不指定则默认为1024
        this(out, 1024);
    }

    public GenericDataOutput(OutputStream out, int buffSize) {
        mOutput = out;
        mLimit = buffSize;
        mBuffer = new byte[buffSize];
    }

    public void writeBool(boolean v) throws IOException { //写入值操作：写入到数组中、变更游标
        write0(v ? VARINT_1 : VARINT_0);
    }

    public void writeByte(byte v) throws IOException {//todo @pause 1.5
        switch (v) {
            case 0:
                write0(VARINT_0);
                break;
            case 1:
                write0(VARINT_1);
                break;
            case 2:
                write0(VARINT_2);
                break;
            case 3:
                write0(VARINT_3);
                break;
            case 4:
                write0(VARINT_4);
                break;
            case 5:
                write0(VARINT_5);
                break;
            case 6:
                write0(VARINT_6);
                break;
            case 7:
                write0(VARINT_7);
                break;
            case 8:
                write0(VARINT_8);
                break;
            case 9:
                write0(VARINT_9);
                break;
            case 10:
                write0(VARINT_A);
                break;
            case 11:
                write0(VARINT_B);
                break;
            case 12:
                write0(VARINT_C);
                break;
            case 13:
                write0(VARINT_D);
                break;
            case 14:
                write0(VARINT_E);
                break;
            case 15:
                write0(VARINT_F);
                break;
            case 16:
                write0(VARINT_10);
                break;
            case 17:
                write0(VARINT_11);
                break;
            case 18:
                write0(VARINT_12);
                break;
            case 19:
                write0(VARINT_13);
                break;
            case 20:
                write0(VARINT_14);
                break;
            case 21:
                write0(VARINT_15);
                break;
            case 22:
                write0(VARINT_16);
                break;
            case 23:
                write0(VARINT_17);
                break;
            case 24:
                write0(VARINT_18);
                break;
            case 25:
                write0(VARINT_19);
                break;
            case 26:
                write0(VARINT_1A);
                break;
            case 27:
                write0(VARINT_1B);
                break;
            case 28:
                write0(VARINT_1C);
                break;
            case 29:
                write0(VARINT_1D);
                break;
            case 30:
                write0(VARINT_1E);
                break;
            case 31:
                write0(VARINT_1F);
                break;
            default:
                write0(VARINT8);
                write0(v);
        }
    }

    public void writeShort(short v) throws IOException {
        writeVarint32(v);
    }

    public void writeInt(int v) throws IOException {
        writeVarint32(v);
    }

    public void writeLong(long v) throws IOException {
        writeVarint64(v);
    }

    public void writeFloat(float v) throws IOException {
        writeVarint32(Float.floatToRawIntBits(v));
    }

    public void writeDouble(double v) throws IOException {
        writeVarint64(Double.doubleToRawLongBits(v));
    }

    /**
     * 将字符串拆分为字符数组后依次做运算
     */
    public void writeUTF(String v) throws IOException {//todo @pause 1.6
        if (v == null) { //空对象
            write0(OBJECT_NULL);
        } else {
            int len = v.length();
            if (len == 0) { //空字符串
                write0(OBJECT_DUMMY);
            } else {
                write0(OBJECT_BYTES);
                writeUInt(len);

                int off = 0, limit = mLimit - 3, size;
                char[] buf = mCharBuf;
                do {
                    size = Math.min(len - off, CHAR_BUF_SIZE);
                    v.getChars(off, off + size, buf, 0); // 从字符串中拷贝指定长度的字符到字符数组

                    for (int i = 0; i < size; i++) {
                        char c = buf[i];
                        if (mPosition > limit) { //todo @csy 此处的逻辑运算待了解
                            if (c < 0x80) {
                                write0((byte) c);
                            } else if (c < 0x800) {
                                write0((byte) (0xC0 | ((c >> 6) & 0x1F)));
                                write0((byte) (0x80 | (c & 0x3F)));
                            } else {
                                write0((byte) (0xE0 | ((c >> 12) & 0x0F)));
                                write0((byte) (0x80 | ((c >> 6) & 0x3F)));
                                write0((byte) (0x80 | (c & 0x3F)));
                            }
                        } else {
                            if (c < 0x80) {
                                mBuffer[mPosition++] = (byte) c;
                            } else if (c < 0x800) {
                                mBuffer[mPosition++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                                mBuffer[mPosition++] = (byte) (0x80 | (c & 0x3F));
                            } else {
                                mBuffer[mPosition++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                                mBuffer[mPosition++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                                mBuffer[mPosition++] = (byte) (0x80 | (c & 0x3F));
                            }
                        }
                    }
                    off += size;
                }
                while (off < len);
            }
        }
    }

    public void writeBytes(byte[] b) throws IOException {
        if (b == null)
            write0(OBJECT_NULL);
        else
            writeBytes(b, 0, b.length);
    }

    public void writeBytes(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            write0(OBJECT_DUMMY);
        } else {
            write0(OBJECT_BYTES);
            writeUInt(len);
            write0(b, off, len);
        }
    }

    /**
     * 此处为啥不扩容？若不扩容，之前的数据是否会被覆盖？或者取实现元素的大小作为下标值：
     * 处理流程：将缓存区满时，将缓存中数据写到输出流中，并将下标置为0，新加入的数据会覆盖之前的数据。
     *         当缓存区下次满时，又会写到输出流中，基础处理数据的添加
     */
    public void flushBuffer() throws IOException { //刷新缓冲区，把数据写到输出流中，并重置下标
        if (mPosition > 0) {
            mOutput.write(mBuffer, 0, mPosition);
            mPosition = 0;
        }
    }

    public void writeUInt(int v) throws IOException {
        byte tmp;
        while (true) {
            tmp = (byte) (v & 0x7f);
            if ((v >>>= 7) == 0) { //todo @csy 运算的含义
                write0((byte) (tmp | 0x80));
                return;
            } else {
                write0(tmp);
            }
        }
    }

    protected void write0(byte b) throws IOException { //单字节写入
        if (mPosition == mLimit) //若游标位置达到限制数，则重新将mPosition置为0
            flushBuffer();

        mBuffer[mPosition++] = b; //mPosition++ ，表达式值为mPosition，变量值+1
    }

    /**
     * 字节数组写入，使用字节数组拷贝
     * 1）判断是否有可写入的空间
     * 2）有可写入的空间，直接使用数组拷贝，并更新游标
     * 3）若无可写入的空间
     *   3.1）从原数组中取部分元素将缓冲区写满，并且刷新缓冲区
     *   3.2）更新原数组的游标和长度，判断是否在允许写入的范围内
     *     3.2.1）若在允许写入的范围内，则拷贝数组，更新游标
     *     3.2.2）若不在允许写入的范围内，表明当前的缓存区不够存储，直接写到输出流中
     */
    protected void write0(byte[] b, int off, int len) throws IOException {
        int rem = mLimit - mPosition; //可写入的大小
        if (rem > len) { //
            System.arraycopy(b, off, mBuffer, mPosition, len); //从原数组指定位置拷贝指定长度的元素到目标数组，并且指定目标数组的起始位置
            mPosition += len;
        } else {
            System.arraycopy(b, off, mBuffer, mPosition, rem);
            mPosition = mLimit;
            flushBuffer();

            off += rem;
            len -= rem;

            if (mLimit > len) {
                System.arraycopy(b, off, mBuffer, 0, len);
                mPosition = len;
            } else {
                mOutput.write(b, off, len);
            }
        }
    }

    private void writeVarint32(int v) throws IOException {
        switch (v) {
            case -15:
                write0(VARINT_NF);
                break;
            case -14:
                write0(VARINT_NE);
                break;
            case -13:
                write0(VARINT_ND);
                break;
            case -12:
                write0(VARINT_NC);
                break;
            case -11:
                write0(VARINT_NB);
                break;
            case -10:
                write0(VARINT_NA);
                break;
            case -9:
                write0(VARINT_N9);
                break;
            case -8:
                write0(VARINT_N8);
                break;
            case -7:
                write0(VARINT_N7);
                break;
            case -6:
                write0(VARINT_N6);
                break;
            case -5:
                write0(VARINT_N5);
                break;
            case -4:
                write0(VARINT_N4);
                break;
            case -3:
                write0(VARINT_N3);
                break;
            case -2:
                write0(VARINT_N2);
                break;
            case -1:
                write0(VARINT_N1);
                break;
            case 0:
                write0(VARINT_0);
                break;
            case 1:
                write0(VARINT_1);
                break;
            case 2:
                write0(VARINT_2);
                break;
            case 3:
                write0(VARINT_3);
                break;
            case 4:
                write0(VARINT_4);
                break;
            case 5:
                write0(VARINT_5);
                break;
            case 6:
                write0(VARINT_6);
                break;
            case 7:
                write0(VARINT_7);
                break;
            case 8:
                write0(VARINT_8);
                break;
            case 9:
                write0(VARINT_9);
                break;
            case 10:
                write0(VARINT_A);
                break;
            case 11:
                write0(VARINT_B);
                break;
            case 12:
                write0(VARINT_C);
                break;
            case 13:
                write0(VARINT_D);
                break;
            case 14:
                write0(VARINT_E);
                break;
            case 15:
                write0(VARINT_F);
                break;
            case 16:
                write0(VARINT_10);
                break;
            case 17:
                write0(VARINT_11);
                break;
            case 18:
                write0(VARINT_12);
                break;
            case 19:
                write0(VARINT_13);
                break;
            case 20:
                write0(VARINT_14);
                break;
            case 21:
                write0(VARINT_15);
                break;
            case 22:
                write0(VARINT_16);
                break;
            case 23:
                write0(VARINT_17);
                break;
            case 24:
                write0(VARINT_18);
                break;
            case 25:
                write0(VARINT_19);
                break;
            case 26:
                write0(VARINT_1A);
                break;
            case 27:
                write0(VARINT_1B);
                break;
            case 28:
                write0(VARINT_1C);
                break;
            case 29:
                write0(VARINT_1D);
                break;
            case 30:
                write0(VARINT_1E);
                break;
            case 31:
                write0(VARINT_1F);
                break;
            default: // 不是指定的特定值
                int t = v, ix = 0;
                byte[] b = mTemp;

                while (true) {
                    b[++ix] = (byte) (v & 0xff); //todo @csy 与0xff与运算，用途是？左移
                    if ((v >>>= 8) == 0)
                        break;
                }

                if (t > 0) {
                    // [ 0a e2 => 0a e2 00 ] [ 92 => 92 00 ]
                    if (b[ix] < 0) //小于0的置为0
                        b[++ix] = 0;
                } else {
                    // [ 01 ff ff ff => 01 ff ] [ e0 ff ff ff => e0 ]
                    while (b[ix] == (byte) 0xff && b[ix - 1] < 0)
                        ix--;
                }

                b[0] = (byte) (VARINT + ix - 1);
                write0(b, 0, ix + 1); //
        }
    }

    private void writeVarint64(long v) throws IOException {
        int i = (int) v;
        if (v == i) {
            writeVarint32(i);
        } else {
            long t = v;
            int ix = 0;
            byte[] b = mTemp;

            while (true) {
                b[++ix] = (byte) (v & 0xff);
                if ((v >>>= 8) == 0)
                    break;
            }

            if (t > 0) {
                // [ 0a e2 => 0a e2 00 ] [ 92 => 92 00 ]
                if (b[ix] < 0)
                    b[++ix] = 0;
            } else {
                // [ 01 ff ff ff => 01 ff ] [ e0 ff ff ff => e0 ]
                while (b[ix] == (byte) 0xff && b[ix - 1] < 0)
                    ix--;
            }

            b[0] = (byte) (VARINT + ix - 1);
            write0(b, 0, ix + 1);
        }
    }
}