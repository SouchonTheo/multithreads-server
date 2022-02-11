package linda.search.RequestFV;

import java.util.Scanner;

import linda.*;

public class Main {

    public static String url = "//localhost:4000/LindaServer";

    public static void main(String args[]) {
        if (args.length != 2) {
            System.err.println("linda.search.basic.Main search file.");
            return;
        }
        Scanner scanner = new Scanner(System.in);
        System.out.print("Combien de Searchers : ");
        int nombresSearchers = 1;

        Linda linda = new linda.server.LindaClient(url);
        Manager manager = new Manager(linda, args[1], args[0], nombresSearchers, url);
        Thread t = new Thread(manager);
        Boolean continuer = true;
        while (continuer) {
            System.out.print("Voulez vous ajouter un manager ? (1 pour oui, 0 sinon) : ");
            int decision = scanner.nextInt();
            if (decision == 1) {
                System.out.println("Combien de Searchers :");
                nombresSearchers = scanner.nextInt();
                linda = new linda.server.LindaClient(url);
                manager = new Manager(linda, args[1], args[0], nombresSearchers, url);
                (new Thread(manager)).start();
                // manager.endSearch();
            } else {
                System.out.println("\n\n" + decision + "\n");
                System.out.println("Au revoir !");
                continuer = false;
            }
        }
        scanner.close();
    }
}
