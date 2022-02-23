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
	private Boolean timerLaunched;
	private Boolean timeout;
	private Boolean writing;
	private Boolean taking;
	private int nbCurrentReaders;
	private int nbReadWaiting;
	private int nbTakeWaiting;
	private int nbWriteWaiting;
	private Condition canRead;
	private Condition canTake;
	private Condition canWrite;

	public CentralizedLinda() {
		listTuples = new Vector<Tuple>();
		readers = new Vector<InternalCallback>();
		takers = new Vector<InternalCallback>();
		monitor = new ReentrantLock();
		timerLaunched = false;
		timeout = false;
		writing = false;
		taking = false;
		nbCurrentReaders = 0;
		nbReadWaiting = 0;
		nbTakeWaiting = 0;
		nbWriteWaiting = 0;
		canRead = monitor.newCondition();
		canTake = monitor.newCondition();
		canWrite = monitor.newCondition();
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

	private void launchTimer() {
		if (!timerLaunched) {
			timerLaunched = true;
			new Thread() {
				public void run() {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (!timerLaunched) {
						timeout = true;
					}
					timerLaunched = false;
				}
			}.start();
		}
	}

	@Override
	public void write(Tuple t) {
		if (notNull(t)) {
			// On vérifie qu'on peut écrire, cad pas de lecteur ou de take ou write.
			// On ne vérifie pas les writing ou taking car on est dans le moniteur donc il
			// est forcément seul.
			monitor.lock();
			if (nbCurrentReaders > 0) {
				try {
					nbWriteWaiting++;
					launchTimer();
					System.out.println("le write attend");
					canWrite.await();
					timerLaunched = false;
					writing = true;
					nbWriteWaiting--;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			// On regarde si ce tuple est voulu par des read ou take (seule partie
			// concurrente)
			Vector<InternalCallback> listICallbacks = getReaders(t);
			InternalCallback takeCallback = getFirstTaker(t);
			if (takeCallback == null) {
				this.listTuples.add(t);
			}

			// On passe la main au take s'il y en a un, sinon au read, sinon au autres write
			writing = false;
			if (nbTakeWaiting > 0) {
				canTake.signal();
			} else if (nbReadWaiting > 0) {
				canRead.signalAll();
			} else if (nbWriteWaiting > 0) {
				canWrite.signal();
			}
			monitor.unlock();

			// On vérifie les read
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
		// On vérifie que l'on peut prendre, c'est à dire qu'il n'y a pas de lecteurs
		if (nbCurrentReaders > 0) {
			try {
				nbTakeWaiting++;
				launchTimer();
				System.out.println("le take attend");
				canTake.await();
				timerLaunched = false;
				taking = true;
				nbTakeWaiting--;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// on cherche le tuple dans l'espace
		Tuple ret = null;
		Iterator<Tuple> iterator = this.listTuples.iterator();
		while (iterator.hasNext()) {
			ret = iterator.next();
			if (ret.matches(template)) {
				this.listTuples.remove(ret);
				break;
			}
		}
		// On passe la main au read s'il y en a, sinon au write, sinon au autres take
		taking = false;
		if (nbReadWaiting > 0) {
			canRead.signalAll();
		} else if (nbWriteWaiting > 0) {
			canWrite.signal();
		} else if (nbTakeWaiting > 0) {
			canTake.signal();
		}
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
		// On vérifie si on peut lire (pas d'écriture ou prise en cours + temps non
		// écoulé)
		monitor.lock();
		if (writing || taking || timeout) {
			try {
				nbReadWaiting++;
				System.out.println("le read attend");
				canRead.await();
				nbReadWaiting--;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// On peut lire
		nbCurrentReaders++;
		monitor.unlock();
		// on cherche le tuple dans l'espace
		Tuple ret = null;
		Iterator<Tuple> iterator = this.listTuples.iterator();
		while (iterator.hasNext()) {
			ret = iterator.next();
			if (ret.matches(template)) {
				break;
			}
		}
		monitor.lock();
		// On vérifie si on doit réveiller quelqu'un
		nbCurrentReaders--;
		if (nbCurrentReaders == 0) {
			if (nbWriteWaiting > 0) {
				canWrite.signal();
			} else if (nbTakeWaiting > 0) {
				canTake.signal();
			}
		}
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
		// On vérifie que l'on peut prendre, c'est à dire qu'il n'y a pas de lecteurs
		if (nbCurrentReaders > 0) {
			try {
				nbTakeWaiting++;
				launchTimer();
				canTake.await();
				timerLaunched = false;
				taking = true;
				nbTakeWaiting--;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		Tuple ret = null;
		Iterator<Tuple> iterator = this.listTuples.iterator();
		while (iterator.hasNext()) {
			ret = iterator.next();
			if (ret.matches(template)) {
				this.listTuples.remove(ret);
				break;
			}
		}
		// On passe la main au read s'il y en a, sinon au write, sinon au autres take
		taking = false;
		if (nbReadWaiting > 0) {
			canRead.signalAll();
		} else if (nbWriteWaiting > 0) {
			canWrite.signal();
		} else if (nbTakeWaiting > 0) {
			canTake.signal();
		}
		monitor.unlock();
		return ret;
	}

	@Override
	public Tuple tryRead(Tuple template) {
		// On vérifie si on peut lire (pas d'écriture ou prise en cours + temps non
		// écoulé)
		monitor.lock();
		if (writing || taking || timeout) {
			try {
				nbReadWaiting++;
				canRead.await();
				nbReadWaiting--;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// On peut lire
		nbCurrentReaders++;
		monitor.unlock();
		Tuple ret = null;
		Iterator<Tuple> iterator = this.listTuples.iterator();
		while (iterator.hasNext()) {
			ret = iterator.next();
			if (ret.matches(template)) {
				break;
			}
		}
		// On vérifie si on doit réveiller quelqu'un
		monitor.lock();
		nbCurrentReaders--;
		if (nbCurrentReaders == 0) {
			if (nbWriteWaiting > 0) {
				canWrite.signal();
			} else if (nbTakeWaiting > 0) {
				canTake.signal();
			}
		}
		monitor.unlock();
		return ret;
	}

	@Override
	public Collection<Tuple> takeAll(Tuple template) {
		monitor.lock();
		// On vérifie que l'on peut prendre, c'est à dire qu'il n'y a pas de lecteurs
		if (nbCurrentReaders > 0) {
			try {
				nbTakeWaiting++;
				launchTimer();
				canTake.await();
				timerLaunched = false;
				taking = true;
				nbTakeWaiting--;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
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
		// On passe la main au read s'il y en a, sinon au write, sinon au autres take
		taking = false;
		if (nbReadWaiting > 0) {
			canRead.signalAll();
		} else if (nbWriteWaiting > 0) {
			canWrite.signal();
		} else if (nbTakeWaiting > 0) {
			canTake.signal();
		}
		monitor.unlock();
		return collectionTuples;
	}

	@Override
	public Collection<Tuple> readAll(Tuple template) {
		// On vérifie si on peut lire (pas d'écriture ou prise en cours + temps non
		// écoulé)
		monitor.lock();
		if (writing || taking || timeout) {
			try {
				nbReadWaiting++;
				canRead.await();
				nbReadWaiting--;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// On peut lire
		nbCurrentReaders++;
		monitor.unlock();

		Collection<Tuple> collectionTuples = new Vector<Tuple>();
		Iterator<Tuple> iterator = this.listTuples.iterator();
		Tuple ret = null;
		while (iterator.hasNext()) {
			ret = iterator.next();
			if (ret.matches(template)) {
				collectionTuples.add(ret);
			}
		}
		// On vérifie si on doit réveiller quelqu'un
		monitor.lock();
		nbCurrentReaders--;
		if (nbCurrentReaders == 0) {
			if (nbWriteWaiting > 0) {
				canWrite.signal();
			} else if (nbTakeWaiting > 0) {
				canTake.signal();
			}
		}
		monitor.unlock();
		return collectionTuples;
	}

	@Override
	public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
		System.out.println("on entre dans l'event register");
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
		System.out.println("on sort de l'event register");
	}

	@Override
	public void debug(String prefix) {
		System.out.println(prefix + " On entre dans debug !");
	}

}
