package com.java.relative.basic.jdbc;

/**
 * 数据库范式设计
 * @author chensy
 * @date 2019-06-14 16:37
 */
public class DbTest {
}

/**
 * 数据库（第一范式，第二范式，第三范式）
 * https://blog.csdn.net/Dream_angel_Z/article/details/45175621
 *
 * 第一范式：列要保证原子性，比如联系电话：应该分为：家庭电话、手机号码
 *
 * 第二范式：一个表必须要有主键（唯一主键、联合主键），没有包含在主键的列表必须完全依赖主键
 * 比如订单明细表：【OrderDetail】（OrderID，ProductID，UnitPrice，Discount，Quantity，ProductName）。
 * 就应该拆分为两个表，订单表和产品表，不符合 2NF 的设计容易产生冗余数据
 * 【OrderDetail】（OrderID，ProductID，Discount，Quantity）
 * 【Product】（ProductID，UnitPrice，ProductName）
 *
 * 第三范式：任何非主属性不依赖于其它非主属性[在2NF基础上消除传递依赖]。
 * 非主键列必须直接依赖于主键，不能存在传递依赖。即不能存在：非主键列 A 依赖于非主键列 B，非主键列 B 依赖于主键的情况
 * 【Order】（OrderID，OrderDate，CustomerID，CustomerName，CustomerAddr，CustomerCity）主键是（OrderID）。
 * CustomerName，CustomerAddr，CustomerCity 直接依赖的是 CustomerID（非主键列），而不是直接依赖于主键
 *
 * 拆分【Order】为【Order】（OrderID，OrderDate，CustomerID）和【Customer】（CustomerID，CustomerName，CustomerAddr，CustomerCity）从而达到 3NF
 *
 *
 * 每个业务表应该考虑增删查改的位置以及数据来源
 */