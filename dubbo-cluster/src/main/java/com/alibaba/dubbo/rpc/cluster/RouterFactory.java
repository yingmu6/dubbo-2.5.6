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
package com.alibaba.dubbo.rpc.cluster;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;
import com.alibaba.dubbo.rpc.Invocation;

/**
 * RouterFactory. (SPI, Singleton, ThreadSafe)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Routing">Routing</a>
 *
 * @author chao.liuc
 * @see com.alibaba.dubbo.rpc.cluster.Cluster#join(Directory)
 * @see com.alibaba.dubbo.rpc.cluster.Directory#list(Invocation)
 */
@SPI
public interface RouterFactory {/**@c 路由工厂 */

    /**
     * Create router.
     *
     * @param url
     * @return router
     */
    @Adaptive("protocol")
    Router getRouter(URL url);

}

/**
 * package com.alibaba.dubbo.rpc.cluster;
 * import com.alibaba.dubbo.common.extension.ExtensionLoader;
 * public class RouterFactory$Adaptive implements com.alibaba.dubbo.rpc.cluster.RouterFactory {
 * public com.alibaba.dubbo.rpc.cluster.Router getRouter(com.alibaba.dubbo.common.URL arg0 )  {
 *       if(arg0 == null) throw new IllegalArgumentException("url == null");
 *       com.alibaba.dubbo.common.URL url = arg0;
 *       String extName = null;
 *       extName = (url.getProtocol() == null || url.getProtocol() == "") ? "" : url.getProtocol();
 *       if (extName == null || extName.equals(""))
 *          throw new IllegalStateException ("com.alibaba.dubbo.rpc.cluster.RouterFactory 的扩展名为空 " );
 *       com.alibaba.dubbo.rpc.cluster.RouterFactory extension =
 *       ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.cluster.RouterFactory.class ).getExtension(extName);
 *  return extension.getRouter(arg0);}}
 */