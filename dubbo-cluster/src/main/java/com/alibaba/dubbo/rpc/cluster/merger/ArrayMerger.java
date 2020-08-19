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
package com.alibaba.dubbo.rpc.cluster.merger;

import com.alibaba.dubbo.rpc.cluster.Merger;

import java.lang.reflect.Array;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class ArrayMerger implements Merger<Object[]> {

    public static final ArrayMerger INSTANCE = new ArrayMerger();

    /**
     * 合并数组的参数
     * 1）查找出数组类型和数组总长度
     * 2）依次遍历参数，把参数作为数组遍历，依次加到总数组中
     */
    public Object[] merge(Object[]... others) {
        if (others.length == 0) {
            return null;
        }
        int totalLen = 0;
        for (int i = 0; i < others.length; i++) { /**@c 遍历参数数组 */
            Object item = others[i];
            if (item != null && item.getClass().isArray()) { /**@c 判断参数是否数组类型 */
                totalLen += Array.getLength(item);
            } else {
                throw new IllegalArgumentException(
                        new StringBuilder(32).append(i + 1)
                                .append("th argument is not an array").toString());
            }
        }

        if (totalLen == 0) {
            return null;
        }

        Class<?> type = others[0].getClass().getComponentType(); /**@c 数组中的元素都是相同类型的，所以取第一个就好 */

        Object result = Array.newInstance(type, totalLen); /**@c 创建指定类型、指定总长度的数组对象 */
        int index = 0;
        for (Object array : others) {
            for (int i = 0; i < Array.getLength(array); i++) { /**@c 每个元素都是数组元素，依次遍历数组元素，加入到总数组 */
                Array.set(result, index++, Array.get(array, i));
            }
        }
        return (Object[]) result;
    }

}
