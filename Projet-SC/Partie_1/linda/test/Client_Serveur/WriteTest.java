package linda.test.Client_Serveur;

import java.util.Vector;

import linda.*;

public class WriteTest {

    public static void main(String[] a) {

        final Linda linda = new linda.server.LindaClient("//localhost:4000/LindaServer");

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // On test l'ajout en parallèle
                Tuple motif = new Tuple(4, 5);
                System.out.println("(1) write: " + motif);
                linda.write(motif);
                // Avec des motifs
                motif = new Tuple(Integer.class, String.class);
                System.out.println("(1) write: " + motif);
                linda.write(motif);
                linda.debug("(1)");
            }
        }.start();

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Un tuple normal
                Tuple t1 = new Tuple(4, 5);
                System.out.println("(2) write: " + t1);
                linda.write(t1);
                // On l'ajoute en double
                Tuple t11 = new Tuple(4, 5);
                System.out.println("(2) write: " + t11);
                linda.write(t11);
                // Un tuple avec une string
                Tuple t2 = new Tuple("hello", 15);
                System.out.println("(2) write: " + t2);
                linda.write(t2);
                // Un tuple dans un autre
                Tuple t3 = new Tuple(t2, t11);
                System.out.println("(2) write: " + t3);
                linda.write(t3);
                // Avec null dans le tuple
                Tuple t4 = new Tuple(null, 'x');
                try {
                    linda.write(t4);
                } catch (IllegalStateException e) {
                    System.out.println("(2) write : " + t4.get(0) + ", " + t4.get(1) + " OK");
                }
                // Avec null
                Tuple t5 = null;
                try {
                    linda.write(t5);
                } catch (IllegalStateException e) {
                    System.out.println("(2) write: " + t5 + " OK");
                }

                // Avec une liste dans le tuple
                Vector<Integer> v = new Vector<Integer>();
                v.add(1);
                v.add(432);
                v.add(3);
                Tuple t6 = new Tuple(-1, v);
                System.out.println("(2) write t6 : " + t6);
                linda.write(t6);

                linda.debug("(2)");

            }
        }.start();

    }
}
