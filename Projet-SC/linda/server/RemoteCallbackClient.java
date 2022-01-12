package linda.server;

import java.rmi.RemoteException;

import linda.Callback;
import linda.Tuple;

public class RemoteCallbackClient implements Callback {

    private RemoteCallbackInterface rcbi;

    public RemoteCallbackClient (RemoteCallbackInterface rcbi){
        this.rcbi = rcbi;
    }

    @Override
    public void call(Tuple t) {
        try {
            this.rcbi.rCall(t);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


}