package linda.test.Multi_Serv;

import linda.*;

public class BasicTest2 {

    public static void main(String[] a) {
        final Linda linda0 = new linda.server.LindaClient("//localhost:4000/LindaServer");
        final Linda linda2 = new linda.server.LindaClient("//localhost:4002/LindaServer");

        for (int i = 1; i <= 3; i++) {
            final int j = i;
            new Thread() {
                public void run() {
                    Tuple motif = new Tuple(Integer.class, String.class);
                    Tuple res = linda2.read(motif);
                    System.out.println("(" + j + ") Resultat:" + res);
                    linda2.debug("(" + j + ")");
                }
            }.start();
        }

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Tuple t1 = new Tuple(4, 5);
                System.out.println("(0) write: " + t1);
                linda0.write(t1);

                Tuple t2 = new Tuple("hello", 15);
                System.out.println("(0) write: " + t2);
                linda0.write(t2);

                linda0.debug("(0)");

                Tuple t3 = new Tuple(4, "foo");
                System.out.println("(0) write: " + t3);
                linda0.write(t3);

                linda0.debug("(0)");

            }
        }.start();

    }
}
