package linda.test.Centralized;

import linda.Tuple;

public class testTuple {
    public static void main(String[] args) {
        Tuple t = new Tuple("r", 2);
        Tuple t2 = new Tuple(t, 0);
        System.out.println(t2.get(0));
    }
}
