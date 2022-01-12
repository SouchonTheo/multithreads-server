package linda.shm;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import linda.Linda;
import linda.Tuple;
import linda.Callback;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {

	private Vector<Tuple> listTuples;
	private Map<Tuple, Map<eventMode, Vector<Callback>>> callbacksRegistered;
	private ReentrantLock monitor;
	private Vector<Condition> readConditions;
	private Vector<Condition> takeConditions;
	private Condition wait;
	private int nbReadWaiting;
	private int nbTakeWaiting;

	public CentralizedLinda() {
		listTuples = new Vector<Tuple>();
		callbacksRegistered = new HashMap<Tuple, Map<Linda.eventMode, Vector<Callback>>>();
		monitor = new ReentrantLock();
		readConditions = new Vector<Condition>();
		takeConditions = new Vector<Condition>();
		wait = monitor.newCondition();
		nbReadWaiting = 0;
		nbTakeWaiting = 0;
	}

	private void addCallback(Tuple tupleTemplate, Linda.eventMode mode, Callback callback) {
		Map<Linda.eventMode, Vector<Callback>> mapEventMode = this.callbacksRegistered.get(tupleTemplate);
		Vector<Callback> vectorCallback = new Vector<Callback>();
		if (mapEventMode == null) {
			mapEventMode = new HashMap<Linda.eventMode, Vector<Callback>>();
			vectorCallback.add(callback);
			mapEventMode.put(mode, vectorCallback);
		} else {
			if (mapEventMode.containsKey(mode)) {
				vectorCallback = mapEventMode.get(mode);
			}
			vectorCallback.add(callback);
		}
		mapEventMode.put(mode, vectorCallback);
		this.callbacksRegistered.put(tupleTemplate, mapEventMode);
	}

	private void removeCallback(Tuple tupleExact, Linda.eventMode mode, Callback callback) {
		Map<Linda.eventMode, Vector<Callback>> mapEventMode = this.callbacksRegistered.get(tupleExact);
		Vector<Callback> vectorCallback = mapEventMode.get(mode);
		vectorCallback.remove(callback);
		mapEventMode.put(mode, vectorCallback);
		this.callbacksRegistered.put(tupleExact, mapEventMode);
	}

	private void CheckCallbacksRead(Tuple tupleExact) {
		for (Tuple tupleTemplate : this.callbacksRegistered.keySet()) {
			if (tupleTemplate.contains(tupleExact)) {
				Map<Linda.eventMode, Vector<Callback>> mapEventMode = this.callbacksRegistered.get(tupleTemplate);
				if (mapEventMode.containsKey(eventMode.READ)) {
					Vector<Callback> vectorCallback = mapEventMode.get(eventMode.READ);
					int taille = vectorCallback.size();
					for (int i = 0; i < taille; i++) {
						Callback c = vectorCallback.get(i);
						removeCallback(tupleTemplate, eventMode.READ, c);
						c.call(tupleExact);
					}
				}
			}
		}
	}

	private void CheckCallbacksTake(Tuple tupleExact) {
		for (Tuple tupleTemplate : this.callbacksRegistered.keySet()) {
			if (tupleTemplate.contains(tupleExact)) {
				Map<Linda.eventMode, Vector<Callback>> mapEventMode = this.callbacksRegistered.get(tupleTemplate);
				if (mapEventMode.containsKey(eventMode.TAKE)) {
					Vector<Callback> vectorCallback = mapEventMode.get(eventMode.TAKE);
					if (listTuples.contains(tupleExact)) {
						Callback c = vectorCallback.get(0);
						removeCallback(tupleTemplate, eventMode.TAKE, c);
						this.listTuples.remove(tupleExact);
						c.call(tupleExact);
					}
				}
			}
		}
	}

	@Override
	public void write(Tuple t) {
		monitor.lock();
		Boolean notNull = t != null;
		Integer k = 0;
		while (k < t.size() && notNull) {
			if (t.get(k) == null) {
				notNull = false;
			}
			k++;
		}
		if (notNull) {
			this.listTuples.add(t);
			// On vérifie les read en premiers
			if (this.nbReadWaiting > 0) {
				int size = this.readConditions.size();
				for (int i = 0; i < size; i++) {
					Condition cond = this.readConditions.get(0);
					this.readConditions.remove(0);
					cond.signal();
					try {// On passe la main au read
						this.wait.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} // Puis tous les callbacks en read
			for (Tuple tuple : this.callbacksRegistered.keySet()) {
				if (t.matches(tuple)) {
					CheckCallbacksRead(t);
				}
			} // Ensuite tous les take
			if ((this.nbReadWaiting == 0) && (this.nbTakeWaiting > 0)) {
				int size = this.takeConditions.size();
				for (int i = 0; i < size; i++) {
					Condition cond = this.takeConditions.get(0);
					this.takeConditions.remove(0);
					cond.signal();
				}
			} // Et enfin les callback take
			for (Tuple tuple : this.callbacksRegistered.keySet()) {
				if (t.matches(tuple)) {
					CheckCallbacksTake(t);
				}
			}

		} else {
			// On peut pas rajouter null à notre espace de tuple
			throw new IllegalStateException();
		}
		monitor.unlock();
	}

	@Override
	public Tuple take(Tuple template) {
		monitor.lock();
		Tuple ret = null;
		boolean continueLoop = true;
		while (continueLoop) {
			Iterator<Tuple> iterator = this.listTuples.iterator();
			while (continueLoop && iterator.hasNext()) {
				ret = iterator.next();
				if (ret.matches(template)) {
					this.listTuples.remove(ret);
					continueLoop = false;
				}
			}
			if (continueLoop) {
				try {
					this.nbTakeWaiting++;
					Condition takeCondition = monitor.newCondition();
					int size = this.takeConditions.size();
					this.takeConditions.add(takeCondition);
					this.takeConditions.get(size).await();
					this.nbTakeWaiting--;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
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
			Iterator<Tuple> iterator = this.listTuples.iterator();
			while (continueLoop && iterator.hasNext()) {
				ret = iterator.next();
				if (ret.matches(template)) {
					continueLoop = false;
				}
			}
			if (continueLoop) {
				try {
					this.nbReadWaiting++;
					this.wait.signal();
					Condition readCondition = monitor.newCondition();
					int size = this.readConditions.size();
					this.readConditions.add(readCondition);
					this.readConditions.get(size).await();
					this.nbReadWaiting--;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		if (this.nbTakeWaiting > 0 && this.nbReadWaiting == 0) {
			Condition cond = this.takeConditions.get(0);
			this.takeConditions.remove(0);
			cond.signal();
		}
		this.wait.signal();
		monitor.unlock();
		return ret;
	}

	@Override
	public Tuple tryTake(Tuple template) {
		monitor.lock();
		Tuple ret = null;
		Iterator<Tuple> iterator = this.listTuples.iterator();
		while (iterator.hasNext()) {
			ret = iterator.next();
			if (ret.matches(template)) {
				this.listTuples.remove(ret);
				monitor.unlock();
				return ret;
			}
		}
		monitor.unlock();
		return null;
	}

	@Override
	public Tuple tryRead(Tuple template) {
		monitor.lock();
		Tuple ret = null;
		Iterator<Tuple> iterator = this.listTuples.iterator();
		while (iterator.hasNext()) {
			ret = iterator.next();
			if (ret.matches(template)) {
				monitor.unlock();
				return ret;
			}
		}
		monitor.unlock();
		return null;
	}

	@Override
	public Collection<Tuple> takeAll(Tuple template) {
		monitor.lock();
		Collection<Tuple> collectionTuples = new Vector<Tuple>();
		Tuple t = null;
		int size = this.listTuples.size();
		int j = 0;
		for (int i = 0; i < size; i++) {
			t = this.listTuples.get(j);
			if (t.matches(template)) {
				collectionTuples.add(t);
				this.listTuples.remove(t);
			} else {
				j++;
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
				addCallback(template, mode, callback);
			}
		} else {
			addCallback(template, mode, callback);
		}
		monitor.unlock();
	}

	@Override
	public void debug(String prefix) {
		System.out.println(prefix + " On entre dans debug !");
	}

}
