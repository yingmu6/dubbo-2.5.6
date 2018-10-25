package com.alibaba.dubbo.remoting.buffer;

/**
 * @author chensy
 * @date 2018/10/14
 */
public class IndexOutExceptionTest {
    public static void main(String[] args) {
        //test1();
        test2();
    }

    public static void test1(){
        //会抛出index越界异常，因为readerIndex，writerIndex都会校验下标的正确性
        //初始化时readerIndex、writerIndex都为0，如果先设置readerIndex=2>writerIndex
        //所以要先设置writerIndex，后设置readerIndex
        ChannelBuffer buf = ChannelBuffers.buffer(8);
        buf.readerIndex(2);
        buf.writerIndex(4);
    }

    public static void test2(){
        //此处不会越界异常，因为使用HeapChannelBuffer创建对象时，把writerIndex置为byte.length
        ChannelBuffer buf = ChannelBuffers.wrappedBuffer(new byte[8]);
        buf.readerIndex(2);
        buf.writerIndex(4);
    }

}
