package linda.primeNumber;

import java.util.List;

import linda.Linda;
import linda.Tuple;

public class PrimeNumberThreadsServer {
    private static String url = "//localhost:4000/LindaServer";
    private static Integer nbresThreads;
    private static Integer n;
    private static List<Tuple> nbresPremiers;

    private static class RemoveThread extends Thread {

        protected Integer id;

        public RemoveThread(Integer id) {
            this.id = id;
        }

        public void run() {
            Integer medMax = (n / 2) + (n % 2);
            Integer max = ((this.id + 1) * medMax) / nbresThreads;
            Integer min = 2;
            if (this.id != 0) {
                min = ((this.id * medMax) / nbresThreads) + 1;
            }

            Linda ld = new linda.server.LindaClient(url);

            for (int i = min; i <= max; i++) {
                delete(i, ld);
            }
        }
    }

    private static class InitializeThread extends Thread {

        protected Integer id;

        public InitializeThread(Integer id) {
            this.id = id;
        }

        public void run() {
            Integer min = ((this.id * n) / nbresThreads) + 1;
            Integer max = ((this.id + 1) * n) / nbresThreads;
            Linda ld = new linda.server.LindaClient(url);
            for (int i = min; i <= max; i++) {
                ld.write(new Tuple(i));
            }
        }
    }

    public static void main(String argv[]) {
        // ld.takeAll(new Tuple(Integer.class));
        long startTime = System.currentTimeMillis();
        n = Integer.parseInt(argv[0]);
        nbresThreads = n / 10 + n % 2;
        if (argv.length > 1) {
            nbresThreads = Integer.parseInt(argv[1]);
        }
        initialize();

        RemoveThread t;
        for (Integer k = 0; k < nbresThreads; k++) {
            t = new RemoveThread(k);
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Liste des nombres premiers de 1 à " + n + " :");
        Linda ld = new linda.server.LindaClient(url);
        nbresPremiers = (List<Tuple>) ld.takeAll(new Tuple(Integer.class));
        System.out.println(nbresPremiers);
        System.out.println("Total execution time: " + (endTime - startTime) * Math.pow(10, -3) + "s");
    }

    private static void initialize() {
        InitializeThread t;
        for (Integer k = 0; k < nbresThreads; k++) {
            t = new InitializeThread(k);
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void delete(Integer min, Linda ld) {
        if (ld.tryRead(new Tuple(min)) != null) {
            for (int i = 2 * min; i <= n; i += min) {
                ld.tryTake(new Tuple(i));
            }
        }
    }

}
