package linda.test.Multi_Serv;

import linda.*;

public class TakereadTest {

    public static void main(String[] a) {

        final Linda linda0 = new linda.server.LindaClient("//localhost:4000/LindaServer");
        final Linda linda1 = new linda.server.LindaClient("//localhost:4001/LindaServer");
        final Linda linda2 = new linda.server.LindaClient("//localhost:4002/LindaServer");

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Tuple motif = new Tuple(Integer.class, String.class);
                Tuple res = linda0.read(motif);
                System.out.println("(1) Resultat read:" + res);
                linda0.debug("(1)");
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
                Tuple res = linda1.take(motif);
                System.out.println("(2) Resultat take:" + res);
                linda1.debug("(2)");
            }
        }.start();

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Tuple t1 = new Tuple(4, 5);
                System.out.println("(3) write: " + t1);
                linda2.write(t1);

                Tuple t11 = new Tuple(4, 5);
                System.out.println("(3) write: " + t11);
                linda2.write(t11);

                Tuple t2 = new Tuple("hello", 15);
                System.out.println("(3) write: " + t2);
                linda2.write(t2);

                Tuple t3 = new Tuple(4, "foo");
                System.out.println("(3) write: " + t3);
                linda2.write(t3);

                linda2.debug("(3)");

            }
        }.start();

    }

}
