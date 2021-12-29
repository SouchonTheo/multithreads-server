package linda.search.Request2;

import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;
import java.nio.file.Files;
import java.nio.file.Paths;
import linda.*;

public class Manager implements Runnable {

    private Linda linda;

    private UUID reqUUID;
    private String pathname;
    private String search;
    private int bestvalue = Integer.MAX_VALUE; // lower is better
    private String bestresult;

    private Integer nbSearchers;
    private Integer nbresDone = 0;
    private String url;


    public Manager(Linda linda, String pathname, String search, int nbre, String url) {
        this.linda = linda;
        this.pathname = pathname;
        this.search = search;
        this.nbSearchers = nbre;
        this.url = url;

        for (int i = 0; i < nbSearchers; i++) {
            Linda ld = new linda.server.LindaClient(url);
            Searcher searcher = new Searcher(ld);
            (new Thread(searcher)).start();
        }
    }

    private void addSearch(String search) {
        this.search = search;
        this.reqUUID = UUID.randomUUID();
        System.out.println("Search " + this.reqUUID + " for " + this.search);
        linda.eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.IMMEDIATE,
                new Tuple(Code.Result, this.reqUUID, String.class, Integer.class), new CbGetResult());
        linda.write(new Tuple(Code.Request, this.reqUUID, this.search));
        linda.eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.IMMEDIATE,
                new Tuple(Code.IncSearchers, this.reqUUID), new CbGetSearchers());
        
    }

    private void loadData(String pathname) {
        try (Stream<String> stream = Files.lines(Paths.get(pathname))) {
            stream.limit(10000).forEach(s -> linda.write(new Tuple(Code.Value, reqUUID, s.trim())));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void waitForEndSearch() {
        while (this.nbresDone != this.nbSearchers) {
            linda.take(new Tuple(Code.Searcher, "done", this.reqUUID));
            this.nbresDone++;
        }
        linda.take(new Tuple(Code.Request, this.reqUUID, String.class)); // remove query
        System.out.println("query done");
    }

    private class CbGetResult implements linda.Callback {
        public void call(Tuple t) { // [ Result, ?UUID, ?String, ?Integer ]
            String s = (String) t.get(2);
            Integer v = (Integer) t.get(3);
            if (v < bestvalue) {
                bestvalue = v;
                bestresult = s;
                System.out.println("New best (" + bestvalue + "): \"" + bestresult + "\"");
            }
            linda.eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.IMMEDIATE,
                    new Tuple(Code.Result, reqUUID, String.class, Integer.class), this);
        }
    }

    private class CbGetSearchers implements linda.Callback{
        public void call (Tuple t){
            this.nbSearchers++;
        } 
        linda.eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.IMMEDIATE,
                    new Tuple(Code.IncSearchers, reqUUID), this);

    } 

    public void run() {
        this.loadData(pathname);
        this.addSearch(search);
        this.waitForEndSearch();
    }
}
