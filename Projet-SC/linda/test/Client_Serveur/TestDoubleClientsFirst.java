package linda.test.Client_Serveur;

import linda.*;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;

public class TestDoubleClientsFirst {

    private static class MyCallback implements Callback {
        public void call(Tuple t) {
            System.out.println("CB got " + t);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            System.out.println("CB done with " + t);
        }
    }
    public static void main(String[] a) {

        final Linda linda = new linda.server.LindaClient("//localhost:4000/LindaServer");
        Tuple cbmotif = new Tuple(Integer.class, String.class);
        linda.eventRegister(eventMode.TAKE, eventTiming.IMMEDIATE, cbmotif, new MyCallback());
    }
}
