/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package dk.ekot.ibm;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * https://www.research.ibm.com/haifa/ponderthis/challenges/February2018.html
 */
public class Feb2018 {
    private static Log log = LogFactory.getLog(Feb2018.class);
    public static final int THREADS = 4;
    public static final int MINUTES_GOAL = 2569;
    private final ExecutorService executor = Executors.newFixedThreadPool(THREADS, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    });

    public static void main(String[] args) throws Exception {
        new Feb2018();
    }

    /*
    There are three steps to find the optima solution. Read the comments inside of the method.
     */
    public Feb2018() throws ExecutionException, InterruptedException {
        // Step 1: Get a ballpark upper number of sides by letting all dices have the same number of sides.
        // This gives me 11
        findOrigo(MINUTES_GOAL, 1000000);

        // Step 2: Add some margin to the 11 from above and run through all permutations.
        // This mostly gives me [9, 13] with the third dice being very jittery
        for (int i = 0 ; i < 10 ; i++) {
            permutate(2, 18, 10000);
        }

        // Step 3: Find the third dice by fixing the first two dices to the best suggestion above.
        // Fixing [9, 13] gives me [3], with [4] as a close second
        fix2(9, 13, 2, 6, 100000000); // 3 or 4

        // Final result for me was [3, 9, 13]
    }

    private void permutate(int min, int max, int runs) throws ExecutionException, InterruptedException {
        System.out.println("*** Finding best permutations (~10 minutes)");
        final long startTime = System.nanoTime();
        double nearestDist = Double.MAX_VALUE;
        double nearestTime = Double.MAX_VALUE;
        int n1 = 0, n2 = 0, n3 = 0;
        for (int d1 = min ; d1 <= max ; d1++) {
            for (int d2 = min ; d2 <= max ; d2++) {
                for (int d3 = min; d3 <= max; d3++) {
                    double time = parallel(d1, d2, d3, runs);
                    double dist = Math.abs(time-MINUTES_GOAL);
                    if (dist < nearestDist) {
                        nearestTime = time;
                        nearestDist = dist;
                        n1 = d1;
                        n2 = d2;
                        n3 = d3;
                    }
                }
            }
        }

        int[] sorted = new int[3];
        sorted[0] = n1;
        sorted[1] = n2;
        sorted[2] = n3;
        Arrays.sort(sorted);
        n1 = sorted[0];
        n2 = sorted[1];
        n3 = sorted[2];

        System.out.println(String.format(
                "Nearest: d1=%2d, d2=%2d, d3=%2d, time=%.2f, dist=%1.2f (runs=%d, execution time: %dms)",
                n1, n2, n3, nearestTime, nearestDist, runs, (System.nanoTime()-startTime)/1000000));
    }

    private void fix2(int f1, int f2, int min, int max, int runs) throws ExecutionException, InterruptedException {
        System.out.println("*** Finding third dice (~10 minutes)");
        for (int d = min ; d <= max ; d++) {
            final long startTime = System.nanoTime();
            double time = parallel(d, f1, f2, runs);
            System.out.println(String.format(
                    "Sides=%2d/%2d/%2d, minutes=%.2f, dist=%1.2f (goal=%d, execution time = %dms)",
                    d, f1, f2, time, Math.abs(time - MINUTES_GOAL), MINUTES_GOAL,
                    (System.nanoTime()-startTime)/1000000));
        }
    }

    private void findOrigo(double goal, int runs) throws ExecutionException, InterruptedException {
        System.out.println("*** Getting ballpark upper limit (<1 minute)");
        for (int d = 2 ; d < Integer.MAX_VALUE ; d++) {
            double time = parallel(d, d, d, runs);
            System.out.println("Sides=" + d + ", minutes=" + time + " (goal=" + MINUTES_GOAL + ")");
            if (time > goal) {
                break;
            }
        }
    }

    private double parallel(int d1, int d2, int d3, long runs) throws ExecutionException, InterruptedException {
        List<Future<Double>> results = new ArrayList<>(THREADS);
        for (int i = 0 ; i < THREADS ; i++) {
            results.add(executor.submit(new Roller(d1, d2, d3, runs/THREADS)));
        }
        double sum = 0;
        for (Future<Double> result: results) {
            sum += result.get();
        }
        return sum/THREADS;
    }

    public static class Roller implements Callable<Double> {
        private final int d1, d2, d3;
        private final long goal1, goal2, goal3;
        private long d1state, d2state, d3state;
        private final long seed = new Random().nextLong();
        private final Random random = new Random(seed);

        private final long games;
        private long rounds;

        public Roller(int d1, int d2, int d3, long runs) {
            this.d1 = d1;
            this.d2 = d2;
            this.d3 = d3;
            goal1 = (1L << d1)-1;
            goal2 = (1L << d2)-1;
            goal3 = (1L << d3)-1;
            d1state = 0;
            d2state = 0;
            d3state = 0;
            this.games = runs;
        }

        private boolean diesReached() {
            return d1state == goal1 && d2state == goal2 && d3state == goal3;
        }

        @Override
        public Double call() throws Exception {
            for (int game = 0; game < games; game++) {
                while (!diesReached()) {
                    d1state |= 1 << (random.nextInt(d1));
                    d2state |= 1 << (random.nextInt(d2));
                    d3state |= 1 << (random.nextInt(d3));
                    rounds++;
                }
                d1state = 0;
                d2state = 0;
                d3state = 0;
            }
            return 60.0 * rounds / games;
        }
    }

}
