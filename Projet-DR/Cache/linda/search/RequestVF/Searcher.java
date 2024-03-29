package linda.search.RequestVF;

import linda.*;
import java.util.Arrays;
import java.util.UUID;

public class Searcher implements Runnable {

    private Linda linda;

    private UUID reqUUID;

    public Searcher(Linda linda, UUID reqUUID) {
        this.linda = linda;
        this.reqUUID = reqUUID;
    }

    public void run() {
        System.out.println("Ready to do a search");
        Tuple treq = linda.read(new Tuple(Code.Request, reqUUID, String.class));
        String req = (String) treq.get(2);
        Tuple tv;
        while (treq != null) {
            System.out.println("Looking for: " + req);
            while ((tv = linda.tryTake(new Tuple(Code.Value, reqUUID, String.class))) != null) {
                String val = (String) tv.get(2);
                int dist = getLevenshteinDistance(req, val);
                if (dist < 100) { // arbitrary
                    System.out.println(reqUUID);
                    linda.write(new Tuple(Code.Result, reqUUID, val, dist));
                }
            }
            if (linda.tryRead(new Tuple(Code.Request, reqUUID, String.class)) != null) {
                linda.write(new Tuple(Code.Searcher, "done", reqUUID));
            }

            treq = linda.tryRead(new Tuple(Code.Request, UUID.class, String.class));
            if (treq != null) {
                reqUUID = (UUID) treq.get(1);
                req = (String) treq.get(2);
                linda.write(new Tuple(Code.IncSearchers, reqUUID));
            }
        }
    }

    /*****************************************************************/

    /* Levenshtein distance is rather slow */
    /* Copied from https://www.baeldung.com/java-levenshtein-distance */
    static int getLevenshteinDistance(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];
        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = min(dp[i - 1][j - 1]
                            + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1);
                }
            }
        }
        return dp[x.length()][y.length()];
    }

    private static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    private static int min(int... numbers) {
        return Arrays.stream(numbers).min().orElse(Integer.MAX_VALUE);
    }

}
