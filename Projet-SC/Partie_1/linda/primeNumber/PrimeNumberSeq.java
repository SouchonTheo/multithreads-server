package linda.primeNumber;

import java.util.List;

import linda.Linda;
import linda.Tuple;

public class PrimeNumberSeq {

    private static Linda ld;

    public static void main(String argv[]) {
        Integer n = Integer.parseInt(argv[0]);
        ld = new linda.shm.CentralizedLinda();
        initialize(n);
        for (int i = 2; i < (n / 2) + (n % 2); i++) {
            delete(n, i);
        }
        List<Tuple> nbresPremiers = (List<Tuple>) ld.takeAll(new Tuple(Integer.class));
        System.out.println(nbresPremiers);
    }

    private static void initialize(Integer n) {
        for (int i = 1; i <= n; i++) {
            ld.write(new Tuple(i));
        }
    }

    private static void delete(Integer max, Integer n) {
        if (ld.tryRead(new Tuple(n)) != null) {
            for (int i = 2 * n; i <= max; i += n) {
                ld.tryTake(new Tuple(i));
            }
        }
    }

}
