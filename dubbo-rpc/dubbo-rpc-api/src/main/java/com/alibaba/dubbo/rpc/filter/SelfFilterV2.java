package com.alibaba.dubbo.rpc.filter;

import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.rpc.*;

/**
 * @author chensy
 * @date 2019-09-14 15:39
 */
//@Activate(group = Constants.PROVIDER)  //自动加载Filter, 加上注解就不需要在XML中指定
public class SelfFilterV2 implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        System.out.println("自定义SelfFilter V2 ");
//        return invoker.invoke(invocation);
        return null;
    }
}
