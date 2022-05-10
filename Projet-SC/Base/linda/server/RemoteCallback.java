package linda.server;

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
        System.out.println("rCall debut");
        this.callback.call(t);
        System.out.println("rCall fin");
        System.exit(0);
    }

}