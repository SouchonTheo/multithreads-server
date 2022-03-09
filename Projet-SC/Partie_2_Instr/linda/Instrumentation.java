package linda;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Scanner;
import java.util.Vector;

import linda.shm.CentralizedLinda;

public class Instrumentation {

    public static void main(String[] args) {
        // Créer le linda
        CentralizedLinda linda = new linda.shm.CentralizedLinda();
        
        Scanner scanner = new Scanner(System.in);
        boolean continueLoop = true;
        while (continueLoop) {
            // Afficher menu départ
            printMenu();
            
            // Regarder le choix et effectuer l'action
            continueLoop = interpret(scanner.nextLine(), linda);
            Vector<Tuple> vector = linda.getListTuple();
            for (Tuple t : vector) {
                System.out.println(t);
            }
        }
    }


    private static void printMenu() {
        System.out.println("Selectionner votre choix sous la forme <action> <tuple> <nombre>");
        System.out.println("Actions possibles : write = w ; read = r ; readAll = ra ; tryRead = tr ; take = t ; takeAll = ta ; tryTake = tt, quit = q");
        System.out.println("Ecrivez le tuple sous la forme [1,'foo',...]\n\n");
    }


    private static boolean interpret(String result, Linda linda) {
        String[] words = result.split(" ");
        Tuple t;
        if (words.length == 1 && words[0].equals("q")) {
            return false;
        } else if (words.length <= 3) {
            t = words.length == 1 ? new Tuple(0) : getTuple(words[1]);
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
                    System.out.println("Saisie invalide");
                    return true;
            }
            try {
                Method method = Linda.class.getMethod(methodName, Tuple.class);
                Integer nbLoop = words.length == 3 ? Integer.parseInt(words[2]) : 1;
                launchAction(linda, method, t, nbLoop);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } else {
            System.out.println("Saisie invalide");
            return true;
        }
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
            t = new Tuple(0);
        }
        return t;
    }

    private static void launchAction(Linda linda, Method m, Tuple t, Integer nbLoop){
        for (int i = 0; i < nbLoop; i++){
            try {
                // TODO
                m.invoke(linda,t);// Faire des threads !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
