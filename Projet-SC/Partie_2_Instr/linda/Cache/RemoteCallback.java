package linda.Cache;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import linda.Callback;
import linda.Tuple;

public class RemoteCallback extends UnicastRemoteObject implements RemoteCallbackInterface {

    private Callback callback;

    public RemoteCallback(Callback callback) throws RemoteException {
        this.callback = callback;
    }

    @Override
    public void rCall(Tuple t) throws RemoteException {
        this.callback.call(t);
        System.exit(0);
    }

}