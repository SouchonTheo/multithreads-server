package linda.Cache;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

import linda.Callback;
import linda.Linda;
import linda.Tuple;
import linda.shm.CentralisedLindaCache;

/**
 * Client part of a client/server implementation of Linda.
 * It implements the Linda interface and propagates everything to the server it
 * is connected to.
 */
public class LindaClient extends UnicastRemoteObject implements Linda, LindaClientInterface {

    private CentralisedLindaCache cache = new CentralisedLindaCache();

    private LindaServerInterface lindaimpl;

    private ReentrantLock monitor;

    private ArrayList<Tuple> pending;

    /**
     * Initializes the Linda implementation.
     * 
     * @param serverURI the URI of the server, e.g.
     *                  "rmi://localhost:4000/LindaServer" or
     *                  "//localhost:4000/LindaServer".
     */
    public LindaClient(String serverURI) throws RemoteException {
        try {
            lindaimpl = (LindaServerInterface) Naming.lookup(serverURI);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
        monitor = new ReentrantLock();
        pending = new ArrayList<Tuple>();
    }

    @Override
    public void write(Tuple t) {
        try {
            
            monitor.lock();
            Tuple temp = cache.tryRead(t);
            if (temp == null) {
                cache.write(t);
            }
            monitor.unlock();
            lindaimpl.write(t);
            
        } catch (RemoteException e) {
            e.printStackTrace();
            //Si il y a une erreur à l'écriture, on supprime l'élément du cache.
            cache.tryTake(t);
        }
    }

    @Override
    public Tuple take(Tuple template) {
        Tuple ret = null;
        try {
            ret = lindaimpl.take(template);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    public Tuple read(Tuple template) {
        Tuple ret = null;
        monitor.lock();
        pending.clear();
        ret = cache.tryRead(template);
        monitor.unlock();

        if (ret==null) {
            try {
                ret = lindaimpl.read(template);
                monitor.lock();
                boolean mettre_cache = true;
                for(Tuple t : pending) {
                    if (ret.matches(t)) {
                        mettre_cache = false;
                        break;
                    }
                }
                if(mettre_cache) {
                    cache.write(ret);
                }
                monitor.unlock();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
            
       
        return ret;
    }

    @Override
    public Tuple tryTake(Tuple template) {
        Tuple ret = null;
        try {
            ret = lindaimpl.tryTake(template);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        Tuple ret = null;
        monitor.lock();
        pending.clear();
        ret = cache.tryRead(template);
        monitor.unlock();
        try {
            
            if (ret==null) {
                ret = lindaimpl.tryRead(template);
                monitor.lock();
                boolean mettre_cache = true;
                for(Tuple t : pending) {
                    if (ret.matches(t)) {
                        mettre_cache = false;
                        break;
                    }
                }
                if(mettre_cache) {
                    cache.write(ret);
                }
                monitor.unlock();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        Collection<Tuple> ret = null;
        try {
            ret = lindaimpl.takeAll(template);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        Collection<Tuple> ret = null;
        try {
            ret = lindaimpl.readAll(template);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
        try {
            RemoteCallbackInterface rcb = new RemoteCallback(callback);
            lindaimpl.eventRegister(mode, timing, template, rcb);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void destroyInCache(Tuple t) throws RemoteException {
        monitor.lock();
        cache.tryTake(t);
        pending.add(t);
        monitor.unlock();
    }

    @Override
    public void debug(String prefix) {
        try {
            lindaimpl.debug(prefix);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
