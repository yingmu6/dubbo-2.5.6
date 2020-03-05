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
package com.alibaba.dubbo.rpc.protocol.dubbo.telnet;

import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.json.JSON;
import com.alibaba.dubbo.common.utils.PojoUtils;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;
import com.alibaba.dubbo.remoting.telnet.support.Help;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * InvokeTelnetHandler
 *
 * @author william.liangf
 */
@Activate
@Help(parameter = "[service.]method(args)", summary = "Invoke the service method.", detail = "Invoke the service method.")
public class InvokeTelnetHandler implements TelnetHandler {

    /**
     * 比较每个接口中 方法以及参数是否相同
     */
    private static Method findMethod(Exporter<?> exporter, String method, List<Object> args) {
        Invoker<?> invoker = exporter.getInvoker();
        Method[] methods = invoker.getInterface().getMethods();
        for (Method m : methods) { //查找方法名相同，且参数类型相同的方法
            if (m.getName().equals(method) && isMatch(m.getParameterTypes(), args)) {
                return m;
            }
        }
        return null;
    }

    /**
     * 将方法的参数类型与参数值比较，看值与类型是否对应
     * @param types 暴露接口方法的参数类型列表
     * @param args 命令传入方法的参数值列表
     * @return
     */
    private static boolean isMatch(Class<?>[] types, List<Object> args) {
        if (types.length != args.size()) {
            return false;
        }
        //此处方法待调试：已调试
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            Object arg = args.get(i);
            if (ReflectUtils.isPrimitive(arg.getClass())) { //若参数值是基本类型，则看暴露接口中的参数是否是基本类型
                if (!ReflectUtils.isPrimitive(type)) {
                    return false;
                }
            } else if (arg instanceof Map) { //参数值的类型是map
                String name = (String) ((Map<?, ?>) arg).get("class"); //判断Map参数中是否包含class参数
                Class<?> cls = arg.getClass();
                if (name != null && name.length() > 0) {
                    cls = ReflectUtils.forName(name); //todo @chenSy 此处怎么传入才正确？
                }
                if (!type.isAssignableFrom(cls)) {
                    return false;
                }
            } else if (arg instanceof Collection) { //参数值的类型是集合
                if (!type.isArray() && !type.isAssignableFrom(arg.getClass())) {
                    return false;
                }
            } else {  //参数值的类型是对象
                if (!type.isAssignableFrom(arg.getClass())) {
                    return false;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    //此方法比较模糊，用途？ telnet命令中的invoke 解析
    /**
     * todo @chenSy telnet命令是怎样解析的？待调试， 此处的channel是怎样构造的
     * invoke指令逻辑：
     * 1）从telnet中接收到invoke命令
     * 2）解析输入的内容，构建服务、方法、参数
     * 3）执行invoke调用
     *
     * invoke CommonServiceV2.sayMulHelloV2("zh", {"key":"232"}, ["22","er"])
     * 传入的格式：对象以及map是{"key":"value"}格式，list或数组是 ["aa","bb"]格式
     *
     *
     */
    public String telnet(Channel channel, String message) { // 调用invoke com.alibaba.dubbo.demo.CommonService.sayHello()，会进行拆分，invoke是命令，com.alibaba.dubbo.demo.CommonService.sayHello()是message
        if (message == null || message.length() == 0) {
            return "Please input method name, eg: \r\ninvoke xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\ninvoke XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\ninvoke com.xxx.XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})";
        }
        StringBuilder buf = new StringBuilder();
        String service = (String) channel.getAttribute(ChangeTelnetHandler.SERVICE_KEY); // todo @chenSy 此处怎么往通道里面设置的？
        if (service != null && service.length() > 0) {
            buf.append("Use default service " + service + ".\r\n"); // 附加输出信息
        }
        int i = message.indexOf("("); //indexOf没有查到指定的字符串，返回-1
        if (i < 0 || !message.endsWith(")")) { // 判断是否包含方法括号，是否以括号结尾
            return "Invalid parameters, format: service.method(args)";
        }
        String method = message.substring(0, i).trim(); // 获取方法名，此处若有服务名，包含服务名，如XxxService.sayHello()
        String args = message.substring(i + 1, message.length() - 1).trim(); //获取包含参数列表的字符串，如："\"en\",\"223\""
        i = method.lastIndexOf("."); //判断是否有点号，方法调用的符号
        if (i >= 0) { //若包含接口名，则将接口和方法拆分
            service = method.substring(0, i).trim(); //获取服务名
            method = method.substring(i + 1).trim();
        }
        List<Object> list;
        try {
            // 将"[" + args + "]" 参数列表组成json数组形式
            list = (List<Object>) JSON.parse("[" + args + "]", List.class); //将参数放入list中
        } catch (Throwable t) {
            return "Invalid json argument, cause: " + t.getMessage();
        }
        Invoker<?> invoker = null;
        Method invokeMethod = null;
        for (Exporter<?> exporter : DubboProtocol.getDubboProtocol().getExporters()) {  //获取服务暴露者
            /**
             * 1）不填接口，也能根据方法找到服务，那不同接口有相同的方法呢？(取其中能比配上的)
             * 会取出接口列表，依次比较方法名和参数列表，若匹配成功则终止
             * 2）填接口，会依次比较接口名、方法名、参数列表类型
             */
            if (service == null || service.length() == 0) {
                invokeMethod = findMethod(exporter, method, list);
                if (invokeMethod != null) {
                    invoker = exporter.getInvoker();
                    break;
                }
            } else {
                // 匹配接口名称：可填简写、全称
                if (service.equals(exporter.getInvoker().getInterface().getSimpleName()) // 匹配接口的简写名
                        || service.equals(exporter.getInvoker().getInterface().getName()) // 匹配接口的全称
                        || service.equals(exporter.getInvoker().getUrl().getPath())) { // 匹配接口的全称
                    invokeMethod = findMethod(exporter, method, list);
                    invoker = exporter.getInvoker();
                    break;
                }
            }
        }
        if (invoker != null) {
            if (invokeMethod != null) {
                try {
                    Object[] array = PojoUtils.realize(list.toArray(), invokeMethod.getParameterTypes(), invokeMethod.getGenericParameterTypes());
                    RpcContext.getContext().setLocalAddress(channel.getLocalAddress()).setRemoteAddress(channel.getRemoteAddress());
                    long start = System.currentTimeMillis();
                    // 具体执行invoke调用：构建RpcInvocation，调用信息 todo @chenSy 调用信息待调试
                    Object result = invoker.invoke(new RpcInvocation(invokeMethod, array)).recreate();
                    long end = System.currentTimeMillis();
                    buf.append(JSON.json(result));
                    buf.append("\r\nelapsed: ");
                    buf.append(end - start);
                    buf.append(" ms.");
                } catch (Throwable t) {
                    return "Failed to invoke method " + invokeMethod.getName() + ", cause: " + StringUtils.toString(t);
                }
            } else {
                buf.append("No such method " + method + " in service " + service);
            }
        } else {
            buf.append("No such service " + service);
        }
        return buf.toString();
    }

}