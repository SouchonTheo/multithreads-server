package linda.shm;

import java.util.Collection;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {

    private Vector<Tuple> listTuples;
    private Lock moniteur;
    private Condition readCondition;
    private Condition takeCondition;
    private int nbReadWaiting;
    private int nbTakeWaiting;

    public CentralizedLinda() {
        // moniteur et condition
        this.moniteur = new ReentrantLock();
        this.readCondition = new moniteur.newCondition();
        this.takeCondition = new moniteur.newCondition();
        // Compteurs
        this.nbReadWaiting = 0;
        this.nbTakeWaiting = 0;
        // Vector
        this.listTuples = new Vector<Tuple>();
    }

    @Override
    public void debug(String prefix) {
        // TODO Auto-generated method stub

    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
        // TODO Auto-generated method stub

    }

    @Override
    public Tuple read(Tuple template) {
        moniteur.lock();
        moniteur.unlock();
        return null;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        moniteur.lock();
        moniteur.unlock();
        return null;
    }

    @Override
    public Tuple take(Tuple template) {
        moniteur.lock();
        moniteur.unlock();
        return null;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        moniteur.lock();
        moniteur.unlock();
        return null;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        moniteur.lock();
        moniteur.unlock();
        return null;
    }

    @Override
    public Tuple tryTake(Tuple template) {
        moniteur.lock();
        moniteur.unlock();
        return null;
    }

    @Override
    public void write(Tuple t) {
        moniteur.lock();
        this.listTuples.add(t);
        if (this.nbTakeWaiting > 0) {
            if (this.nbReadWaiting > 0) {
                this.readCondition.notifyAll();
            } else {
                this.takeCondition.notifyAll();
            }
        } else if (this.nbReadWaiting > 0) {
            this.readCondition.notifyAll();
        }
        moniteur.unlock();
    }

    // TO BE COMPLETED

}
