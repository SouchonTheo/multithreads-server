package linda.search.Request1man1search;

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
    private String url;

    public Manager(Linda lda, String pathname, String search, int nbre, String url) {
        linda = lda;
        this.url = url;
        this.pathname = pathname;
        this.search = search;
        nbSearchers = nbre;
        reqUUID = UUID.randomUUID();
    }

    private void addSearch(String search) {

        this.search = search;
        System.out.println("Search " + this.search);
        linda.eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.IMMEDIATE,
                new Tuple(Code.Result, reqUUID, String.class, Integer.class), new CbGetResult());
        linda.write(new Tuple(Code.Request, reqUUID, this.search));
        linda.eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.IMMEDIATE,
                new Tuple(Code.IncSearchers, reqUUID), new CbGetSearchers());

    }

    private void loadData(String pathname) {
        try (Stream<String> stream = Files.lines(Paths.get(pathname))) {
            stream.limit(1000).forEach(s -> linda.write(new Tuple(Code.Value, reqUUID, s.trim())));
        } catch (java.io.IOException e) {
            System.out.println("prout");
        }
        System.out.println("Fin du chargement du dictionaire");
    }

    private void waitForEndSearch() {
        linda.take(new Tuple(Code.Searcher, "done", reqUUID));
        linda.take(new Tuple(Code.Request, reqUUID, String.class)); // remove query
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
                        new Tuple(Code.Result, reqUUID, String.class, Integer.class), new CbGetResult()); // Problem
            } else {
                linda.eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.IMMEDIATE,
                        new Tuple(Code.Result, reqUUID, String.class, Integer.class), new CbGetResult());
            }
        }
    }

    private class CbGetSearchers implements linda.Callback {
        public void call(Tuple t) {
            nbSearchers++;
            linda.eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.IMMEDIATE,
                    new Tuple(Code.IncSearchers, reqUUID), new CbGetSearchers());
        }

    }

    public void run() {
        this.loadData(pathname);
        for (int i = 0; i < nbSearchers; i++) {
            Linda ld = new linda.server.LindaClient(url);
            Searcher searcher = new Searcher(ld, reqUUID);
            (new Thread(searcher)).start();
        }
        this.addSearch(search);
        this.waitForEndSearch();
    }
}
