package linda.server;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

/**
 * Client part of a client/server implementation of Linda.
 * It implements the Linda interface and propagates everything to the server it
 * is connected to.
 */
public class LindaClient implements Linda {

    private LindaServer lindaimpl;
    private Registry registry;

    /**
     * Initializes the Linda implementation.
     * 
     * @param serverURI the URI of the server, e.g.
     *                  "rmi://localhost:4000/LindaServer" or
     *                  "//localhost:4000/LindaServer".
     */
    public LindaClient(String serverURI) {
        try {
            lindaimpl = (LindaServer) Naming.lookup(serverURI);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void write(Tuple t) {
        try {
            lindaimpl.write(t);
        } catch (RemoteException e) {
            e.printStackTrace();
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
        try {
            ret = lindaimpl.read(template);
        } catch (RemoteException e) {
            e.printStackTrace();
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
        try {
            ret = lindaimpl.tryRead(template);
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
            System.out.println("dans le LindaClient");
            RemoteCallbackInterface rcb = new RemoteCallback(callback);
            System.out.println("New callback : " + rcb + "\n");
            lindaimpl.eventRegister(mode, timing, template, rcb);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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
