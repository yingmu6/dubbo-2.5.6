package com.java.relative.basic.callback;

/**
 * @author chensy
 * @date 2019-07-14 01:01
 */
public class Test {
    public static void main(String[] args) {
        //接口回调，将什么实例赋值给接口，就回调哪个实例的方法
        Animal animal = new Dog();
        Animal animal1 = new Pig();
        animal.cry();
        animal1.cry();

        //向上转型 将子类赋值给父类变量，使用哪个子类类型就指向哪个
        Dog bigDog = new BigDog();
        bigDog.cry();
        Dog smallDog = new SmallDog();
        smallDog.cry();
    }

    /**
     * https://e6621887.iteye.com/blog/349396
     * 接口回调是指：可以把使用实现了某一接口的类创建的对象的引用赋给该接口声明的接口变量，那么该接口变量就可以调用被类实现的接口的方法。
     *      实际上，当接口变量调用被类实现的接口中的方法时，就是通知相应的对象调用接口的方法，这一过程称为对象功能的接口回调
     */
}
