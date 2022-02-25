package linda.MultiServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import linda.Callback;
import linda.Tuple;

public class RemoteCallbackClient implements Callback {

    private RemoteCallbackInterface rcbi;

    public RemoteCallbackClient(RemoteCallbackInterface rcbi) {
        this.rcbi = rcbi;
    }

    @Override
    public void call(Tuple t) {
        try {
            this.rcbi.rCall(t);
            System.out.println("avant le exit");
            // UnicastRemoteObject.unexportObject(rcbi, true);
            // System.exit(0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}