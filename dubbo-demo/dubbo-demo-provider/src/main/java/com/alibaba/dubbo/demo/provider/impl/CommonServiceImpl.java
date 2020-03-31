package com.alibaba.dubbo.demo.provider.impl;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.demo.CommonService;

/**
 * @author chensy
 * @date 2019-11-07 17:55
 */
public class CommonServiceImpl implements CommonService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private String source;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    // 上下文信息处理
    @Override
    public String sayHello() {
        // logger.info("你好 Common");
        return "来自：" + getSource() + " ,hello 你好";
    }
}
