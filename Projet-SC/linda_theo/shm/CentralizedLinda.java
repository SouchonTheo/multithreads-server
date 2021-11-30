package linda.shm;

import java.util.Collection;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {

    private Collection<Tuple> listTuples;

    public CentralizedLinda() {
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tuple take(Tuple template) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tuple tryTake(Tuple template) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void write(Tuple t) {
        this.listTuples.add(t);
    }

    // TO BE COMPLETED

}
