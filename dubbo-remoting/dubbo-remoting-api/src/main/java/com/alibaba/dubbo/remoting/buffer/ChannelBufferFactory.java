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
//提供多个工厂方法构造
public interface ChannelBufferFactory {// read finish
    //模仿Netty创建ChannelBuffer
    //HeapChannelBufferFactory分配堆内Buffer
    //DirectChannelBufferFactory直接封装了ByteBuffer的 directBuffer,直接buffer，没有中间缓冲区的
    //NettyBackedChannelBufferFactory分配动态太小的buffer

    //构造ChannelBuffer
    ChannelBuffer getBuffer(int capacity);

    ChannelBuffer getBuffer(byte[] array, int offset, int length);

    ChannelBuffer getBuffer(ByteBuffer nioBuffer);

}
