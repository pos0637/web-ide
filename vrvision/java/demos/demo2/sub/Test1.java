package sub;

public class Test1 {
    public String axx = "abc";

    public void foo() {
        System.out.println("Test1:foo");
    }

    public int bar(int abc, int b) {
        System.out.println("Test1:bar " + (abc + b));
        return abc + b;
    }
}
