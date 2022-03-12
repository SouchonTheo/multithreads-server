package linda.server;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;

import linda.Linda.eventMode;
import linda.Linda.eventTiming;
import linda.Tuple;
import linda.shm.CentralizedLinda;

public class LindaServer extends UnicastRemoteObject implements LindaServerInterface {

    private CentralizedLinda linda;
    private List<LindaClientInterface> listeClient = new ArrayList<LindaClientInterface>();
    private ReentrantLock monitor;

    protected LindaServer() throws RemoteException {
        this.linda = new linda.shm.CentralizedLinda();
        monitor = new ReentrantLock();
    }

    public void subscribe(LindaClientInterface l) throws RemoteException {
        monitor.lock();
        listeClient.add(l);
        monitor.unlock();
    }

    public void unsubscribe(LindaClientInterface l) throws RemoteException {
        monitor.lock();
        listeClient.remove(l);
        monitor.unlock();
    }

    @Override
    public void write(Tuple t) throws RemoteException {
        monitor.lock();
        linda.write(t);
        monitor.unlock();
    }

    @Override
    public Tuple take(Tuple template) throws RemoteException {
        monitor.lock();
        Tuple result = linda.take(template);
        monitor.unlock();
        if (linda.tryRead(result) == null) {
            for (LindaClientInterface l : listeClient) {
                l.destroyInCache(result);
            }
        }
        return result;
    }

    @Override
    public Tuple read(Tuple template) throws RemoteException {
        monitor.lock();
        Tuple t = linda.read(template);
        monitor.unlock();
        return t;
    }

    @Override
    public Tuple tryTake(Tuple template) throws RemoteException {
        monitor.lock();
        Tuple result = linda.tryTake(template);
        monitor.unlock();
        if (linda.tryRead(result) == null) {
            for (LindaClientInterface l : listeClient) {
                l.destroyInCache(result);
            }
        }
        return result;
    }

    @Override
    public Tuple tryRead(Tuple template) throws RemoteException {
        monitor.lock();
        Tuple t = linda.tryRead(template);
        monitor.unlock();
        return t;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) throws RemoteException {
        monitor.lock();
        Collection<Tuple> result = linda.takeAll(template);
        monitor.unlock();
        for(Tuple t : result) {
            if (linda.tryRead(t) == null) {
                for (LindaClientInterface l : listeClient) {
                    l.destroyInCache(t);
                }
            }
        }
        return result;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) throws RemoteException {
        monitor.lock();
        Collection<Tuple> t = linda.readAll(template);
        monitor.unlock();
        return t;
        
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, RemoteCallbackInterface rCallback)
            throws RemoteException {
        System.out.println("LSI - ER");
        RemoteCallbackClient cb = new RemoteCallbackClient(rCallback);
        System.out.println("mode : " + mode);
        System.out.println("timing : " + timing);
        System.out.println("template : " + template);
        System.out.println("rCallback : " + rCallback);
        monitor.lock();
        linda.eventRegister(mode, timing, template, cb);
        monitor.unlock();
    }

    @Override
    public void debug(String prefix) throws RemoteException {
        monitor.unlock();
        linda.debug(prefix);
        monitor.unlock();
    }

    public static void main(String args[]) {
        int port = 4000;
        String URL1;
        try {
            LindaServer server = new LindaServer();
            LocateRegistry.createRegistry(port);
            URL1 = "//" + InetAddress.getLocalHost().getHostName() + ":" + port + "/LindaServer";
            Naming.rebind(URL1, server);
            System.out.println("Le serveur est démarré sur " + URL1);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

}
