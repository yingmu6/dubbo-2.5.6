package com.alibaba.dubbo.remoting.transport.codec;

import com.alibaba.dubbo.common.io.Bytes;
import com.alibaba.dubbo.common.io.StreamUtils;
import com.alibaba.dubbo.common.io.UnsafeByteArrayInputStream;
import com.alibaba.dubbo.common.io.UnsafeByteArrayOutputStream;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.alibaba.dubbo.common.serialize.Serialization;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Codec;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.remoting.exchange.support.DefaultFuture;
import com.alibaba.dubbo.remoting.transport.CodecSupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:gang.lvg@taobao.com">kimi</a>
 */
final class DeprecatedExchangeCodec extends DeprecatedTelnetCodec implements Codec { //不推荐的类使用

    // header length.
    protected static final int HEADER_LENGTH = 16;
    // magic header.
    protected static final short MAGIC = (short) 0xdabb;
    protected static final byte MAGIC_HIGH = Bytes.short2bytes(MAGIC)[0];
    protected static final byte MAGIC_LOW = Bytes.short2bytes(MAGIC)[1];
    // message flag.
    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_TWOWAY = (byte) 0x40;
    protected static final byte FLAG_EVENT = (byte) 0x20;
    protected static final int SERIALIZATION_MASK = 0x1f;
    private static final Logger logger = LoggerFactory.getLogger(DeprecatedExchangeCodec.class);

    public Short getMagicCode() {
        return MAGIC;
    }

    /**
     * 用途：将消息对象进行编码，构造出字节数组并写入到字节输出流中
     * 思路：
     * 判断消息对象的类型
     * 1）若是特殊对象，如Request或Response，则按自定义的协议头进行编码
     * 2）若是其它的类型，则按字符串或普通对象的方式进行编码
     *
     * @param channel 通道：主要用于传入的数据大小进行检查
     * @param os  字节输出流：将消息对应的字节数组写入输出流
     * @param msg 消息对象，如Request、Response、String等
     * @throws IOException IO操作可能出现异常
     */
    public void encode(Channel channel, OutputStream os, Object msg) throws IOException {
        if (msg instanceof Request) {
            encodeRequest(channel, os, (Request) msg);
        } else if (msg instanceof Response) {
            encodeResponse(channel, os, (Response) msg);
        } else {
            super.encode(channel, os, msg);
        }
    }

    /**
     * 用途：从输入流中读取数据进行解码，构建相关的对象
     * 思路：
     * 1）先读取请求头的字节数组，读取16个字节
     * 2）解码请求体数据，并返回解码后的对象
     *
     * @param channel 通道：检查传输大小
     * @param is 输入流：从输入流中读取需要解码的数据
     * @return
     * @throws IOException
     */
    public Object decode(Channel channel, InputStream is) throws IOException {
        int readable = is.available(); //输入流中可读取的字节数
        byte[] header = new byte[Math.min(readable, HEADER_LENGTH)]; //取最小数，可能请求头数据不完整
        is.read(header);
        return decode(channel, is, readable, header);
    }

    /**
     *
     * @param channel
     * @param is
     * @param readable
     * @param header
     * @return
     * @throws IOException
     */
    protected Object decode(Channel channel, InputStream is, int readable, byte[] header) throws IOException {
        // check magic number.
        if (readable > 0 && header[0] != MAGIC_HIGH
                || readable > 1 && header[1] != MAGIC_LOW) { //若没有魔法数，则表明不是Request、Response，是普通的对象
            int length = header.length;
            if (header.length < readable) {
                header = Bytes.copyOf(header, readable); //todo 10/21 此处header会补全？
                is.read(header, length, readable - length);
            }
            for (int i = 1; i < header.length - 1; i++) {
                if (header[i] == MAGIC_HIGH && header[i + 1] == MAGIC_LOW) {  //todo 10/21 逻辑待理解
                    UnsafeByteArrayInputStream bis = ((UnsafeByteArrayInputStream) is);
                    bis.position(bis.position() - header.length + i);
                    header = Bytes.copyOf(header, i);
                    break;
                }
            }
            return super.decode(channel, is, readable, header);
        }
        // check length.
        if (readable < HEADER_LENGTH) { //字节数未达到协议头的长度时，不进行后续操作
            return NEED_MORE_INPUT;
        }

        // get data length.
        int len = Bytes.bytes2int(header, 12);
        checkPayload(channel, len);

        int tt = len + HEADER_LENGTH;
        if (readable < tt) {
            return NEED_MORE_INPUT;
        }

        // limit input stream.
        if (readable != tt)
            is = StreamUtils.limitedInputStream(is, len);

        try {
            return decodeBody(channel, is, header);
        } finally {
            if (is.available() > 0) {
                try {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Skip input stream " + is.available());
                    }
                    StreamUtils.skipUnusedStream(is);
                } catch (IOException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 用途：解码请求体或响应体
     * 思路：
     *
     * @param channel
     * @param is
     * @param header
     * @return
     * @throws IOException
     */
    protected Object decodeBody(Channel channel, InputStream is, byte[] header) throws IOException {
        byte flag = header[2], proto = (byte) (flag & SERIALIZATION_MASK);
        Serialization s = CodecSupport.getSerialization(channel.getUrl(), proto);
        ObjectInput in = s.deserialize(channel.getUrl(), is);
        // get request id.
        long id = Bytes.bytes2long(header, 4);
        if ((flag & FLAG_REQUEST) == 0) {
            // decode response.（解码响应数据）
            Response res = new Response(id);
            if ((flag & FLAG_EVENT) != 0) {
                res.setEvent(Response.HEARTBEAT_EVENT);
            }
            // get status.
            byte status = header[3];
            res.setStatus(status);
            if (status == Response.OK) {
                try {
                    Object data;
                    if (res.isHeartbeat()) {
                        data = decodeHeartbeatData(channel, in);
                    } else if (res.isEvent()) {
                        data = decodeEventData(channel, in);
                    } else {
                        data = decodeResponseData(channel, in, getRequestData(id));
                    }
                    res.setResult(data);
                } catch (Throwable t) {
                    res.setStatus(Response.CLIENT_ERROR);
                    res.setErrorMessage(StringUtils.toString(t));
                }
            } else {
                res.setErrorMessage(in.readUTF());
            }
            return res;
        } else {
            // decode request.（解码请求数据）
            Request req = new Request(id);
            req.setVersion("2.0.0");
            req.setTwoWay((flag & FLAG_TWOWAY) != 0);
            if ((flag & FLAG_EVENT) != 0) {
                req.setEvent(Request.HEARTBEAT_EVENT);
            }
            try {
                Object data;
                if (req.isHeartbeat()) {
                    data = decodeHeartbeatData(channel, in);
                } else if (req.isEvent()) {
                    data = decodeEventData(channel, in);
                } else {
                    data = decodeRequestData(channel, in);
                }
                req.setData(data);
            } catch (Throwable t) {
                // bad request
                req.setBroken(true);
                req.setData(t);
            }
            return req;
        }
    }

    protected Object getRequestData(long id) {
        DefaultFuture future = DefaultFuture.getFuture(id);
        if (future == null)
            return null;
        Request req = future.getRequest();
        if (req == null)
            return null;
        return req.getData();
    }

    /**
     * 用途：编码请求对象，并将构建的字节数组写入输出流
     * 思路：
     * 1）构建请求头的字节数组，设置请求相关信息
     * 2）编码请求体数据
     * 3）将请求头、请求体的字节数组都写入输出流
     *
     * @param channel 通道：检查传输的数据大小
     * @param os 输出流
     * @param req 请求对象
     * @throws IOException
     */
    protected void encodeRequest(Channel channel, OutputStream os, Request req) throws IOException {
        Serialization serialization = CodecSupport.getSerialization(channel.getUrl());
        // header.(请求头字节数组)
        byte[] header = new byte[HEADER_LENGTH];
        // set magic number.(设置魔法数)
        Bytes.short2bytes(MAGIC, header); //把魔法数转换为两个字节，并设置到数组中

        // set request and serialization flag.
        header[2] = (byte) (FLAG_REQUEST | serialization.getContentTypeId());

        if (req.isTwoWay()) header[2] |= FLAG_TWOWAY; //todo 10/21 异或运算的结果是啥？
        if (req.isEvent()) header[2] |= FLAG_EVENT;

        // set request id.
        Bytes.long2bytes(req.getId(), header, 4);

        // encode request data.
        UnsafeByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(1024);
        ObjectOutput out = serialization.serialize(channel.getUrl(), bos);
        if (req.isEvent()) { //todo 10/21 数据是怎么扩容的？
            encodeEventData(channel, out, req.getData());
        } else {
            encodeRequestData(channel, out, req.getData());
        }
        out.flushBuffer();
        bos.flush();
        bos.close();
        byte[] data = bos.toByteArray();
        checkPayload(channel, data.length); //todo 10/21 是否包含请求头的16字节？
        Bytes.int2bytes(data.length, header, 12);

        // write
        os.write(header); // write header.
        os.write(data); // write data.
    }

    /**
     * 用途：编码响应对象，并将构建的字节数组写入输出流
     * 思路：
     * 1）构建响应头的字节数组，设置请求相关信息
     * 2）编码响应体数据
     * 3）将响应头、响应体的字节数组都写入输出流
     * 4）异常处理：发送失败信息给Consumer，否则Consumer只能等超时了
     *
     * @param channel 通道：检查传输的数据大小
     * @param os 输出流
     * @param res 响应对象
     * @throws IOException
     */
    protected void encodeResponse(Channel channel, OutputStream os, Response res) throws IOException { //编码响应内容（将对象序列化为字节数组）
        try {
            Serialization serialization = CodecSupport.getSerialization(channel.getUrl());
            // header.
            byte[] header = new byte[HEADER_LENGTH];
            // set magic number.
            Bytes.short2bytes(MAGIC, header);
            // set request and serialization flag.
            header[2] = serialization.getContentTypeId();
            if (res.isHeartbeat()) header[2] |= FLAG_EVENT;
            // set response status.
            byte status = res.getStatus();
            header[3] = status;
            // set request id.
            Bytes.long2bytes(res.getId(), header, 4);

            UnsafeByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(1024);
            ObjectOutput out = serialization.serialize(channel.getUrl(), bos);
            // encode response data or error message.
            if (status == Response.OK) {
                if (res.isHeartbeat()) {
                    encodeHeartbeatData(channel, out, res.getResult());
                } else {
                    encodeResponseData(channel, out, res.getResult());
                }
            } else out.writeUTF(res.getErrorMessage());
            out.flushBuffer();
            bos.flush();
            bos.close();

            byte[] data = bos.toByteArray();
            checkPayload(channel, data.length);
            Bytes.int2bytes(data.length, header, 12);
            // write
            os.write(header); // write header.
            os.write(data); // write data.
        } catch (Throwable t) {
            // 发送失败信息给Consumer，否则Consumer只能等超时了
            if (!res.isEvent() && res.getStatus() != Response.BAD_RESPONSE) {
                try {
                    // FIXME 在Codec中打印出错日志？在IoHanndler的caught中统一处理？
                    logger.warn("Fail to encode response: " + res + ", send bad_response info instead, cause: " + t.getMessage(), t);

                    Response r = new Response(res.getId(), res.getVersion());
                    r.setStatus(Response.BAD_RESPONSE);
                    r.setErrorMessage("Failed to send response: " + res + ", cause: " + StringUtils.toString(t));
                    channel.send(r); //todo 10/21 底层是怎样发送数据的？Netty吗

                    return;
                } catch (RemotingException e) {
                    logger.warn("Failed to send bad_response info back: " + res + ", cause: " + e.getMessage(), e);
                }
            }

            // 重新抛出收到的异常
            if (t instanceof IOException) {
                throw (IOException) t;
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new RuntimeException(t.getMessage(), t);
            }
        }
    }

    protected Object decodeData(ObjectInput in) throws IOException {
        return decodeRequestData(in);
    }

    @Deprecated
    protected Object decodeHeartbeatData(ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    protected Object decodeRequestData(ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    protected Object decodeResponseData(ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    protected void encodeData(ObjectOutput out, Object data) throws IOException {
        encodeRequestData(out, data);
    }

    private void encodeEventData(ObjectOutput out, Object data) throws IOException {
        out.writeObject(data);
    }

    @Deprecated
    protected void encodeHeartbeatData(ObjectOutput out, Object data) throws IOException {
        encodeEventData(out, data);
    }

    protected void encodeRequestData(ObjectOutput out, Object data) throws IOException {
        out.writeObject(data);
    }

    protected void encodeResponseData(ObjectOutput out, Object data) throws IOException {
        out.writeObject(data);
    }

    protected Object decodeData(Channel channel, ObjectInput in) throws IOException {
        return decodeRequestData(channel, in);
    }

    protected Object decodeEventData(Channel channel, ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    @Deprecated
    protected Object decodeHeartbeatData(Channel channel, ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    protected Object decodeRequestData(Channel channel, ObjectInput in) throws IOException {
        return decodeRequestData(in);
    }

    protected Object decodeResponseData(Channel channel, ObjectInput in) throws IOException {
        return decodeResponseData(in);
    }

    protected Object decodeResponseData(Channel channel, ObjectInput in, Object requestData) throws IOException {
        return decodeResponseData(channel, in);
    }

    protected void encodeData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeRequestData(channel, out, data);
    }

    private void encodeEventData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeEventData(out, data);
    }

    @Deprecated
    protected void encodeHeartbeatData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeHeartbeatData(out, data);
    }

    protected void encodeRequestData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeRequestData(out, data);
    }

    protected void encodeResponseData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeResponseData(out, data);
    }

}
