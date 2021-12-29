package linda.search.RequestVF;

import java.util.UUID;
import java.util.stream.Stream;
import java.nio.file.Files;
import java.nio.file.Paths;
import linda.*;

public class Manager implements Runnable {

    private static Linda linda;

    private static UUID reqUUID;
    private String pathname;
    private String search;
    private int bestvalue = Integer.MAX_VALUE; // lower is better
    private String bestresult;

    private static Integer nbSearchers;
    private Integer nbresDone = 0;

    public Manager(Linda lda, String pathname, String search, int nbre, String url) {
        linda = lda;
        this.pathname = pathname;
        this.search = search;
        nbSearchers = nbre;
        reqUUID = UUID.randomUUID();

        for (int i = 0; i < nbSearchers; i++) {
            Linda ld = new linda.server.LindaClient(url);
            Searcher searcher = new Searcher(ld, reqUUID);
            (new Thread(searcher)).start();
        }
    }

    private void addSearch(String search) {
        this.search = search;
        System.out.println("Search " + reqUUID + " for " + this.search);
        linda.eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.IMMEDIATE,
                new Tuple(Code.Result, reqUUID, String.class, Integer.class), new CbGetResult());
        linda.write(new Tuple(Code.Request, reqUUID, this.search));
        linda.eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.IMMEDIATE,
                new Tuple(Code.IncSearchers, reqUUID), new CbGetSearchers());

    }

    private void loadData(String pathname) {
        try (Stream<String> stream = Files.lines(Paths.get(pathname))) {
            stream.limit(10000).forEach(s -> linda.write(new Tuple(Code.Value, reqUUID, s.trim())));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private static class EndSearch extends Thread {

        public EndSearch() {
        }

        public void run() {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < nbSearchers; i++) {
                if (linda.tryRead(new Tuple(Code.Request, reqUUID, String.class)) != null) {
                    linda.write(new Tuple(Code.Searcher, "done", reqUUID));
                }
            }
        }
    }

    private void waitForEndSearch() {
        EndSearch t = new EndSearch();
        t.start();
        while (this.nbresDone < nbSearchers) {
            linda.take(new Tuple(Code.Searcher, "done", reqUUID));
            this.nbresDone++;
        }
        linda.take(new Tuple(Code.Request, reqUUID, String.class)); // remove query
        linda.takeAll(new Tuple(Code.Value, reqUUID, String.class));
        linda.takeAll(new Tuple(Code.Searcher, "done", reqUUID));
        System.out.println("query done");
    }

    private class CbGetResult implements linda.Callback {
        public void call(Tuple t) { // [ Result, ?UUID, ?String, ?Integer ]
            String s = (String) t.get(2);
            Integer v = (Integer) t.get(3);
            if (v == 0) {
                bestvalue = v;
                bestresult = s;
                System.out.println("New best (" + bestvalue + "): \"" + bestresult + "\"");
                for (int i = 0; i < nbSearchers; i++) {
                    if (linda.tryRead(new Tuple(Code.Request, reqUUID, String.class)) != null) {
                        linda.write(new Tuple(Code.Searcher, "done", reqUUID));
                    }
                }
            } else if (v < bestvalue) {
                bestvalue = v;
                bestresult = s;
                System.out.println("New best (" + bestvalue + "): \"" + bestresult + "\"");
                linda.eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.IMMEDIATE,
                        new Tuple(Code.Result, reqUUID, String.class, Integer.class), this);
            } else {
                linda.eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.IMMEDIATE,
                        new Tuple(Code.Result, reqUUID, String.class, Integer.class), this);
            }
        }
    }

    private class CbGetSearchers implements linda.Callback {
        public void call(Tuple t) {
            nbSearchers++;
            linda.eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.IMMEDIATE,
                    new Tuple(Code.IncSearchers, reqUUID), this);
        }
    }

    public void run() {
        this.loadData(pathname);
        this.addSearch(search);
        this.waitForEndSearch();
    }
}
