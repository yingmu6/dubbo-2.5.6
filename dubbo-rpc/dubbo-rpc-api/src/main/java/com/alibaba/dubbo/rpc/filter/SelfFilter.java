package com.alibaba.dubbo.rpc.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;

/**
 * @author chensy
 * @date 2019-09-14 15:39
 */
@Activate(group = Constants.PROVIDER)  //自动加载Filter, 加上注解就不需要在XML中指定
public class SelfFilter implements Filter { //原生的Filter
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        System.out.println("自定义Filter");
        return invoker.invoke(invocation);
    }
}
