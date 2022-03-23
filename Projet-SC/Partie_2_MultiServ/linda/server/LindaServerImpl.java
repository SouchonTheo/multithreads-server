package linda.server;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import linda.Linda.eventMode;
import linda.Linda.eventTiming;
import linda.InternalCallback;
import linda.Tuple;
import linda.shm.CentralizedLinda;

public class LindaServerImpl extends UnicastRemoteObject implements LindaServer {

    private CentralizedLinda linda;
    private static LindaServer ldNextServ;
    private static Integer nbresServer;

    protected LindaServerImpl() throws RemoteException {
        this.linda = new linda.shm.CentralizedLinda();
    }

    @Override
    public void write(Tuple t) throws RemoteException {
        Vector<InternalCallback> readers;
        InternalCallback taker;
        taker = linda.getFirstTaker(t);
        if (taker == null) {
            taker = ldNextServ.collectTake(t, nbresServer - 1);
        }
        readers = linda.getReaders(t);
        readers.addAll(ldNextServ.collectReaders(t, nbresServer - 1));
        for (InternalCallback reader : readers) {
            reader.getCallback().call(t);
        }

        if (true) {
            this.linda.write(t);
        }
    }

    @Override
    public Vector<InternalCallback> collectReaders(Tuple template, Integer nbRestant) throws RemoteException {
        Vector<InternalCallback> readers = null;
        if (nbRestant > 1) {
            readers = linda.getReaders(template);
            readers.addAll(ldNextServ.collectReaders(template, nbRestant - 1));
        }
        return readers;
    }

    @Override
    public InternalCallback collectTake(Tuple template, Integer nbRestant) throws RemoteException {
        InternalCallback taker = null;
        if (nbRestant > 1) {
            taker = linda.getFirstTaker(template);
            if (taker == null) {
                taker = ldNextServ.collectTake(template, nbresServer - 1);
            }
        }
        return taker;
    }

    @Override
    public Tuple take(Tuple template) throws RemoteException {
        Tuple findTuple = linda.tryTake(template);
        if (findTuple == null && nbresServer > 1) {
            findTuple = ldNextServ.take(template, nbresServer);
        } else if ( findTuple == null) {
            findTuple = linda.take(template);
        }
        return findTuple;
    }

    @Override
    public Tuple take(Tuple template, Integer nbRestant) throws RemoteException {
        Tuple findTuple;
        if (nbRestant > 1) {
            findTuple = linda.tryTake(template);
            if (findTuple == null) {
                findTuple = ldNextServ.take(template, nbRestant - 1);
            }
        } else {
            findTuple = linda.take(template);
        }
        return findTuple;
    }

    @Override
    public Tuple read(Tuple template) throws RemoteException {
        Tuple findTuple = linda.tryRead(template);
        if (findTuple == null && nbresServer > 1) {
            findTuple = ldNextServ.read(template, nbresServer);
        } else if (findTuple == null) {
            findTuple = linda.read(template);
        }
        return findTuple;
    }
    
    @Override
    public Tuple read(Tuple template, Integer nbRestant) throws RemoteException {
        Tuple findTuple;
        if (nbRestant > 1) {
            findTuple = linda.tryRead(template);
            if (findTuple == null) {
                findTuple = ldNextServ.read(template, nbRestant - 1);
            }
        } else {
            findTuple = linda.read(template);
        }
        return findTuple;
    }

    @Override
    public Tuple tryTake(Tuple template) throws RemoteException {
        Tuple findTuple = linda.tryTake(template);
        if (findTuple == null && nbresServer > 1) {
            findTuple = ldNextServ.tryTake(template, nbresServer);
        }
        return findTuple;
    }

    @Override
    public Tuple tryTake(Tuple template, Integer nbRestant) throws RemoteException {
        Tuple findTuple = null;
        if (nbRestant > 1) {
            findTuple = linda.tryTake(template);
            if (findTuple == null) {
                findTuple = ldNextServ.tryTake(template, nbRestant - 1);
            }
        }
        return findTuple;
    }

    @Override
    public Tuple tryRead(Tuple template) throws RemoteException {
        Tuple findTuple = linda.tryRead(template);
        if (findTuple == null && nbresServer > 1) {
            findTuple = ldNextServ.tryRead(template, nbresServer);
        }
        return findTuple;
    }

    @Override
    public Tuple tryRead(Tuple template, Integer nbRestant) throws RemoteException {
        Tuple findTuple = null;
        if (nbRestant > 1) {
            findTuple = linda.tryRead(template);
            if (findTuple == null) {
                findTuple = ldNextServ.tryRead(template, nbRestant - 1);
            }
        }
        return findTuple;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) throws RemoteException {
        Collection<Tuple> clServeur = linda.takeAll(template);
        if (nbresServer > 1) {
            Collection<Tuple> clTuples = ldNextServ.takeAll(template, nbresServer);
            clServeur.addAll(clTuples);
        }
        return clServeur;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template, Integer nbRestant) throws RemoteException {
        Collection<Tuple> clTuples;
        Collection<Tuple> clServeur = null;
        if (nbRestant > 1) {
            clServeur = linda.takeAll(template);
            clTuples = ldNextServ.takeAll(template, nbRestant - 1);
            clServeur.addAll(clTuples);
        }
        return clServeur;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) throws RemoteException {
        Collection<Tuple> clServeur = linda.readAll(template);
        if (nbresServer > 1) {
            Collection<Tuple> clTuples = ldNextServ.readAll(template, nbresServer);
            clServeur.addAll(clTuples);
        }
        return clServeur;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template, Integer nbRestant) throws RemoteException {
        Collection<Tuple> clTuples;
        Collection<Tuple> clServeur = null;
        if (nbRestant > 1) {
            clServeur = linda.readAll(template);
            clTuples = ldNextServ.readAll(template, nbRestant-1);
            clServeur.addAll(clTuples);
        }
        return clServeur;
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, RemoteCallbackInterface rCallback)
            throws RemoteException {
        RemoteCallbackClient cb = new RemoteCallbackClient(rCallback);
        linda.eventRegister(mode, timing, template, cb);
    }

    @Override
    public void debug(String prefix) throws RemoteException {
        linda.debug(prefix);
    }


    public static void ServerStart(String url, String nextURL, Integer port, Integer nbServer) {
        try {
            nbresServer = nbServer;
            LindaServerImpl server = new LindaServerImpl();
            LocateRegistry.createRegistry(port);
            Naming.rebind(url, server);
            System.out.println("Le serveur est démarré sur " + url);
            ldNextServ = (LindaServer) Naming.lookup(nextURL);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }


}
