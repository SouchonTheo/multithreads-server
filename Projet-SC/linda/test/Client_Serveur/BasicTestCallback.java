
package linda.test.Client_Serveur;

import linda.*;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;

public class BasicTestCallback {

    private static Linda linda;
    private static Tuple cbmotif;

    private static class MyCallback implements Callback {
        public void call(Tuple t) {
            System.out.println("CB got " + t);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            System.out.println(cbmotif);
            linda.eventRegister(eventMode.TAKE, eventTiming.IMMEDIATE, cbmotif, this);
        }
    }

    public static void main(String[] a) {
        final Linda linda = new linda.server.LindaClient("//localhost:4000/LindaServer");

        cbmotif = new Tuple(Integer.class, String.class);
        linda.eventRegister(eventMode.TAKE, eventTiming.IMMEDIATE, cbmotif, new MyCallback());
        Tuple t1;
        for (int i = 1; i < 3; i++) {
            t1 = new Tuple(4, "foo");
            System.out.println("write: " + t1);
            linda.write(t1);
        }

        linda.debug("(2)");

    }

}
