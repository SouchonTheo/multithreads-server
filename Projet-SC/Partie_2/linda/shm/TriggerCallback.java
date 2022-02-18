package linda.shm;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import linda.Callback;
import linda.Tuple;

public class TriggerCallback implements Callback {

    private Condition condition;

    private Tuple tuple;

    private ReentrantLock monitor;

    public TriggerCallback(Condition condition, ReentrantLock monitor) {
        this.condition = condition;
        this.monitor = monitor;
    }

    public void call(Tuple t) {
        this.tuple = t;
        this.monitor.lock();
        this.condition.signal();
        this.monitor.unlock();
    }

    public Tuple getTuple() {
        return this.tuple;
    }

}
