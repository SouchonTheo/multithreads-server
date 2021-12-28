package linda.test.Centralized;

import linda.*;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;

public class CallbackPlusTakeReadTest {

    private static Tuple cbmotif;

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
        final Linda linda = new linda.shm.CentralizedLinda();
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Tuple motif = new Tuple(Integer.class, String.class);
                Tuple res = linda.take(motif);
                System.out.println("(Take Results): " + res);
                linda.debug("(Take)");
            }
        }.start();

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Tuple motif = new Tuple(Integer.class, String.class);
                Tuple res = linda.read(motif);
                System.out.println("(Read Resultat): " + res);
                linda.debug("(Read)");
            }
        }.start();

        new Thread() {
            // linda = new linda.server.LindaClient("//localhost:4000/MonServeur");
            public void run() {

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                cbmotif = new Tuple(Integer.class, String.class);
                linda.eventRegister(eventMode.TAKE, eventTiming.IMMEDIATE, cbmotif, new MyCallback());

                // linda.eventRegister(eventMode.READ, eventTiming.IMMEDIATE, cbmotif, new
                // MyCallback());

                Tuple t1 = new Tuple(4, 5);
                System.out.println("(2) write: " + t1);
                linda.write(t1);

                Tuple t2 = new Tuple("hello", 15);
                System.out.println("(2) write: " + t2);
                linda.write(t2);

                Tuple t3 = new Tuple(4, "foo");
                System.out.println("(2) write: " + t3);
                linda.write(t3);

                linda.debug("(2)");

            }

        }.start();

    }
}
