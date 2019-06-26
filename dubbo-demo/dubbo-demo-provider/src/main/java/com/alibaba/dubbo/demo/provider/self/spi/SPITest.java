package com.alibaba.dubbo.demo.provider.self.spi;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.remoting.Transporter;
import com.alibaba.dubbo.remoting.TransporterSelf;
import com.alibaba.dubbo.remoting.transport.mina.MinaTransporterSelf;

/**
 * @author chensy
 * @date 2019-06-11 20:00
 */
public class SPITest {
    public static void main(String[] args) {
//        System.out.println(ExtensionLoader.getExtensionLoader(Transporter.class).getExtension("netty").getClass());
//        System.out.println(ExtensionLoader.getExtensionLoader(Transporter.class).getAdaptiveExtension().getClass());

        //ExtensionLoader.getExtensionLoader(TransporterSelf.class).getAdaptiveExtension().getClass();

        // 指定扩展名
        System.out.println(ExtensionLoader.getExtensionLoader(TransporterSelf.class).getExtension("nettySelf").getClass());

        //ExtensionLoader 没有公有的构造函数，调用getExtensionLoader获取扩展实例

        // 不指定扩展名，运行时动态从url中获取 （生成自适应代码，然后获取自适应扩展实例）
        ExtensionLoader extensionLoader = ExtensionLoader.getExtensionLoader(TransporterSelf.class);
        System.out.println(extensionLoader.getAdaptiveExtension().getClass());

//        System.out.println(ExtensionLoader.getExtensionLoader(TransporterSelf.class).getExtension("minaSelf").getClass());
//        System.out.println(ExtensionLoader.getExtensionLoader(TransporterSelf.class).getAdaptiveExtension().getClass());
    }

    /**
     * 在获取netty扩展时，会先获取spi、spring的扩展
     *
     * 会遍历指定目标下的DUBBO_INTERNAL_DIRECTORY、DUBBO_DIRECTORY、SERVICES_DIRECTORY
     * 所有与接口名相同的 文件com.alibaba.dubbo.remoting.Transporter，然后存在缓存Map<String, Class<?>>
     *     如"netty" -> "class com.alibaba.dubbo.remoting.transport.netty.NettyTransporter"
     *       "mina" -> "class com.alibaba.dubbo.remoting.transport.mina.MinaTransporter"
     *       "netty4" -> "class com.alibaba.dubbo.remoting.transport.netty4.NettyTransporter"
     *       "grizzly" -> "class com.alibaba.dubbo.remoting.transport.grizzly.GrizzlyTransporter"
     */
}

/**
 * Dubbo源码学习--SPI实现@SPI和@Adaptive
 * https://blog.csdn.net/qq924862077/article/details/77510121
 *
 * Protocol refprotocol = ExtensionLoader.getExtensionLoader(Protocol.class).getDefaultExtension();
 * Protocol refprotocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("dubbo");
 *
 * 这样获取的都是实现类DubboProtocol，实现类的对应关系是配置在文件中的，从ExtensionLoader会解析文件将数据添加到一个Map中，
 * 这样可以直接通过“dubbo”来获取到实现类DubboProtocol。这种实现还是比较简单，
 * 但有一个问题就是需要在代码中写死使用哪个实现类，这个就和SPI的初衷有所差别了，因此Dubbo提供了一个另外一个注解@@Adaptive。
 *
 *
 * Dubbo通过注解@Adaptive作为标记实现了一个适配器类，并且这个类是动态生成的，因此在Dubbo的源码中是看不到代码的，
 * 但是我们还是可以看到其实现方式的。Dubbo提供一个动态的适配器类的原因就是可以通过配置文件来动态的使用想要的接口实现类，
 * 并且不用改变任何接口的代码，简单来说其也是通过代理来实现的。
 *
 *
 * createAdaptiveExtensionClass中首先会获取代理类的源码，然后在编译源码获取一个Class
 * Dubbo实现一个适配器类的关键在createAdaptiveExtensionClassCode，会生成一个类的代理适配器类的源码
 *
 */
