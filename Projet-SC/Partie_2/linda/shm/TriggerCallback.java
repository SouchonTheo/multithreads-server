package linda.shm;

import java.util.concurrent.locks.Condition;

import linda.Callback;
import linda.Tuple;

public class TriggerCallback implements Callback {

    private Condition condition;

    private Tuple tuple;

    public TriggerCallback(Condition condition) {
        this.condition = condition;
    }

    public void call(Tuple t) {
        this.tuple = t;
        this.condition.signal();
    }

    public Tuple getTuple() {
        return this.tuple;
    }
    
}
