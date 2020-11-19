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
package com.alibaba.dubbo.remoting.transport.netty;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

import org.jboss.netty.logging.AbstractInternalLogger;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;

/**
 * @author <a href="mailto:gang.lvg@taobao.com">kimi</a>
 */
final class NettyHelper { //继承netty的日志接口进行日志打印

    /**
     * InternalLoggerFactory 是工厂方法模式：抽象了创建对象的方法，对象的创建交由子类进行
     * public abstract InternalLogger newInstance(String name);
     */
    public static void setNettyLoggerFactory() {
        InternalLoggerFactory factory = InternalLoggerFactory.getDefaultFactory();
        if (factory == null || !(factory instanceof DubboLoggerFactory)) {
            InternalLoggerFactory.setDefaultFactory(new DubboLoggerFactory());
        }
    }

    static class DubboLoggerFactory extends InternalLoggerFactory { //DubboLoggerFactory静态内部类

        @Override
        public InternalLogger newInstance(String name) {
            return new DubboLogger(LoggerFactory.getLogger(name));
        }
    }

    /**
     * 静态内部类
     * 静态内部类与非静态内部类之间存在一个最大的区别，就是非静态内部类在编译完成之后会隐含地保存着一个引用，
     * 该引用是指向创建它的外围内，但是静态内部类却没有
     *
     * 静态内部类：
     * 1.只能在内部类中定义静态类
     * 2.静态内部类与外层类绑定，即使没有创建外层类的对象，它一样存在。
     * 3.静态类的方法可以是静态的方法也可以是非静态的方法，静态的方法可以在外层通过静态类调用，而非静态的方法必须要创建类的对象之后才能调用。
     * 4.只能引用外部类的static成员变量（也就是类变量）。
     * 5.如果一个内部类不是被定义成静态内部类，那么在定义成员变量或者成员方法的时候，是不能够被定义成静态的
     *
     * https://juejin.im/post/6844903791863529480 静态内部类与非静态内部类的区别
     */
    static class DubboLogger extends AbstractInternalLogger { //继承Netty日志
        /**
         * netty中的AbstractInternalLogger实现了InternalLogger接口，
         * 但AbstractInternalLogger没有具体实现，交由抽象类的子类去实现
         *
         * 类继承关系：
         * DubboLogger继承了AbstractInternalLogger抽象类
         * AbstractInternalLogger实现InternalLogger了接口
         */

        private Logger logger;

        DubboLogger(Logger logger) {
            this.logger = logger;
        }

        public boolean isDebugEnabled() { //该接口来自InternalLogger
            return logger.isDebugEnabled();
        }

        public boolean isInfoEnabled() {
            return logger.isInfoEnabled();
        }

        public boolean isWarnEnabled() {
            return logger.isWarnEnabled();
        }

        public boolean isErrorEnabled() {
            return logger.isErrorEnabled();
        }

        public void debug(String msg) {
            logger.debug(msg);
        }

        public void debug(String msg, Throwable cause) {
            logger.debug(msg, cause);
        }

        public void info(String msg) {
            logger.info(msg);
        }

        public void info(String msg, Throwable cause) {
            logger.info(msg, cause);
        }

        public void warn(String msg) {
            logger.warn(msg);
        }

        public void warn(String msg, Throwable cause) {
            logger.warn(msg, cause);
        }

        public void error(String msg) {
            logger.error(msg);
        }

        public void error(String msg, Throwable cause) {
            logger.error(msg, cause);
        }

        @Override
        public String toString() {
            return logger.toString();
        }
    }

}
