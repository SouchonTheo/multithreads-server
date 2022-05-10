package linda.shm;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;

import linda.Tuple;

public class SearchList extends Thread {
    
    private List<Tuple> listTuples;
    private Tuple template;
    private Tuple result;
    private Semaphore sem;

    public SearchList(List<Tuple> listTuples, Tuple template, Semaphore sem) {
        this.listTuples = listTuples;
        this.template = template;
        this.sem = sem;
        this.result = null;
    }

    public void run() {
        Tuple ret = null;
        Iterator<Tuple> iterator = listTuples.iterator();
        while (iterator.hasNext()) {
            ret = iterator.next();
            if (ret.matches(template)) {
                result = ret;
            }
        }
        sem.release();
    }

    public Tuple getResult() {
        return this.result;
    }
}