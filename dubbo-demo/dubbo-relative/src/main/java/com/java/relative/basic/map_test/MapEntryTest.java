package com.java.relative.basic.map_test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 接口 Map.Entry<K,V> 使用，Map.Entry这是接口，并不是内部类
 * @author chensy
 * @date 2019-06-13 19:42
 */
public class MapEntryTest {
    public static void main(String[] args) {
        Map<String, String> map = new HashMap<>();
        map.put("name", "zhang");
        map.put("address", "hangzhou");

        MapEntryTest entryTest = new MapEntryTest();
        entryTest.basic(map);
        System.out.println("---entryMap-----");
        entryTest.getEntrySet(map);
        System.out.println("---iterator-----");
        entryTest.getIterator(map);
    }

    //普通使用方法，二次取值
    public void basic(Map<String, String> map) {
        for (String key : map.keySet()) {
            System.out.println(key + "," + map.get(key));
        }
    }

    //通过map 调用entrySet方法,返回Set集合
    public void getEntrySet(Map<String, String> map) {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            System.out.println(entry.getKey() + "," + entry.getValue());
        }
    }

    //通过iterator遍历
    public void getIterator(Map<String, String> map) {
        Iterator< Map.Entry<String, String> > it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            System.out.println(entry.getKey() + "," + entry.getValue());
        }
    }


}

/**
 * Map是java中的接口，Map.Entry是Map的一个内部接口。
 *
 * Map提供了一些常用方法，如keySet()、entrySet()等方法。
 *
 * keySet()方法返回值是Map中key值的集合；entrySet()的返回值也是返回一个Set集合，此集合的类型为Map.Entry。
 *
 * Map.Entry是Map声明的一个内部接口，此接口为泛型，定义为Entry<K,V>。它表示Map中的一个实体（一个key-value对）。接口中有getKey(),getValue方法。
 */
