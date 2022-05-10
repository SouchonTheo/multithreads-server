package linda.test.Multi_Serv;

import linda.*;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;

public class BasicTestAsyncCallback {

    private static class MyCallback implements Callback {
        public void call(Tuple t) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            System.out.println("Got " + t);
        }
    }

    public static void main(String[] a) {
        final Linda linda0 = new linda.server.LindaClient("//localhost:4000/LindaServer");
        final Linda linda1 = new linda.server.LindaClient("//localhost:4001/LindaServer");
        final Linda linda2 = new linda.server.LindaClient("//localhost:4002/LindaServer");

        Tuple motif = new Tuple(Integer.class, String.class);
        linda0.eventRegister(eventMode.TAKE, eventTiming.IMMEDIATE, motif, new AsynchronousCallback(new MyCallback()));

        Tuple t1 = new Tuple(4, 5);
        System.out.println("(2) write: " + t1);
        linda1.write(t1);

        Tuple t2 = new Tuple("hello", 15);
        System.out.println("(2) write: " + t2);
        linda2.write(t2);
        linda2.debug("(2)");

        Tuple t3 = new Tuple(4, "foo");
        System.out.println("(2) write: " + t3);
        linda1.write(t3);

        linda1.debug("(2)");
    }
}
