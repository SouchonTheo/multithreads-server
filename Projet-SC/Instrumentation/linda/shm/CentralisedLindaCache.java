package linda.shm;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import linda.Tuple;
import linda.Callback;
import linda.Linda;
import linda.InternalCallback;

public class CentralisedLindaCache extends LindaInstru {

	public CentralisedLindaCache() throws RemoteException {
		super();
		listTuples = new Vector<Tuple>();
		readers = new Vector<InternalCallback>();
		takers = new Vector<InternalCallback>();
		monitor = new ReentrantLock();
	}

	private Boolean notNull(Tuple t) {
		if (t == null) {
			return false;
		} else {
			Boolean notNull = true;
			Integer k = 0;
			while (k < t.size() && notNull) {
				if (t.get(k) == null) {
					notNull = false;
				}
				k++;
			}
			return notNull;
		}
	}

	private Vector<InternalCallback> getReaders(Tuple t) {
		Vector<InternalCallback> listICallbacks = new Vector<InternalCallback>();
		if (this.readers.size() > 0) {
			Iterator<InternalCallback> iterator = this.readers.iterator();
			while (iterator.hasNext()) {
				InternalCallback iCallback = iterator.next();
				if (iCallback.getTemplate().contains(t)) {
					listICallbacks.add(iCallback);
					this.takers.remove(iCallback);
				}
			}
		}
		return listICallbacks;
	}

	private InternalCallback getFirstTaker(Tuple t) {
		InternalCallback takeCb = null;
		if (this.takers.size() > 0) {
			Iterator<InternalCallback> iterator = this.takers.iterator();
			while (iterator.hasNext()) {
				InternalCallback iCallback = iterator.next();
				if (iCallback.getTemplate().contains(t)) {
					takeCb = iCallback;
					this.takers.remove(iCallback);
					break;
				}
			}
		}
		return takeCb;
	}


	private Tuple search(Tuple template) {
		Tuple ret = null;
        Iterator<Tuple> iterator = listTuples.iterator();
        while (iterator.hasNext()) {
            ret = iterator.next();
            if (ret.matches(template)) {
                break;
            }
			ret = null;
        }
		return ret;
	}
	@Override
	public void write(Tuple t) {
		if (notNull(t)) {
			monitor.lock();
			// On regarde si ce tuple est voulu par des read ou take
			Vector<InternalCallback> listICallbacks = getReaders(t);
			InternalCallback takeCallback = getFirstTaker(t);
			if (takeCallback == null) {
				this.listTuples.add(t);
			}
			monitor.unlock();

			// On appelle les read
			if (listICallbacks.size() > 0) {
				Iterator<InternalCallback> iterator = listICallbacks.iterator();
				while (iterator.hasNext()) {
					InternalCallback iCallback = iterator.next();
					iCallback.getCallback().call(t);
				}
			}
			if (takeCallback != null) {
				// On appelle le take
				takeCallback.getCallback().call(t);
			}
		} else {
			// On peut pas rajouter null à notre espace de tuple
			throw new IllegalStateException();
		}
	}

	@Override
	public Tuple take(Tuple template) {
		monitor.lock();
		// on cherche le tuple dans l'espace
		Tuple ret = search(template);
		this.listTuples.remove(ret);
		// Si on ne le trouve pas on enregistre le callback et attend sa réponse
		if (ret == null) {
			Condition condition = monitor.newCondition();
			TriggerCallback tCb = new TriggerCallback(condition, monitor);
			this.takers.add(new InternalCallback(template, tCb));
			try {
				condition.await();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			ret = tCb.getTuple();
		}
		monitor.unlock();
		return ret;
	}

	@Override
	public Tuple read(Tuple template) {
		monitor.lock();
		// on cherche le tuple dans l'espace
		Tuple ret = search(template);
		if (ret == null) {
			// Si on ne le trouve pas on enregistre le callback puis on attend sa réponse
			Condition condition = monitor.newCondition();
			TriggerCallback tCb = new TriggerCallback(condition, monitor);
			
			this.readers.add(new InternalCallback(template, tCb));
			try {
				condition.await();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			ret = tCb.getTuple();
		}		
		monitor.unlock();
		return ret;
	}

	@Override
	public Tuple tryTake(Tuple template) {
		monitor.lock();
		Tuple ret = search(template);
		if (ret != null)
			this.listTuples.remove(ret);
		monitor.unlock();
		return ret;
	}

	@Override
	public Tuple tryRead(Tuple template) {
		monitor.lock();
		Tuple ret = search(template);
		monitor.unlock();
		return ret;
	}

	@Override
	public Collection<Tuple> takeAll(Tuple template) {
		monitor.lock();
		// On collecte tous les tuples correspondant au template
		Collection<Tuple> collectionTuples = new Vector<Tuple>();
		Iterator<Tuple> iterator = this.listTuples.iterator();
		Tuple ret = null;
		while (iterator.hasNext()) {
			ret = iterator.next();
			if (ret.matches(template)) {
				collectionTuples.add(ret);
				this.listTuples.remove(ret);
			}
		}
		monitor.unlock();
		return collectionTuples;
	}

	@Override
	public Collection<Tuple> readAll(Tuple template) {
		// On vérifie si on peut lire (pas d'écriture ou prise en cours + temps non
		// écoulé)
		monitor.lock();
		Collection<Tuple> collectionTuples = new Vector<Tuple>();
		Iterator<Tuple> iterator = this.listTuples.iterator();
		Tuple ret = null;
		while (iterator.hasNext()) {
			ret = iterator.next();
			if (ret.matches(template)) {
				collectionTuples.add(ret);
			}
		}
		monitor.unlock();
		return collectionTuples;
	}

	@Override
	public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
		if (timing.equals(Linda.eventTiming.IMMEDIATE)) {
			Tuple t = null;
			if (mode.equals(Linda.eventMode.READ)) {
				t = tryRead(template);
				if (t != null) {
					callback.call(t);
				} else {
					monitor.lock();
					this.readers.add(new InternalCallback(template, callback));
					monitor.unlock();
				}
			} else {
				t = tryTake(template);
				if (t != null) {
					callback.call(t);
				} else {
					monitor.lock();
					this.takers.add(new InternalCallback(template, callback));
					monitor.unlock();
				}
			}
		} else {
			monitor.lock();
			if (mode.equals(Linda.eventMode.READ)) {
				this.readers.add(new InternalCallback(template, callback));
			} else {
				this.takers.add(new InternalCallback(template, callback));
			}
			monitor.unlock();
		}
	}

	@Override
	public void debug(String prefix) {
		System.out.println(prefix + " On entre dans debug !");
	}
}
