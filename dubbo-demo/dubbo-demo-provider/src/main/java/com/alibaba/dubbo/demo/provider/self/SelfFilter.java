package com.alibaba.dubbo.demo.provider.self;

import com.alibaba.dubbo.rpc.*;

/**
 * @author : chensy
 * Date : 2020/5/22 上午11:58
 */
public class SelfFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        System.out.println("调用前");
        Result result = invoker.invoke(invocation);
        System.out.println("调用后");
        return result;
    }
}
