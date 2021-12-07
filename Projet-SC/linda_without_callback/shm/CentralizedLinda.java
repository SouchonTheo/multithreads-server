ppackage linda.shm;

import java.util.Collection;
import java.util.Vector;

import java.util.concurrent.locks.*;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {
	
	private Vector<Tuple> listTuples;
    private ReentrantLock monitor;
    private Condition readCondition;
    private Condition takeCondition;
    private int nbReadWaiting;
    private int nbTakeWaiting;

    public CentralizedLinda() {
    	listTuples = new Vector<Tuple>();
    	monitor = new ReentrantLock();
    	readCondition = monitor.newCondition();
    	takeCondition = monitor.newCondition();
    	nbReadWaiting = 0;
    	nbTakeWaiting = 0;
    }

	@Override
	public void write(Tuple t) {
		monitor.lock();
		this.listTuples.add(t);
		monitor.unlock();
	}
	

	@Override
	public Tuple take(Tuple template) {
		monitor.lock();
		Tuple ret = null;
		boolean continueLoop = true;
		while (continueLoop) {
			int i = this.listTuples.size();
			Tuple t = null;
			while (ret == null && i > 0) {
				t = this.listTuples.get(i);
	    		if (t.matches(template)) {
	    			this.listTuples.remove(t);
	    			ret = t;
	    			continueLoop = false;
	    		}
	    		i--;
	    	}
			try {
				this.takeCondition.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}		
		monitor.unlock();
		return ret;
	}

	@Override
	public Tuple read(Tuple template) {
		monitor.lock();
		Tuple ret = null;
		boolean continueLoop = true;
		while (continueLoop) {
			int i = this.listTuples.size();
			Tuple t = null;
			while (ret == null && i > 0) {
				t = this.listTuples.get(i);
	    		if (t.matches(template)) {
	    			ret = t;
	    			continueLoop = false;
	    		}
	    		i--;
	    	}
			try {
				this.readCondition.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}		
		monitor.unlock();
		if (this.nbTakeWaiting > 0 && this.nbReadWaiting == 0) {
			this.takeCondition.signalAll();
		}
		return ret;
	}

	@Override
	public Tuple tryTake(Tuple template) {
		monitor.lock();
		Tuple ret = null;
		int i = this.listTuples.size();
		Tuple t = null;
		while (ret == null && i > 0) {
			t = this.listTuples.get(i);
    		if (t.matches(template)) {
    			this.listTuples.remove(t);
    			ret = t;
    		}
    		i--;
    	}
		monitor.unlock();
		return ret;
	}

	@Override
	public Tuple tryRead(Tuple template) {
		monitor.lock();
		Tuple ret = null;
		int i = this.listTuples.size();
		Tuple t = null;
		while (ret == null && i > 0) {
			t = this.listTuples.get(i);
    		if (t.matches(template)) {
    			ret = t;
    		}
    		i--;
    	}
		monitor.unlock();
		return ret;
	}

	@Override
	public Collection<Tuple> takeAll(Tuple template) {
		monitor.lock();
		Collection<Tuple> collectionTuples = new Vector<Tuple>();
		for (Tuple t : this.listTuples) {
    		if (t.matches(template)) {
    			collectionTuples.add(t);
    			this.listTuples.remove(t);
    		}
    	}
		monitor.unlock();
		return collectionTuples;
	}

	@Override
	public Collection<Tuple> readAll(Tuple template) {
		monitor.lock();
		Collection<Tuple> collectionTuples = new Vector<Tuple>();
		for (Tuple t : this.listTuples) {
    		if (t.matches(template)) {
    			collectionTuples.add(t);
    		}
    	}
		monitor.unlock();
		return collectionTuples;
	}

	@Override
	public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
		monitor.lock();
		monitor.unlock();
		
	}

	@Override
	public void debug(String prefix) {
		// TODO Auto-generated method stub
		monitor.lock();
		monitor.unlock();
		
	}


}
