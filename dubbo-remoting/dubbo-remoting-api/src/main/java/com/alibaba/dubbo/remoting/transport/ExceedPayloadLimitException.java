package com.alibaba.dubbo.remoting.transport;

import java.io.IOException;

public class ExceedPayloadLimitException extends IOException { //在I/O操作时，超过负载限制异常
    private static final long serialVersionUID = -1112322085391551410L;

    public ExceedPayloadLimitException(String message) {
        super(message);
    }
}
