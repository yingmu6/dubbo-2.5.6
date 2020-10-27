package com.alibaba.dubbo.rpc.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class ProtocolUtils {

    private ProtocolUtils() {
    }

    //serviceKey的值待测试？ 解：将port、path、version、group拼接（服务的唯一标识key）
    public static String serviceKey(URL url) {// read finish
        return serviceKey(url.getPort(), url.getPath(), url.getParameter(Constants.VERSION_KEY),
                url.getParameter(Constants.GROUP_KEY));
    }

    //拼接服务的key (服务的key，会存入Map<String, Exporter<?>> exporterMap，根据可以找出暴露的服务)
    public static String serviceKey(int port, String serviceName, String serviceVersion, String serviceGroup) {
        StringBuilder buf = new StringBuilder();
        // ServiceKey格式  serviceGroup/serviceName:serviceVersion:port
        if (serviceGroup != null && serviceGroup.length() > 0) {
            buf.append(serviceGroup);
            buf.append("/");
        }
        buf.append(serviceName);//接口名
        if (serviceVersion != null && serviceVersion.length() > 0 && !"0.0.0".equals(serviceVersion)) {
            buf.append(":");
            buf.append(serviceVersion);
        }
        buf.append(":");
        buf.append(port);
        return buf.toString();
    }

    /**
     * 是否是泛化类型
     * history-new 返回类型不是"*"，为啥有这些"nativejava"
     */
    public static boolean isGeneric(String generic) {
        return generic != null
                && !"".equals(generic) //泛化类型
                && (Constants.GENERIC_SERIALIZATION_DEFAULT.equalsIgnoreCase(generic)  /* 正常的泛化调用 */ //默认的值
                || Constants.GENERIC_SERIALIZATION_NATIVE_JAVA.equalsIgnoreCase(generic) /* 支持java序列化的流式泛化调用 */  //java原生的泛化
                || Constants.GENERIC_SERIALIZATION_BEAN.equalsIgnoreCase(generic)); //序列化为JavaBeanDescriptor
    }

    public static boolean isDefaultGenericSerialization(String generic) {
        return isGeneric(generic)
                && Constants.GENERIC_SERIALIZATION_DEFAULT.equalsIgnoreCase(generic);
    }

    public static boolean isJavaGenericSerialization(String generic) {
        return isGeneric(generic)
                && Constants.GENERIC_SERIALIZATION_NATIVE_JAVA.equalsIgnoreCase(generic);
    }

    public static boolean isBeanGenericSerialization(String generic) {
        return isGeneric(generic) && Constants.GENERIC_SERIALIZATION_BEAN.equals(generic);
    }
}
