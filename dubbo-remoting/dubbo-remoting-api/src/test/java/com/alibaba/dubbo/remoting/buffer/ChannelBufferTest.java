package com.alibaba.dubbo.remoting.buffer;

/**
 * @author chensy
 * @date 2018/10/16 上午8:56
 */
public class ChannelBufferTest {
    public static void main(String[] args) {
       // test1();
        test2();
    }

    private static void test1(){
        byte []arr = new byte[]{'a','b','c'};
        ChannelBuffer channelBuffer = ChannelBuffers.wrappedBuffer(arr);
        System.out.println(channelBuffer.capacity());
    }

    private static void test2(){

        /**
         * 没有动态扩容
         * java.lang.ArrayIndexOutOfBoundsException
         *
        ChannelBuffer channelBuffer = ChannelBuffers.buffer(2);
        byte []arr = new byte[]{'a','b','c'};
        channelBuffer.writeBytes(arr);
         **/
        ChannelBuffer channelBuffer = ChannelBuffers.dynamicBuffer(2);
        byte []arr = new byte[]{'a','b','c','d','e'};
        channelBuffer.writeBytes(arr);
        System.out.println(channelBuffer.capacity());
    }
}
