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
package com.alibaba.dubbo.common.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

/**
 * JSON reader.(JSON 字符输入流)
 *
 * @author qian.lei
 */

public class JSONReader {

    private static ThreadLocal<Yylex> LOCAL_LEXER = new ThreadLocal<Yylex>() {
    };

    private Yylex mLex;

    public JSONReader(InputStream is, String charset) throws UnsupportedEncodingException {
        this(new InputStreamReader(is, charset));
    }

    /**
     * 构建JSON 输入流
     *   构造词法分析器，并设置到成员变量
     */
    public JSONReader(Reader reader) {
        mLex = getLexer(reader);
    }

    /**
     * 从输入流中获取词法解析器Yylex
     * 1）从本地线程中ThreadLocal获取词法分析器Yylex
     * 2）若词法分析器为空，则创建对象并设置到ThreadLocal中
     *    若词法分析器不为空，则读取新的输入流对词法分析器进行重置
     */
    private static Yylex getLexer(Reader reader) {
        Yylex ret = LOCAL_LEXER.get();
        if (ret == null) {
            ret = new Yylex(reader);
            LOCAL_LEXER.set(ret);
        } else {
            ret.yyreset(reader);
        }
        return ret;
        /**
         * 问题集： todo 0728
         * 1）调试看词法分析器、以及ThreadLocal
         * 2）调试yyreset() 方法
         */
    }

    public JSONToken nextToken() throws IOException, ParseException {
        return mLex.yylex();
    }

    /**
     * 获取下一个分词
     * 若分词为空或分词的类型不是传入的类型，则抛出异常
     */
    public JSONToken nextToken(int expect) throws IOException, ParseException {
        JSONToken ret = mLex.yylex();
        if (ret == null)
            throw new ParseException("EOF error.");
        if (expect != JSONToken.ANY && expect != ret.type)
            throw new ParseException("Unexcepted token.");
        return ret;
    }

    /**
     * 问题集
     * 1）实践：ThreadLocal了解以及使用
     *    解：目前已基本使用
     * 2）InputStreamReader了解、InputStream、Reader了解以及使用
     */
}