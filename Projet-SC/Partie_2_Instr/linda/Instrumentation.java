package linda;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import linda.Multiserver.LindaClient;
import linda.shm.CentralisedLindaBase;
import linda.shm.CentralisedLindaCache;
import linda.shm.CentralisedLindaPar;
import linda.shm.LindaInstru;

public class Instrumentation {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\033[1;31m";
    public static final String ANSI_MAGENTA= "\033[1;35m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_GREEN_BOLD = "\033[1;32m";
    public static final String ANSI_YELLOW =  "\033[1;33m";
    public static final String ANSI_BLUE = "\033[0;94m";
    public static final String ANSI_PURPLE = "\033[1;35m";
    public static final String ANSI_CYAN_UNDERLINED = "\u001B[0;4;96m";
    public static final String ANSI_CYAN = "\u001B[96m";
    public static final String ANSI_WHITE = "\u001B[37m";
    private static boolean file = false;
    private static int nbInvalid = 0;
    private static int currentLine = 0;
    private static List<Integer> invalidLines;
    private static List<String> invalidMessages;
    private static LindaInstru linda;
    private static String realName = "basique";



    public static void main(String[] args) {
        // Créer le linda
        invalidLines = new ArrayList<Integer>();
        invalidMessages = new ArrayList<String>();
        boolean ok = true;

        if (args.length == 2) {
            ok = defineLinda(args[0]);
            if (ok) {
                file = true;
                readFromFile(args[1]);
            } else {
                System.out.println(ANSI_RED + "Version de linda inconnue." + ANSI_RESET);
            }
            ok = false;
        } else if (args.length == 1) {
            ok = defineLinda(args[0]);
            if (!ok) {
                file = true;
                readFromFile(args[0]);
            }
        } else if (args.length == 0) {
            linda = new CentralisedLindaBase();
        }
        if (ok) {      
            Scanner scanner = new Scanner(System.in);
            boolean continueLoop = true;
            while (continueLoop) {
                // Afficher menu départ
                printMenu();
                
                // Regarder le choix et effectuer l'action
                continueLoop = interpret(scanner.nextLine());
            }
            scanner.close();
        }
        
        System.exit(0); // pour arreter tous les threads
    }


    private static void printMenu() {
        System.out.println(ANSI_BLUE + "\nSelectionner votre choix sous la forme " + ANSI_PURPLE + "<action> <tuple> <nombre>");
        System.out.println(ANSI_CYAN_UNDERLINED + "Actions possibles :");
        System.out.println(ANSI_BLUE + "write = " + ANSI_GREEN + "w");
        System.out.println(ANSI_BLUE + "read = " + ANSI_GREEN + "r " + ANSI_BLUE +"; readAll = " + ANSI_GREEN + "ra " + ANSI_BLUE +"; tryRead = " + ANSI_GREEN + "tr");
        System.out.println(ANSI_BLUE + "take = " + ANSI_GREEN + "t " + ANSI_BLUE +"; takeAll = " + ANSI_GREEN + "ta " + ANSI_BLUE +"; tryTake = " + ANSI_GREEN + "tt");
        System.out.println(ANSI_BLUE + "Etat des processus = " + ANSI_GREEN + "ep " + ANSI_BLUE +"; état des files d'attente = " + ANSI_GREEN + "ef");
        System.out.println(ANSI_BLUE + "Afficher la liste des tuples = " + ANSI_GREEN + "p " + ANSI_BLUE +"; nombre de tuples dans l'espace = " + ANSI_GREEN + "n"); 
        System.out.println(ANSI_BLUE + "Quitter =" + ANSI_GREEN + " q");
        System.out.print(ANSI_BLUE + "Ecrivez le tuple sous la forme " + ANSI_PURPLE +"[1,foo,...]\n\n" + ANSI_RESET + "> ");
    }


    private static boolean interpret(String result) {
        
        String[] words = result.trim().split(" \\s*");
        if (words[0].equals("for")) {
            doFor(result, words[1]);
            return true;
        } else if (words.length == 1) {
            return OneWordAction(words[0]);
        } else if (words.length <= 3) {
            return ThreeWordsActions(words);
        } else {
            invalid("Commande non reconnue.");
            return true;
        }
    }
    
    private static boolean OneWordAction(String action) {
        if (file) {
            System.out.println(ANSI_PURPLE + "ligne " + currentLine + ", " + ANSI_CYAN + action + ANSI_PURPLE +  " : " + ANSI_RESET);
        }
        switch (action) {
            case "q" :
                System.out.println(ANSI_MAGENTA + "A" + ANSI_YELLOW + "u " + ANSI_MAGENTA + "r" + ANSI_YELLOW + "e" + ANSI_MAGENTA + "v" + ANSI_YELLOW + "o" + ANSI_MAGENTA + "i" + ANSI_YELLOW + "r" + ANSI_MAGENTA + " !" + ANSI_RESET);
                return false;
            case "ep" :
                if (!realName.equals("basique"))
                    System.out.println("Il y a actuellement " + linda.getNbReadBlocked() + " read(s) blocké(s) et "+ linda.getNbTakeBlocked() + " take(s) blocké(s).");
                break;
            case "ef" :
                if (!realName.equals("basique"))
                    System.out.println("Il y a actuellement " + linda.getNbWriteWaiting() + " write en attente et "+ linda.getNbTakeWaiting() + " take en attente.");
                break;
            case "p" :
                printListTuple();
                break;
            case "n" :
                System.out.println(linda.getNbTuples());
                break;
            default :
                invalid("Commande non reconnue.");
                return true;
            }
            return true;
    }

    
    private static boolean ThreeWordsActions(String[] words) {
        boolean lindaMethod = true;
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
            case "s" :
            sleep(words[1]);
                lindaMethod = false;
                break;
            default :
                invalid("Commande non reconnue.");
                return true;
        }
        if (lindaMethod) { // Seulement si l'on veut appeler une méthode de lind
        try {
                Tuple t = getTuple(words[1]);
                if (t == null) {
                    return true;
                }        
                Method method = Linda.class.getMethod(methodName, Tuple.class);
                Integer nbLoop = words.length == 3 ? Integer.parseInt(words[2]) : 1;
                LaunchAction thread = new LaunchAction(linda, method, t, nbLoop, file);
                thread.start();
            } catch (OutOfMemoryError e) {
                invalid("Trop de threads démarrés. Mémoire maximale de la JVM atteinte.");
                System.gc();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
                    if (elements[i].endsWith(".class")) {
                        String className = elements[i].substring(0, elements[i].length() - 6);
                        Class<?> clazz = Class.forName(className);
                        components[i] = clazz;
                    } else {
                        int j = Integer.parseInt(elements[i]);
                        components[i] = j;
                    }
                } catch (NumberFormatException | ClassNotFoundException e ) {
                    try {
                        float f = Float.parseFloat(elements[i]);
                        components[i] = f;
                    } catch (NumberFormatException excep) {
                        components[i] = elements[i];
                    }
                }
            }
            t = new Tuple(components);
        } else {
            invalid("Fromat du tuple incorrect");
        }
        return t;
    }


    private static void printListTuple() {
        Vector<Tuple> vector = linda.getListTuple();
        for (Tuple t : vector) {
            System.out.print(t+ "  ");
        }
        System.out.println("");
    }

    private static void invalid() {
        invalid("");
    }
    private static void invalid(String message) {
        if (file) {
            invalidMessages.add(message);
            invalidLines.add(currentLine);
            nbInvalid++;
        } else {
            message = message.equals("") ? "" : " : " + message;
            System.out.println(ANSI_RED + "Saisie invalide" + message + ANSI_RESET);
        }
    }



    private static void readFromFile(String filename) {
        try(BufferedReader br = new BufferedReader(new FileReader(filename))) 
        {
            String line;
            long init = System.currentTimeMillis();
            while ((line = br.readLine()) != null) {
                currentLine++;
                if (!line.trim().equals("")) {
                    interpret(line);
                }
            }
            long time =  System.currentTimeMillis()-init;

            printResult(time);
        }
        catch (IOException e) {
            System.out.println(ANSI_RED + "Impossible de lire ce fichier" + ANSI_RESET);
            e.printStackTrace();
        }
    }

    private static void printResult(long time) {
        System.out.println(ANSI_GREEN_BOLD + "\n\nExécution teminée.");
        if (nbInvalid > 0) {
            System.out.println(ANSI_RESET + "Il y a eu " + ANSI_RED + nbInvalid + ANSI_RESET + " opération(s) invalide(s).");
            for (Integer i : invalidLines) {
                System.out.println("ligne " + i + " : " + invalidMessages.get(i));
            }
        } else {
            System.out.print("Aucune opération invalide !" + ANSI_RESET);
        }
        System.out.println(ANSI_BLUE +  "\nTemps d'execution : " + time + " ms." + ANSI_RESET);
    }

    private static void sleep(String time) {
        try {
            int realTime = Integer.parseInt(time);
            Thread.sleep(realTime);
        } catch (NumberFormatException e) {
            invalid();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void doFor(String line, String number) {
        try {
            int nbLoops = Integer.parseInt(number);
            
            line = line.substring(line.indexOf(number) + number.length() + 1); // On enlève le for n

            String[] commands = line.split(";");
            for (int i = 0; i < nbLoops ; i++) {
                for (String command : commands) {
                    interpret(command);
                }
            }
        } catch (NumberFormatException e) {
            invalid();
        }
    }

    private static boolean defineLinda(String lindaName) {
        boolean res = true;
        switch (lindaName) {
            case "p" :
                linda = new CentralisedLindaPar();
                realName = "parallèle";
                break;
            case "c" :
                linda = new CentralisedLindaCache();
                realName = "cache";
                break;
            case "m" :
                linda = new linda.Multiserver.LindaClient("//localhost:4000/LindaServer");
                //Linda linda1 = new linda.server.LindaClient("//localhost:4002/LindaServer");
                realName = "multi-serveur";
                break;
            default:
                linda = new CentralisedLindaBase();
                if (!lindaName.equals("b")) {
                    res = false;
                }
        }
        System.out.println(ANSI_MAGENTA + "La version de linda est : " + realName + ANSI_RESET);
        return res;
    }
}
