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

package com.alibaba.dubbo.remoting.transport.codec;

import com.alibaba.dubbo.common.io.UnsafeByteArrayInputStream;
import com.alibaba.dubbo.common.io.UnsafeByteArrayOutputStream;
import com.alibaba.dubbo.common.utils.Assert;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Codec;
import com.alibaba.dubbo.remoting.Codec2;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffer;

import java.io.IOException;

/**
 *  1）此处适配器的用途？ 采取对象适配器方式 2）适配模式设计模式学习
 * 适配器模式：进行接口转化，将一个接口转换为另一个接口形式。适配器分为对象适配器、类适配器，是一个中间者的角色
 * 默认适配器，实现了接口中所有方法，子类可以选择实现执行的方法
 * 类适配器：采用继承被适配对象方式，进行接口适配
 * 对象适配器：采取组合被适配对象的方式，进行接口适配
 * https://www.cnblogs.com/java-my-life/archive/2012/04/13/2442795.html
 * https://design-patterns.readthedocs.io/zh_CN/latest/structural_patterns/adapter.html
 */
public class CodecAdapter implements Codec2 { //todo 11/18 编码适配器了解

    private Codec codec;

    public CodecAdapter(Codec codec) {
        Assert.notNull(codec, "codec == null");
        this.codec = codec;
    }

    public void encode(Channel channel, ChannelBuffer buffer, Object message)
            throws IOException {
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream(1024);
        codec.encode(channel, os, message);
        buffer.writeBytes(os.toByteArray());
    }

    public Object decode(Channel channel, ChannelBuffer buffer) throws IOException {
        byte[] bytes = new byte[buffer.readableBytes()];
        int savedReaderIndex = buffer.readerIndex();
        buffer.readBytes(bytes);
        UnsafeByteArrayInputStream is = new UnsafeByteArrayInputStream(bytes);
        Object result = codec.decode(channel, is);
        buffer.readerIndex(savedReaderIndex + is.position());
        return result == Codec.NEED_MORE_INPUT ? DecodeResult.NEED_MORE_INPUT : result;
    }

    public Codec getCodec() {
        return codec;
    }
}

