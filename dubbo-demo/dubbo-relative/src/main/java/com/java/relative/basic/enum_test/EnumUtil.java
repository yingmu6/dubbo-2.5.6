package com.java.relative.basic.enum_test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * 枚举工具类使用 EnumSet 和 EnumMap
 * @author chensy
 * @date 2019-06-13 15:57
 */
public class EnumUtil {
    private enum Animal {Apple, Banana, Orange};
    public static void main(String[] args) {
        EnumUtil enumUtil = new EnumUtil();
        enumUtil.testEnumSet();

        enumUtil.testEnumMap ();
    }

    // 测试EnumSet
    public void testEnumSet () {
        // EnumSet可以获取某个范围的枚举实例
        EnumSet<Animal> elements = EnumSet.range(Animal.Apple, Animal.Banana);
        for (Animal animal : elements ) {
            System.out.println(animal.name());
        }
        System.out.println("------分隔1------");
        EnumSet<Animal> elements2 = EnumSet.allOf(Animal.class); //创建指定类型的所有枚举值的集合
        for (Animal animal : elements2 ) {
            System.out.println(animal.name());
        }
        System.out.println("------分隔2------");
        EnumSet<Animal> elements3 = EnumSet.of(Animal.Apple, Animal.Orange); //创建指定枚举值的集合
        for (Animal animal : elements3 ) {
            System.out.println(animal.name());
        }
    }

    // 测试EnumMap
    public void testEnumMap () {
        EnumMap<Animal, String> enumMap = new EnumMap<>(Animal.class);
        enumMap.put(Animal.Apple, "apple map");
        enumMap.put(Animal.Orange, "orange map");
        for (Map.Entry<Animal, String> entry : enumMap.entrySet()) { //Entry使用，直接获取key、value，不用遍历key，然后再获取值
            System.out.println("key:" + entry.getKey().name() + ", val:" + entry.getValue());
        }

    }
}

/**
 *
 * 像大多数集合实现一样， EnumMap不同步。 如果多个线程同时访问枚举映射，并且至少有一个线程修改映射，则应该在外部进行同步。
 * 这通常通过在自然地封装枚举映射的某些对象上进行同步来实现。 如果没有此类对象存在，则应使用Collections.synchronizedMap(java.util.Map<K, V>)方法“包装”地图。
 * 这最好在创建时完成，以防止意外的不同步访问：
 *
 *   Map<EnumKey, V> m
 *          = Collections.synchronizedMap(new EnumMap<EnumKey, V>(...));
 */
