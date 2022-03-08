package linda.server;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;

import linda.Linda.eventMode;
import linda.Linda.eventTiming;
import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;

public class LindaServerImpl extends UnicastRemoteObject implements LindaServer {

    private CentralizedLinda linda;
    private static Linda ldClient;

    protected LindaServerImpl() throws RemoteException {
        this.linda = new linda.shm.CentralizedLinda();
    }

    @Override
    public void write(Tuple t) throws RemoteException {
        linda.write(t);

    }

    @Override
    public Tuple take(Tuple template) throws RemoteException {
        return linda.take(template);
    }

    @Override
    public Tuple read(Tuple template) throws RemoteException {
        return linda.read(template);
    }

    @Override
    public Tuple tryTake(Tuple template) throws RemoteException {
        return linda.tryTake(template);
    }

    @Override
    public Tuple tryRead(Tuple template) throws RemoteException {
        return linda.tryRead(template);
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) throws RemoteException {
        return linda.takeAll(template);
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) throws RemoteException {
        return linda.readAll(template);
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


    public static void ServerStart(String url, String nextURL, Integer port) {
        try {
            LindaServerImpl server = new LindaServerImpl();
            LocateRegistry.createRegistry(port);
            Naming.rebind(url, server);
            System.out.println("Le serveur est démarré sur " + url);
            ldClient = new linda.server.LindaClient(nextURL);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

}
