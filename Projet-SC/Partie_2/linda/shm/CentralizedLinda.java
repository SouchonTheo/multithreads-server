package linda.shm;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import linda.Tuple;
import linda.Callback;
import linda.Linda;
import linda.InternalCallback;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {

	private Vector<Tuple> listTuples;
	private Vector<InternalCallback> readers;
	private Vector<InternalCallback> takers;
	private ReentrantLock monitor;
	private Vector<Condition> readConditions;
	private Vector<Condition> takeConditions;
	private Condition wait;
	private int nbReadWaiting;
	private int nbTakeWaiting;

	public CentralizedLinda() {
		listTuples = new Vector<Tuple>();
		readers = new Vector<InternalCallback>();
		takers = new Vector<InternalCallback>();
		monitor = new ReentrantLock();
		readConditions = new Vector<Condition>();
		takeConditions = new Vector<Condition>();
		wait = monitor.newCondition();
		nbReadWaiting = 0;
		nbTakeWaiting = 0;
	}

	private Boolean notNull(Tuple t) {
		Boolean notNull = t != null;
		Integer k = 0;
		while (k < t.size() && notNull) {
			if (t.get(k) == null) {
				notNull = false;
			}
			k++;
		}
		return notNull;
	}

	public Vector<InternalCallback> getReaders(Tuple t) {
		Vector<InternalCallback> listICallbacks = new Vector<InternalCallback>();
		if (this.readers.size() > 0) {
			Iterator<InternalCallback> iterator = this.readers.iterator();
			while (iterator.hasNext()) {
				InternalCallback iCallback = iterator.next();
				if (iCallback.getTemplate().matches(t)) {
					listICallbacks.add(iCallback);
					this.takers.remove(iCallback);
				}
			}
		}
		return listICallbacks;
	}

	public InternalCallback getFirstTaker(Tuple t) {
		InternalCallback takeCb = null;
		if (this.takers.size() > 0) {
			Iterator<InternalCallback> iterator = this.takers.iterator();
			while (iterator.hasNext()) {
				InternalCallback iCallback = iterator.next();
				if (iCallback.getTemplate().matches(t)) {
					takeCb = iCallback;
					this.takers.remove(iCallback);
					break;
				}
			}
		}
		return takeCb;
	}

	@Override
	public void write(Tuple t) {
		monitor.lock();
		if (notNull(t)) {
			Vector<InternalCallback> listICallbacks = getReaders(t);
			InternalCallback takeCallback = getFirstTaker(t);
			if (takeCallback == null){
				this.listTuples.add(t);
			}
			//monitor.unlock();
			// On vérifie les read en premiers
			if (listICallbacks.size() > 0) {
				Iterator<InternalCallback> iterator = listICallbacks.iterator();
				while (iterator.hasNext()) {
					InternalCallback iCallback = iterator.next();
					if (iCallback.getTemplate().matches(t)) {
						iCallback.getCallback().call(t);
					}
				}
			}
			// Puis on appelle le take
			if (takeCallback != null) {
				takeCallback.getCallback().call(t);
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
		// on cherche le tuple dans l'eespace
		Tuple ret = null;
		Iterator<Tuple> iterator = this.listTuples.iterator();
		while (iterator.hasNext()) {
			ret = iterator.next();
			if (ret.matches(template)) {
				this.listTuples.remove(ret);
				return ret;
			}
		}

		// Si on ne le trouve pas on enregistre le callback
		Condition condition = monitor.newCondition();
		TriggerCallback tCb = new TriggerCallback(condition);

		this.takers.add(new InternalCallback(template, tCb));
		monitor.unlock();
		try {
			condition.wait();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		return tCb.getTuple();
	}

	@Override
	public Tuple read(Tuple template) {
		monitor.lock();
		// on cherche le tuple dans l'eespace
		Tuple ret = null;
		Iterator<Tuple> iterator = this.listTuples.iterator();
		while (iterator.hasNext()) {
			ret = iterator.next();
			if (ret.matches(template)) {
				return ret;
			}
		}

		// Si on ne le trouve pas on enregistre le callback
		Condition condition = monitor.newCondition();
		TriggerCallback tCb = new TriggerCallback(condition);

		this.readers.add(new InternalCallback(template, tCb));
		monitor.unlock();
		try {
			condition.wait();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		return tCb.getTuple();
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
				if (t != null) {
					callback.call(t);
				} else {
					this.readers.add(new InternalCallback(template, callback));
				}
			} else {
				t = tryTake(template);
				if (t != null) {
					callback.call(t);
				} else {
					this.takers.add(new InternalCallback(template, callback));
				}
			}
		} else {
			if (mode.equals(Linda.eventMode.READ)) {
				this.readers.add(new InternalCallback(template, callback));
			} else {
				this.takers.add(new InternalCallback(template, callback));
			}
		}
		monitor.unlock();
	}

	@Override
	public void debug(String prefix) {
		System.out.println(prefix + " On entre dans debug !");
	}

}
