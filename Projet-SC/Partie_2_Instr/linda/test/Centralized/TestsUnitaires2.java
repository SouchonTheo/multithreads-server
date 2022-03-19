
package linda.test.Centralized;

import linda.*;

public class TestsUnitaires2 {

    /*Test de priorité des accès pour le lecteur rédacteur. Il a servi à trouver une erreur de blockage.*/
    public static void main(String[] a) {
                
        final Linda linda = new linda.shm.CentralizedLinda();
        // final Linda linda = new linda.server.LindaClient("//localhost:4000/aaa");
                
        Tuple t1 = new Tuple(1);
        Tuple t2 = new Tuple(1,1);
        Tuple t3 = new Tuple(1,2);
        Tuple t4 = new Tuple(1,1,1);


        linda.write(t2);

        for (int i = 0; i < 10000; i++){
            linda.write(t1);
            /*Pour forcer un grand espace de tuple long à parcourir*/
        }
        System.out.println("Sortie de l'écriture");

        linda.write(t3);

        Tuple res1 = linda.tryTake(t2);
        System.out.print("Ceci devrait être faux : ");
        System.out.println(res1==null);

        Thread lecture2 = new Thread() {
            @Override
            public void run() {
                for(int i = 0; i< 100; i++) {
                    //linda.write(t2);
                    }
            }
        };
        Thread lecture3 = new Thread() {
            @Override
            public void run() {
                for(int i = 0; i< 100; i++) {
                linda.tryTake(t3);
                }
            }
        };

        lecture2.start();
        lecture3.start();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        //Tuple res4 = linda.read(t4);
        System.out.println("On write");
        linda.write(t4);
        System.out.println("On a fini de write");
        Tuple res5 = linda.read(t4);
        

        System.out.print("Ceci devrait être vrai : ");
        //System.out.println(res4==null);
        System.out.print("Ceci devrait être faux : ");
        System.out.println(res5==null);
        


    }
}
