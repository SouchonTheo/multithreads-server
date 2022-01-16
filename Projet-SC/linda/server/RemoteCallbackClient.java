package linda.server;

import java.io.EOFException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import linda.Callback;
import linda.Tuple;

public class RemoteCallbackClient implements Callback {

    private static RemoteCallbackInterface rcbi;
    private static Tuple t;

    public RemoteCallbackClient(RemoteCallbackInterface r) {
        rcbi = r;
    }

    @Override
    public void call(Tuple tuple) {
        t = tuple;
        Thread thread = new Thread("New Thread") {
            public void run() {
                try {
                    rcbi.rCall(t);
                } catch (java.io.IOException ex) {
                    ex.printStackTrace();
                }
            }
        };
        thread.start();

    }
}