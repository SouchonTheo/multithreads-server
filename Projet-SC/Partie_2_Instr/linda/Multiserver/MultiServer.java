package linda.Multiserver;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MultiServer {

    private static int nbresServer;

    public static void init(int nbS) {
        nbresServer = nbS;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Ajoutez le nombre de serveurs souhaité.");
            System.exit(2);
        }
        init(Integer.parseInt(args[0]));
        ServerStart ss;
        for (int i = 0; i < nbresServer; i++) {
            ss = new ServerStart(i);
            ss.start();
        }
    }

    private static class ServerStart extends Thread {

        protected Integer id;
        protected Integer initialPort = 4000;

        public ServerStart(Integer id) {
            this.id = id;
        }

        public void run() {
            String url = null;
            String nextURL = null;
            try {
                url = "//" + InetAddress.getLocalHost().getHostName() + 
                    ":" + (initialPort + id) + "/LindaServer";
                nextURL = "//" + InetAddress.getLocalHost().getHostName() + 
                    ":" + (initialPort + ((id + 1)%nbresServer)) + "/LindaServer";
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            LindaServerImpl.ServerStart(url, nextURL, initialPort + id, nbresServer);
        }
    }

}
