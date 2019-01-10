package com.alibaba.dubbo.governance.biz.common.i18n;

public interface MessageResourceService {/**@c */

    public String get(String key, Object... args);

    public String getMessage(String key, Object... args);

}
