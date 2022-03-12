package linda;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Scanner;
import java.util.Vector;

import linda.shm.CentralizedLinda;

public class Instrumentation {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\033[1;31m";
    public static final String ANSI_MAGENTA= "\033[1;35m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW =  "\033[1;33m";
    public static final String ANSI_BLUE = "\033[0;94m";
    public static final String ANSI_PURPLE = "\033[1;35m";
    public static final String ANSI_CYAN = "\u001B[0;4;96m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static void main(String[] args) {
        // Créer le linda
        CentralizedLinda linda = new linda.shm.CentralizedLinda();
        
        Scanner scanner = new Scanner(System.in);
        boolean continueLoop = true;
        Vector<Tuple> vector = new Vector<Tuple>();
        while (continueLoop) {
            // Afficher menu départ
            printMenu();
            
            // Regarder le choix et effectuer l'action
            continueLoop = interpret(scanner.nextLine(), linda);
            vector = linda.getListTuple();
        }
    }


    private static void printMenu() {
        System.out.println(ANSI_BLUE + "\nSelectionner votre choix sous la forme " + ANSI_PURPLE + "<action> <tuple> <nombre>");
        System.out.println(ANSI_CYAN + "Actions possibles :");
        System.out.println(ANSI_BLUE + "write = " + ANSI_GREEN + "w");
        System.out.println(ANSI_BLUE + "read = " + ANSI_GREEN + "r " + ANSI_BLUE +"; readAll = " + ANSI_GREEN + "ra " + ANSI_BLUE +"; tryRead = " + ANSI_GREEN + "tr");
        System.out.println(ANSI_BLUE + "take = " + ANSI_GREEN + "t " + ANSI_BLUE +"; takeAll = " + ANSI_GREEN + "ta " + ANSI_BLUE +"; tryTake = " + ANSI_GREEN + "tt");
        System.out.println(ANSI_BLUE + "Etat des processus = " + ANSI_GREEN + "ep " + ANSI_BLUE +"; état des files d'attente = " + ANSI_GREEN + "ef");
        System.out.println(ANSI_BLUE + "Afficher la liste des tuples = " + ANSI_GREEN + "p " + ANSI_BLUE +"; nombre de tuples dans l'espace = " + ANSI_GREEN + "n"); 
        System.out.println(ANSI_BLUE + "Quitter =" + ANSI_GREEN + " q");
        System.out.println(ANSI_BLUE + "Ecrivez le tuple sous la forme " + ANSI_PURPLE +"[1,foo,...]\n" + ANSI_RESET);
    }


    private static boolean interpret(String result, CentralizedLinda linda) {
        String[] words = result.split(" ");
        Tuple t;
        if (words.length == 1) {
            return OneWordAction(words[0], linda);
        } else if (words.length <= 3) {
            return ThreeWordsActions(words, linda);
        } else {
            System.out.println(ANSI_RED + "Saisie invalide" + ANSI_RESET);
            return true;
        }
    }

    private static boolean OneWordAction(String action, CentralizedLinda linda) {
        switch (action) {
            case "q" :
                System.out.println(ANSI_MAGENTA + "A" + ANSI_YELLOW + "u " + ANSI_MAGENTA + "r" + ANSI_YELLOW + "e" + ANSI_MAGENTA + "v" + ANSI_YELLOW + "o" + ANSI_MAGENTA + "i" + ANSI_YELLOW + "r" + ANSI_MAGENTA + " !" + ANSI_RESET);
                System.exit(0);
                return false;
            case "ep" :
                System.out.println("Il y a actuellement " + linda.getNbReadBlocked() + " read blocké(s) et "+ linda.getNbTakeBlocked() + " take blockéts).");
                break;
            case "ef" :
                System.out.println("Il y a actuellement " + linda.getNbWriteWaiting() + " write en attente et "+ linda.getNbTakeWaiting() + " take en attente.");
                break;
            case "p" :
                printListTuple(linda);
                break;
            case "n" :
                System.out.println(linda.getNbTuples());
                break;
            default :
                System.out.println(ANSI_RED + "Saisie invalide" + ANSI_RESET);
                return true;
        }
        return true;
    }


    private static boolean ThreeWordsActions(String[] words, Linda linda) {
        Tuple t = words.length == 1 ? new Tuple(0) : getTuple(words[1]);
        String methodName = "write";
        switch (words[0]) {
            case "w" :
                methodName= "write";
                break;
            case "r" :
                methodName= "read";
                break;
            case "ra" :
                methodName= "readAll";
                break;
            case "tr" :
                methodName= "tryRead";
                break;
            case "t" :
                methodName= "take";
                break;
            case "ta" :
                methodName= "takeAll";
                break;
            case "tt" :
                methodName= "tryTake";
                break;
            default :
                System.out.println(ANSI_RED + "Saisie invalide" + ANSI_RESET);
                return true;
        }
        try {
            Method method = Linda.class.getMethod(methodName, Tuple.class);
            Integer nbLoop = words.length == 3 ? Integer.parseInt(words[2]) : 1;
            LaunchAction thread = new LaunchAction(linda, method, t, nbLoop);
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }


    private static Tuple getTuple(String string) {
        Tuple t = null;
        if (string.startsWith("[") && string.endsWith("]")) {
            string = string.substring(1, string.length()-1);
            String[] elements = string.split(",");
            Serializable[] components = new Serializable[elements.length];
            for (int i = 0; i < elements.length; i++) {
                try {
                    int j = Integer.parseInt(elements[i]);
                    components[i] = j;
                } catch (Exception e ) {
                    components[i] = elements[i];
                }
            }
            t = new Tuple(components);
        } else {
            System.out.println(ANSI_RED + "Lecture du tuple invalide, l'opération est effectuée avec le tuple [0] à la place" + ANSI_RESET);
            t = new Tuple(0);
        }
        return t;
    }


    private static void printListTuple(CentralizedLinda linda) {
        Vector<Tuple> vector = linda.getListTuple();
        for (Tuple t : vector) {
            System.out.print(t+ "  ");
        }
        System.out.println("");
    }
}
