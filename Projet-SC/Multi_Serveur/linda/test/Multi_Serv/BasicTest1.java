package linda.test.Multi_Serv;

import linda.Linda;
import linda.Tuple;

public class BasicTest1 {
    public static void main(String[] a) {

        // final Linda linda = new linda.shm.CentralizedLinda();
        final Linda linda0 = new linda.server.LindaClient("//localhost:4000/LindaServer");
        final Linda linda1 = new linda.server.LindaClient("//localhost:4002/LindaServer");

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Tuple motif = new Tuple(Integer.class, String.class);
                Tuple res = linda1.take(motif);
                System.out.println("(1) Resultat:" + res);
                linda1.debug("(1)");
            }
        }.start();

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Tuple t1 = new Tuple(4, 5);
                System.out.println("(2) write: " + t1);
                linda0.write(t1);

                Tuple t11 = new Tuple(4, 5);
                System.out.println("(2) write: " + t11);
                linda0.write(t11);

                Tuple t2 = new Tuple("hello", 15);
                System.out.println("(2) write: " + t2);
                linda0.write(t2);

                Tuple t3 = new Tuple(4, "foo");
                System.out.println("(2) write: " + t3);
                linda0.write(t3);

                linda0.debug("(2)");

            }
        }.start();

    }
}
