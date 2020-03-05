package com.alibaba.dubbo.rpc.benchmark;


/**
 * todo @csy-h3 Comment of HelloService
 *
 * @author tony.chenl
 */
public class DemoServiceImpl implements DemoService {
    ResponseObject responseObject = new ResponseObject(100);

    public Object sendRequest(Object request) {
        return request;
    }
}
