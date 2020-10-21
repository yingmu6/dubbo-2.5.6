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
package com.alibaba.dubbo.common.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Stream utils.
 *
 * @author qian.lei
 * @author ding.lid
 */

public class StreamUtils {
    private StreamUtils() {
    }

    /**
     * 用途：构建可以限制数据大小的输入流
     * 思路：
     * 1）通过匿名类的方式构建抽象类InputStream的实现类
     * 2）取限制数limit以及输入流中可读数据作比较，取最小值进行后续处理
     *
     *
     * @param is
     * @param limit
     * @return
     * @throws IOException
     */
    public static InputStream limitedInputStream(final InputStream is, final int limit) throws IOException {
        return new InputStream() {
            private int mPosition = 0, mMark = 0, mLimit = Math.min(limit, is.available());

            /**
             * 从输入流中读取单个字节
             * 校验位置下标是否正确，若正确则读取一个数值
             */
            public int read() throws IOException {
                if (mPosition < mLimit) {
                    mPosition++;
                    return is.read();
                }
                return -1;
            }

            /**
             * 从输入流中读取数据写到字节数组中，并返回读取到的数据个数
             */
            public int read(byte b[], int off, int len) throws IOException {
                if (b == null)
                    throw new NullPointerException();

                if (off < 0 || len < 0 || len > b.length - off)
                    throw new IndexOutOfBoundsException();

                if (mPosition >= mLimit)
                    return -1;

                if (mPosition + len > mLimit)
                    len = mLimit - mPosition;

                if (len <= 0)
                    return 0;

                is.read(b, off, len);
                mPosition += len;
                return len;
            }

            public long skip(long len) throws IOException {
                if (mPosition + len > mLimit)
                    len = mLimit - mPosition;

                if (len <= 0)
                    return 0;

                is.skip(len);
                mPosition += len;
                return len;
            }

            public int available() {
                return mLimit - mPosition;
            }

            public boolean markSupported() {
                return is.markSupported();
            }

            public void mark(int readlimit) {
                is.mark(readlimit);
                mMark = mPosition;
            }

            public void reset() throws IOException {
                is.reset();
                mPosition = mMark;
            }

            public void close() throws IOException {
            }
        };
    }

    public static InputStream markSupportedInputStream(final InputStream is, final int markBufferSize) {
        if (is.markSupported()) {
            return is;
        }

        return new InputStream() {
            byte[] mMarkBuffer;

            boolean mInMarked = false;
            boolean mInReset = false;
            boolean mDry = false;
            private int mPosition = 0;
            private int mCount = 0; //todo 10/21 标志待了解

            /**
             * todo 10/21 用途待了解
             * @return
             * @throws IOException
             */
            @Override
            public int read() throws IOException {
                if (!mInMarked) {
                    return is.read();
                } else {
                    if (mPosition < mCount) {
                        byte b = mMarkBuffer[mPosition++];
                        return b & 0xFF;
                    }

                    if (!mInReset) {
                        if (mDry) return -1;

                        if (null == mMarkBuffer) {
                            mMarkBuffer = new byte[markBufferSize];
                        }
                        if (mPosition >= markBufferSize) {
                            throw new IOException("Mark buffer is full!");
                        }

                        int read = is.read();
                        if (-1 == read) {
                            mDry = true;
                            return -1;
                        }

                        mMarkBuffer[mPosition++] = (byte) read;
                        mCount++;

                        return read;
                    } else {
                        // mark buffer is used, exit mark status!
                        mInMarked = false;
                        mInReset = false;
                        mPosition = 0;
                        mCount = 0;

                        return is.read();
                    }
                }
            }

            /**
             * NOTE: the <code>readlimit</code> argument for this class
             *  has no meaning.
             */
            @Override
            public synchronized void mark(int readlimit) {
                mInMarked = true;
                mInReset = false;

                // mark buffer is not empty
                int count = mCount - mPosition;
                if (count > 0) {
                    System.arraycopy(mMarkBuffer, mPosition, mMarkBuffer, 0, count);
                    mCount = count;
                    mPosition = 0;
                }
            }

            @Override
            public synchronized void reset() throws IOException {
                if (!mInMarked) {
                    throw new IOException("should mark befor reset!");
                }

                mInReset = true;
                mPosition = 0;
            }

            @Override
            public boolean markSupported() {
                return true;
            }

            @Override
            public int available() throws IOException {
                int available = is.available();

                if (mInMarked && mInReset) available += mCount - mPosition;

                return available;
            }
        };
    }

    public static InputStream markSupportedInputStream(final InputStream is) {
        return markSupportedInputStream(is, 1024);
    }

    public static void skipUnusedStream(InputStream is) throws IOException {
        if (is.available() > 0) {
            is.skip(is.available());
        }
    }
}