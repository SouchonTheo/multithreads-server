
package linda.test.Multi_Serv;

import linda.*;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;

public class BasicTestCallback {

    private static class MyCallback implements Callback {

        private Linda ld;
        private Tuple tp;

        public MyCallback(Linda ld, Tuple cmf) {
            this.ld = ld;
            this.tp = cmf;
        }

        public void call(Tuple t) {
            System.out.println("CB got " + t);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            System.out.println(this.tp);
            this.ld.eventRegister(eventMode.TAKE, eventTiming.IMMEDIATE, this.tp, this);
        }
    }

    public static void main(String[] a) {
        final Linda linda0 = new linda.server.LindaClient("//localhost:4000/LindaServer");
        final Linda linda1 = new linda.server.LindaClient("//localhost:4001/LindaServer");
        final Linda linda2 = new linda.server.LindaClient("//localhost:4002/LindaServer");

        Tuple cbmotif = new Tuple(Integer.class, String.class);
        MyCallback mc = new MyCallback(linda0, cbmotif);
        linda0.eventRegister(eventMode.TAKE, eventTiming.IMMEDIATE, cbmotif, mc);
        Tuple t1;
        for (int i = 1; i < 3; i++) {
            t1 = new Tuple(4, "foo");
            System.out.println("write: " + t1);
            linda0.write(t1);
        }

        linda0.debug("(2)");

    }

}
