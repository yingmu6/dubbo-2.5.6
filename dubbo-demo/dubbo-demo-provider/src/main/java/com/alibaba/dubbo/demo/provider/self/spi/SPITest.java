package com.alibaba.dubbo.demo.provider.self.spi;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.remoting.Transporter;

/**
 * @author chensy
 * @date 2019-06-11 20:00
 */
public class SPITest {
    public static void main(String[] args) {
        System.out.println(ExtensionLoader.getExtensionLoader(Transporter.class).getExtension("netty").getClass());
    }
}
