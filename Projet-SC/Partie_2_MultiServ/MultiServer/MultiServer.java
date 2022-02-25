package linda.MultiServer;

public class MultiServer {

    private static int nbresServer;

    public static void init(int nbS) {
        nbresServer = nbS;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.exit(2);
        }
        init(Integer.parseInt(args[0]));
        ServerStart ss;
        for (int i = 1; i <= nbresServer; i++) {
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
            LindaServerImpl.ServerStart(id, initialPort + id - 1);
        }
    }

}
