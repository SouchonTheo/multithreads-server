package linda.test.Client_Serveur;

import linda.Linda;
import linda.Tuple;

public class TestDoubleClientsSecond {
    public static void main(String[] a) {

        // final Linda linda = new linda.shm.CentralizedLinda();
        final Linda linda = new linda.server.LindaClient("//localhost:4000/LindaServer");
        Tuple t1 = new Tuple(4, 5);
        System.out.println("(2) write: " + t1);
        linda.write(t1);

        Tuple t11 = new Tuple(4, 5);
        System.out.println("(2) write: " + t11);
        linda.write(t11);

        Tuple t2 = new Tuple("hello", 15);
        System.out.println("(2) write: " + t2);
        linda.write(t2);

        Tuple t3 = new Tuple(4, "foo");
        System.out.println("(2) write: " + t3);
        linda.write(t3);

        linda.debug("(2)");
    }
}
