package linda;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LaunchAction extends Thread {

    private Linda linda;
    private Method method;
    private Tuple tuple;
    private Integer nbLoops;
    private boolean file;

    public LaunchAction (Linda linda, Method m, Tuple t, Integer nbLoop, boolean file){
        this.linda = linda;
        this.method = m;
        this.tuple = t;
        this.nbLoops = nbLoop;
        this.file = file;
    }

    public void run() {
        for (int i = 0; i < nbLoops; i++){
            try {
                method.invoke(linda,tuple);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        if (!file) System.out.println("On a terminé de " + method.getName() + " le tuple " + tuple + " " + nbLoops + " fois.\n");
    }
}
