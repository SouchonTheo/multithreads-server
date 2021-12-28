package linda.search.Request2;

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
        System.out.print("Combien de Searchers : ");
        int nombresSearchers = scanner.nextInt();
        Manager manager = new Manager(linda, args[1], args[0], nombresSearchers);
        (new Thread(manager)).start();
        Boolean continuer = true;
        while (continuer) {
            System.out.print("Voulez vous ajouter un manager ? : ");
            String decision = scanner.nextLine();
            if (decision.equals("oui") || decision.equals("o")) {
                System.out.println("Combien de Searchers : ");
                nombresSearchers = scanner.nextInt();
                manager = new Manager(linda, args[1], args[0], nombresSearchers);
                (new Thread(manager)).start();
            } else {
                System.out.println("Salut !");
                continuer = false;
            }
        }
        scanner.close();
    }
}
