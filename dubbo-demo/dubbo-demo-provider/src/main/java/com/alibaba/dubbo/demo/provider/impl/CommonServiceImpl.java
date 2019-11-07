package com.alibaba.dubbo.demo.provider.impl;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.demo.CommonService;
import com.alibaba.dubbo.rpc.RpcContext;

/**
 * @author chensy
 * @date 2019-11-07 17:55
 */
public class CommonServiceImpl implements CommonService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    // 上下文信息处理
    @Override
    public void dealRpcContext() {
        RpcContext rpcContext = RpcContext.getContext();
        logger.info("上下文：" + rpcContext.getUrl());
    }
}
