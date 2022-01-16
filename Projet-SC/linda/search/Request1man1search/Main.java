package linda.search.Request1man1search;

import java.util.Scanner;

import linda.*;

public class Main {

    public static String url = "//localhost:4000/LindaServer";

    public static void main(String args[]) {
        if (args.length != 2) {
            System.err.println("linda.search.basic.Main search file.");
            return;
        }
        int nombresSearchers = 1;

        Linda linda = new linda.server.LindaClient(url);
        Manager manager = new Manager(linda, args[1], args[0], nombresSearchers, url);
        Thread t = new Thread(manager);
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
