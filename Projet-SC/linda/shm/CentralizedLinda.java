package linda.shm;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.security.auth.callback.Callback;

import linda.Linda;
import linda.Tuple;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {

	private Vector<Tuple> listTuples;
	private Map<Tuple, Map<Linda.eventMode, Vector<Callback>>> callbacksRegistered;
	private ReentrantLock monitor;
	private Condition readCondition;
	private Condition takeCondition;
	private int nbReadWaiting;
	private int nbTakeWaiting;

	public CentralizedLinda() {
		listTuples = new Vector<Tuple>();
		callbacksRegistered = new Map<Tuple, Map<Linda.eventMode, Vector<Callback>>>();
		monitor = new ReentrantLock();
		readCondition = monitor.newCondition();
		takeCondition = monitor.newCondition();
		nbReadWaiting = 0;
		nbTakeWaiting = 0;
	}

	private void addCallback(Tuple t, Linda.eventMode mode, Callback callback) {
		Map<Linda.eventMode, Vector<Callback>> mapEventMode = this.callbacksRegistered.get(t);
		Vector<Callback> vectorCallback = new Vector<Callback>();
		if (mapEventMode == null) {
			mapEventMode = new HashMap<Linda.eventMode, Vector<Tuple>>();
			vectorCallback.add(callback);
			mapEventMode.put(mode, vectorCallback);
		} else {
			if (mapEventMode.containsKey(mode)) {
				vectorCallback = mapEventMode.get(mode);
			}
			vectorCallback.add(callback);
		}
		mapEventMode.put(mode, vectorCallback);
		this.callbacksRegistered.put(t, mapEventMode);
	}

	private void removeCallback(Tuple t, Linda.eventMode mode, Callback callback) {
		Map<Linda.eventMode, Vector<Callback>> mapEventMode = this.callbacksRegistered.get(t);
		Vector<Callback> vectorCallback = mapEventMode.get(mode);
		vectorCallback.remove(callback);
		mapEventMode.put(mode, vectorCallback);
		this.callbacksRegistered.put(t, mapEventMode);
	}

	private void CheckCallbacks(Tuple t, Linda.eventMode mode) {
		Map<Linda.eventMode, Vector<Callback>> mapEventMode = this.callbacksRegistered.get(t);
		if (mapEventMode.containsKey(mode)) {
			Vector<Callback> vectorCallback = mapEventMode.get(mode);
			for (Callback c : vectorCallback) {
				c.call(t);
				this.callbacksRegistered.remove(t, mode, c);
			}
		}
		if (mode.equals(Linda.eventMode.TAKE)) {
			this.listTuples.remove(t);
		}
	}

	@Override
	public void write(Tuple t) {
		moniteur.lock();
		this.listTuples.add(t);
		// On vérifie les read en premiers
		if (this.nbReadWaiting > 0) {
			this.readCondition.notifyAll();
		} // Puis tous les callbacks en read
		for (Tuple tuple : this.callbacksRegistered.keys()) {
			if (t.matches(tuple)) {
				CheckCallbacks(t, Linda.eventMode.READ);
			}
		} // Ensuite tous les callbacks en take
		for (Tuple tuple : this.callbacksRegistered.keys()) {
			if (t.matches(tuple)) {
				CheckCallbacks(t, Linda.eventMode.TAKE);
			}
		} // Et enfin les take
		if (this.nbTakeWaiting > 0) {
			this.takeCondition.notifyAll();
		}
		moniteur.unlock();
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
				this.nbTakeWaiting++;
				this.takeCondition.await();
				this.nbTakeWaiting--;
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
				this.nbReadWaiting++;
				this.readCondition.await();
				this.nbReadWaiting--;
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
		if (timing.equals(Linda.eventTiming.IMMEDIATE)) {
			Tuple t = null;
			if (mode.equals(Linda.eventMode.READ)) {
				t = tryRead(template);
			} else {
				t = tryTake(template);
			}
			if (t != null) {
				callback.call(t);
			} else {
				this.callbacksRegistered.add(template, callback);
			}
		} else {
			this.callbacksRegistered.add(template, callback);
		}
		monitor.unlock();

	}

	@Override
	public void debug(String prefix) {
		monitor.lock();
		monitor.unlock();

	}

}
