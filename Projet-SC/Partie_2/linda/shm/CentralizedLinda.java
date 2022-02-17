package linda.shm;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
				}
			}
		}
		return listICallbacks;
	}

	public InternalCallback getFirstTaker(Tuple t) {
		InternalCallback takeCb = null;
		if (this.readers.size() > 0) {
			Iterator<InternalCallback> iterator = this.takers.iterator();
			while (iterator.hasNext()) {
				InternalCallback iCallback = iterator.next();
				if (iCallback.getTemplate().matches(t)) {
					takeCb = iCallback;
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
					System.out.println();
					this.takeConditions.get(size).await();
					this.nbTakeWaiting--;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("take monitor unlock");
		monitor.unlock();
		return ret;
	}

	@Override
	public Tuple read(Tuple template) {
		System.out.println("read monitor lock");
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
		System.out.println("read monitor unlock");
		monitor.unlock();
		return ret;
	}

	@Override
	public Tuple tryTake(Tuple template) {
		System.out.println("tryTake monitor lock");
		monitor.lock();
		Tuple ret = null;
		Iterator<Tuple> iterator = this.listTuples.iterator();
		while (iterator.hasNext()) {
			ret = iterator.next();
			if (ret.matches(template)) {
				this.listTuples.remove(ret);
				System.out.println("tryTake monitor unlock");
				monitor.unlock();
				return ret;
			}
		}
		System.out.println("tryTake monitor unlock");
		monitor.unlock();
		return null;
	}

	@Override
	public Tuple tryRead(Tuple template) {
		System.out.println("tryRead monitor lock");
		monitor.lock();
		Tuple ret = null;
		Iterator<Tuple> iterator = this.listTuples.iterator();
		while (iterator.hasNext()) {
			ret = iterator.next();
			if (ret.matches(template)) {
				System.out.println("tryread monitor unlock");
				monitor.unlock();
				return ret;
			}
		}
		System.out.println("tryRead monitor unlock");
		monitor.unlock();
		return null;
	}

	@Override
	public Collection<Tuple> takeAll(Tuple template) {
		System.out.println("takeAll monitor lock");
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
		System.out.println("takeAll monitor unlock");
		monitor.unlock();
		return collectionTuples;
	}

	@Override
	public Collection<Tuple> readAll(Tuple template) {
		System.out.println("readAll monitor lock");
		monitor.lock();
		Collection<Tuple> collectionTuples = new Vector<Tuple>();
		for (Tuple t : this.listTuples) {
			if (t.matches(template)) {
				collectionTuples.add(t);
			}
		}
		System.out.println("readAll monitor unlock");
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
