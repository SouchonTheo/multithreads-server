package linda.nbresPremiers;

import java.util.ArrayList;
import java.util.List;
import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;

public class CribleEratosthene {
	static int limite;
	static int nbThreads;
	static double sqrt;
	static List<Integer> resultat = new ArrayList<Integer>();
	// final static Linda linda = new linda.shm.CentralizedSequentialLinda();
	final static Linda linda = new CentralizedLinda();
	// final static Linda linda = new linda.server.LindaClient("//localhost:4000/MonServeur");
	public static void main(String args[]) throws InterruptedException {
		System.err.close();
		limite = Integer.parseInt(args[0]);
		nbThreads = Integer.parseInt(args[1]);
		sqrt = Math.sqrt((double) limite);
		ExecutorService pool = Executors.newFixedThreadPool(nbThreads);
		
		
		init();
		long debutMulti = System.nanoTime();
		for(int premier = 2; premier <= sqrt; premier++) {
			Thread envoi = new ThreadSecondaire(premier);
			pool.execute(envoi);
		}
		pool.shutdown();
		
		try {
	        if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
	            pool.shutdownNow();
	        }
	    } catch (InterruptedException ex) {
	        ex.printStackTrace();
	    }
		long finMulti = System.nanoTime();
		System.out.print("Avec " + ((Integer)nbThreads).toString() + " Threads : ");
		System.out.println(finMulti - debutMulti);
		//count(); Ne décommentez que pour vérifier le nombre de premeir que pour n<100 000
		
		init();
		long debutMono = System.nanoTime();
		cribler(2);
		long finMono = System.nanoTime();
		System.out.print("Avec 1 Thread : ");
		System.out.println(finMono - debutMono);
		//count(); Ne décommentez que pour vérifier le nombre de premeir que pour n<100 000
		

		
		System.out.println("");
		System.out.print("Différence en nano-secondes (temps monoThread - temps multiThread) : ");
		System.out.println((finMono-debutMono) - (finMulti-debutMulti));

	}
	
	
	public static void init() {
		for (int depot = 2; depot<= limite; depot++) {
			linda.write(new Tuple(depot));
		}
	}
	
	public static void count() {
		int count = 0;
		for (int depot = 2; depot<= limite; depot++) {
			Tuple recup = linda.tryTake(new Tuple(depot));
			
			if(recup != null) {
				count++;
			}
		}
		System.out.print("Nombre de premiers : ");
		System.out.println(count);
	}
	
	static class ThreadSecondaire extends Thread{
		int nombre;
		public ThreadSecondaire(int nombre) {
			this.nombre = nombre;
		}
		
		public void run() {
			for (int i = 2; i<= limite/nombre; i++) {
				Tuple multiple = linda.tryTake(new Tuple((int)i*nombre));
			}
		}
	}

	public static void cribler(double debut) {
		for (double i = debut; i<= sqrt; i++) {
			Tuple prime = linda.tryRead(new Tuple((int)i));
			if (prime!=null) {
				for (int j=2; j<= 1 + limite/i; j++) {
					Tuple multiple = linda.tryTake(new Tuple((int)i*j));
				}
			}
		}
	}
}