package com.java.relative.basic.abstract_test;

/**
 * 抽象类不一定要有抽象方法，但有抽象方法就一定要被子类实现
 * @author chensy
 * @date 2019-05-31 17:02
 */
public abstract class Employee {
    private String name;
    private String address;
    private int number;
    // 构造方法不能是abstract
    public  Employee(String name, String address, int number)
    {
        System.out.println("Constructing an Employee");
        this.name = name;
        this.address = address;
        this.number = number;
    }
    public double computePay()
    {
        System.out.println("Inside Employee computePay");
        return 0.0;
    }
    public void mailCheck()
    {
        System.out.println("Mailing a check to " + this.name
                + " " + this.address);
    }
    public String toString()
    {
        return name + " " + address + " " + number;
    }
    public String getName()
    {
        return name;
    }
    public String getAddress()
    {
        return address;
    }
    public void setAddress(String newAddress)
    {
        address = newAddress;
    }
    public int getNumber()
    {
        return number;
    }

    // 抽象方法
    public abstract void sayHello();

    //静态方法不能是抽象方法
    // public abstract static void sayHi();

}
