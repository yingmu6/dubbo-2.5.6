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
package com.alibaba.dubbo.remoting.transport.mina;

import com.alibaba.dubbo.common.Node;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.*;
import com.alibaba.dubbo.rpc.Invocation;

/**
 * @author ding.lid
 */
public class MinaTransporterSelf implements TransporterSelf {

    public static final String NAME = "mina";

    public Server bind(URL url, ChannelHandler handler) throws RemotingException {
        return new MinaServer(url, handler);
    }

    public Client connect(URL url, ChannelHandler handler, Invocation invocation) throws RemotingException {
        return new MinaClient(url, handler);
    }

    @Override
    public Server testUrl(Node node) {
        return null;
    }

}