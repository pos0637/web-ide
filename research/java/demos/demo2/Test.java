import sub.Test1;

public class Test {
    private static double f = 1.5D;
    private static long l = 123223213L;
    private String d ="abc";

    public void foo() {
        int a = 10;
        int b = 20;
        bar(a, b);

        Test1 test1 = new Test1();
        test1.foo();
        System.out.println(test1.a);
    }

    public void bar(int a, int b) {
        double c = f * l + a / b;
        System.out.println("Hello World! " + c + d);
    }

    public static void main(String[] args) {
        new Test().foo();
    }
}
