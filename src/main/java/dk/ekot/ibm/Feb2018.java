/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package dk.ekot.ibm;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.*;
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
        //new Roller(2, 3, 4, 10).call();
        new Feb2018();
    }

    /*
    There are three steps to find the optima solution. Read the comments inside of the method.
     */
    public Feb2018() throws Exception {
        // Step 1: Get a ballpark upper number of sides by letting all dices have the same number of sides.
        // This gives me 11
        //findOrigo(MINUTES_GOAL, 1000000);

        // Step 2: Add some margin to the 11 from above and run through all permutations.
        // This mostly gives me [9, 13] with the third dice being very jittery
        //permutate(2, 18, 1_000_000, 10);

        // Step 3: Find the third dice by fixing the first two dices to the best suggestion above.
        // Fixing [9, 13] gives me [3], with [4] as a close second
        //fix2(9, 13, 2, 6, 1*M); // 3 or 4

        // Final result for me was [3, 9, 13]

        // Specifics:
        System.out.println("*** Specific solutions");
        final int SPECIFIC_RUNS = 1_000_000_000;
        System.out.println(parallel(1, 9, 13, SPECIFIC_RUNS));
        System.out.println(parallel(2, 9, 13, SPECIFIC_RUNS));
        System.out.println(parallel(3, 9, 13, SPECIFIC_RUNS));
        System.out.println(parallel(4, 9, 13, SPECIFIC_RUNS));
        System.out.println(parallel(5, 9, 13, SPECIFIC_RUNS));
        System.out.println(parallel(6, 9, 13, SPECIFIC_RUNS));
        // Random
        // Sides= 3/ 9/13, minutes=2568.56, dist=0.4430 (goal=2569, execution time = 141636ms)
        // Sides= 4/ 9/13, minutes=2568.58, dist=0.4248 (goal=2569, execution time = 140249ms)
        // SplittableRandom
        // Sides= 3/ 9/13, minutes=2568.51, dist=0.4892 (goal=2569, execution time = 72252ms)
        // Sides= 4/ 9/13, minutes=2568.67, dist=0.3251 (goal=2569, execution time = 64145ms)

        /* SplittableRandom
Sides= 1/ 9/13, minutes=2568.51, dist=0.4903 (goal=2569, execution time = 18869ms, games=100000000)
Sides= 2/ 9/13, minutes=2568.73, dist=0.2747 (goal=2569, execution time = 18989ms, games=100000000)
Sides= 3/ 9/13, minutes=2568.53, dist=0.4738 (goal=2569, execution time = 23343ms, games=100000000)
Sides= 4/ 9/13, minutes=2568.72, dist=0.2817 (goal=2569, execution time = 20018ms, games=100000000)
Sides= 5/ 9/13, minutes=2569.67, dist=0.6678 (goal=2569, execution time = 25542ms, games=100000000)
           XoRoShiRo128PlusRandom
Sides= 1/ 9/13, minutes=2568.54, dist=0.4610 (goal=2569, execution time = 35,382ms, games=100,000,000)
Sides= 2/ 9/13, minutes=2568.65, dist=0.3516 (goal=2569, execution time = 35,215ms, games=100,000,000)
Sides= 3/ 9/13, minutes=2568.65, dist=0.3467 (goal=2569, execution time = 46,230ms, games=100,000,000)
Sides= 4/ 9/13, minutes=2568.69, dist=0.3148 (goal=2569, execution time = 41,715ms, games=100,000,000)
Sides= 5/ 9/13, minutes=2569.68, dist=0.6780 (goal=2569, execution time = 41,039ms, games=100,000,000)

Sides= 1/ 9/13, minutes=2568.57, dist=0.4268 (goal=2569, execution time=326,389ms, games=1,000,000,000)
Sides= 2/ 9/13, minutes=2568.52, dist=0.4813 (goal=2569, execution time=346,894ms, games=1,000,000,000)
Sides= 3/ 9/13, minutes=2568.57, dist=0.4292 (goal=2569, execution time=409,667ms, games=1,000,000,000)
Sides= 4/ 9/13, minutes=2568.68, dist=0.3192 (goal=2569, execution time=331,381ms, games=1,000,000,000)
Sides= 5/ 9/13, minutes=2569.78, dist=0.7845 (goal=2569, execution time=382,734ms, games=1,000,000,000)
Sides= 6/ 9/13, minutes=2573.74, dist=4.7382 (goal=2569, execution time=388,454ms, games=1,000,000,000)

         */

    }
    public static final int M = 1_000_000;

    private void permutate(int min, int max, int runs, int metaRuns) throws ExecutionException, InterruptedException {
        System.out.println("*** Finding best permutations (~10 minutes)");
        RollResult nearestRoll = null;
        List<RollResult> rolls = new ArrayList<>(metaRuns);
        for (int m = 0 ; m < metaRuns ; m++) {
            final long metaStart = System.nanoTime();
            double nearestDist = Double.MAX_VALUE;
            for (int d1 = min; d1 <= max; d1++) {
                for (int d2 = d1; d2 <= max; d2++) {
                    for (int d3 = d2; d3 <= max; d3++) {
                        RollResult roll = parallel(d1, d2, d3, runs);
                        if (roll.getDistFromIdeal() < nearestDist) {
                            nearestRoll = roll;
                            nearestDist = roll.getDistFromIdeal();
                        }
                    }
                }
            }
            if (nearestRoll != null) {
                nearestRoll = nearestRoll.setTime(System.nanoTime() - metaStart);
                System.out.println("Intermediate: " + nearestRoll);
                rolls.add(nearestRoll);
            }
        }
        System.out.println("*** Best permutations sorted");
        Collections.sort(rolls);
        for (RollResult roll: rolls) {
            System.out.println(roll);
        }
    }

    private void fix2(int f1, int f2, int min, int max, int runs) throws ExecutionException, InterruptedException {
        System.out.println("*** Finding third dice (~10 minutes)");
        List<RollResult> rolls = new ArrayList<>(max-min+1);
        for (int d = min ; d <= max ; d++) {
            final long startTime = System.nanoTime();
            RollResult roll = parallel(d, f1, f2, runs);
            System.out.println(roll);
            rolls.add(roll);
        }
        Collections.sort(rolls);
        System.out.println("*** Third dice sorted");
        for (RollResult roll: rolls) {
            System.out.println(roll);
        }
    }

    private void findOrigo(double goal, int runs) throws ExecutionException, InterruptedException {
        System.out.println("*** Getting ballpark upper limit (<1 minute)");
        for (int d = 2 ; d < Integer.MAX_VALUE ; d++) {
            RollResult roll = parallel(d, d, d, runs);
            System.out.println(roll);
            if (roll.getAverageMinutes() > goal) {
                break;
            }
        }
    }

    private RollResult parallel(int d1, int d2, int d3, int runs) throws ExecutionException, InterruptedException {
        final long startTime = System.nanoTime();
        List<Future<RollResult>> results = new ArrayList<>(THREADS);
        for (int i = 0 ; i < THREADS ; i++) {
            results.add(executor.submit(new Roller(d1, d2, d3, runs/THREADS)));
        }
        double sum = 0;
        for (Future<RollResult> result: results) {
            sum += result.get().getAverageMinutes();
        }
        return new RollResult(d1, d2, d3, runs, sum/THREADS, System.nanoTime()-startTime);
    }

    public static class Roller implements Callable<RollResult> {
        private final int d1, d2, d3;
        private final long d1goal, d2goal, d3goal;
        private long d1state, d2state, d3state;
        private final long seed = new Random().nextLong();
        //private final Random random = new it.unimi.dsi.util.XorShift64StarRandom();
        private final Random random = new XoRoShiRo128PlusRandom();
        //private final SplittableRandom random = new SplittableRandom(seed);
        //private final Random random = new Random(seed);

        private final int games;
        private long rounds;

        public Roller(int d1, int d2, int d3, int runs) {
            this.d1 = d1;
            this.d2 = d2;
            this.d3 = d3;
            d1goal = (1L << d1) - 1;
            d2goal = (1L << d2) - 1;
            d3goal = (1L << d3) - 1;
            d1state = 0;
            d2state = 0;
            d3state = 0;
            this.games = runs;
        }

        private boolean diesReached() {
            return d1state == d1goal && d2state == d2goal && d3state == d3goal;
        }

        @Override
        public RollResult call() throws Exception {
            final long startTime = System.nanoTime();
            for (int game = 0; game < games; game++) {
                while (!diesReached()) {
// Getting a single random and playing with the bits is not faster than getting 3 randoms. Probably because of %
//                    final long r = random.nextLong() & (~1L >>> 1);
//                    d1state |= 1 << (r % d1);
//                    d2state |= 1 << ((r >> 20) % d2);
//                    d3state |= 1 << ((r >> 40) % d3);

                    d1state |= 1 << (random.nextInt(d1));
                    d2state |= 1 << (random.nextInt(d2));
                    d3state |= 1 << (random.nextInt(d3));
                    rounds++;
                }
                d1state = 0;
                d2state = 0;
                d3state = 0;
            }
            return new RollResult(d1, d2, d3, games, 60.0 * rounds / games, System.nanoTime()-startTime);
        }
    }

    private static class RollResult implements Comparable<RollResult> {
        private final int d1, d2, d3;
        private final int games;
        private final double averageMinutes;
        private final long wallTimeNS;

        private final double distFromIdeal;

        public RollResult(int d1, int d2, int d3, int games, double averageMinutes, long wallTimeNS) {
            int[] sorted = new int[3];
            sorted[0] = d1;
            sorted[1] = d2;
            sorted[2] = d3;
            Arrays.sort(sorted);
            this.d1 = sorted[0];
            this.d2 = sorted[1];
            this.d3 = sorted[2];

            this.games = games;
            this.averageMinutes = averageMinutes;
            this.wallTimeNS = wallTimeNS;
            distFromIdeal = Math.abs(MINUTES_GOAL - averageMinutes);
        }

        public double getAverageMinutes() {
            return averageMinutes;
        }

        public double getDistFromIdeal() {
            return distFromIdeal;
        }

        @Override
        public int compareTo(RollResult o) {
            return distFromIdeal < o.distFromIdeal ? -1 : 1;
        }

        @Override
        public String toString() {
            return String.format(
                    "Sides=%2d/%2d/%2d, minutes=%.2f, dist=%1.4f (goal=%d, execution time=%,dms, games=%,d)",
                    d1, d2, d3, averageMinutes, distFromIdeal, MINUTES_GOAL, wallTimeNS/1000000, games);
        }

        public RollResult setTime(long ns) {
            return new RollResult(d1, d2, d3, games, averageMinutes, ns);
        }
    }
}
