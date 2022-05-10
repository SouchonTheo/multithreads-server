package linda.search.Request1;

import java.util.Scanner;

import linda.*;

public class Main {

    public static void main(String args[]) {
        if (args.length != 2) {
            System.err.println("linda.search.basic.Main search file.");
            return;
        }
        Scanner scanner = new Scanner(System.in);
        Linda linda = new linda.server.LindaClient("//localhost:4000/LindaServer");
        System.out.println("Combien de Searchers : ");
        int nombresSearchers = scanner.nextInt();
        Searcher searcher;
        Manager manager = new Manager(linda, args[1], args[0], nombresSearchers);
        (new Thread(manager)).start();
        for (int i = 0; i < nombresSearchers; i++) {
            searcher = new Searcher(linda);
            (new Thread(searcher)).start();
        }
        scanner.close();
    }
}
