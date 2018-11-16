/**
 * @author chensy
 * @date 2018/11/8 上午7:36
 */
public class JavaTest {
    public static void main(String[] args) {
        //Enum e = new Enum("aa",1);  程序员无法调用此构造函数。 它由编译器响应枚举类型声明发出的代码使用。
        for(Fruits fruits : Fruits.values()){
            System.out.println(fruits);
        }
    }
}

enum Fruits {
    APPLE,BANANA,ORANGE
}
