package linda.shm;

import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import linda.InternalCallback;
import linda.Linda;
import linda.Tuple;

public abstract class LindaInstru implements Linda {
    
    protected Vector<Tuple> listTuples;
	protected Vector<InternalCallback> readers;
	protected Vector<InternalCallback> takers;
	protected ReentrantLock monitor;
	protected int nbThreads;
	protected int nbTakeWaiting;
	protected int nbWriteWaiting;

	protected LindaInstru () {
		listTuples = new Vector<Tuple>();
		readers = new Vector<InternalCallback>();
		takers = new Vector<InternalCallback>();
		monitor = new ReentrantLock();
		nbThreads = 2;
		nbTakeWaiting = 0;
		nbWriteWaiting = 0;
	}


    public Vector<Tuple> getListTuple() {
		monitor.lock();
		Vector<Tuple> vec = (Vector<Tuple>) this.listTuples.clone();
		monitor.unlock();
		return vec;
	}

	public int getNbReadBlocked() {
		return this.readers.size();
	}
	public int getNbTakeBlocked() {
		return this.takers.size();
	}
	public int getNbTakeWaiting() {
		return this.nbTakeWaiting;
	}
	public int getNbWriteWaiting() {
		return this.nbWriteWaiting;
	}
	public int getNbTuples() {
		return this.listTuples.size();
	}
}
