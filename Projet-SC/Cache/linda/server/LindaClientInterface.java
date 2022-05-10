package linda.server;

import java.rmi.*;
import java.util.Collection;

import linda.Tuple;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;

public interface LindaClientInterface extends Remote {

    public void destroyInCache(Tuple t) throws RemoteException;

}

