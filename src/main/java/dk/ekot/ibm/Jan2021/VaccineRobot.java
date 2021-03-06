/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.ekot.ibm.Jan2021;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/*
 *   https://www.research.ibm.com/haifa/ponderthis/challenges/January2021.html
 */

/*
 * Instructions
 *
 * Build with
 *   mvn clean compile assembly:single -DskipTests
 * Run with
 *   java -jar target/ponder-this-0.1-SNAPSHOT-jar-with-dependencies.jar -h
 * to get usage.
 *
 * For larger grids (200+) you probably want some feedback while processing. Running
 *   VERBOSE=true java -jar target/ponder-this-0.1-SNAPSHOT-jar-with-dependencies.jar 20 200 250 | tee results.txt
 * searches for solutions in grids 200-250.
 *
 * The VERBOSE part triggers outputting a "." on stderr for each starting antibot until the first column has been
 * searched for antibots, where both antibots in column 0. After that the strategy is shifted to exhaustive search,
 * where each starting antibot results in "<antiindex(antiX,antiY)>" (e.g. "<1910(4,198)>") on stderr.
 * Found solutions (not the verbose output) will be collected in "results.txt".
 */

/*
 *
 * TODO: Plot processing times
 * TODO: Investigate shortest & longest distance between first antibot and second
 * TODO: Add corner search
 * TODO: Investigate secondary bot placement for the different strategies
 */
/*
 * Optimization timeline (by memory)
 *
 * Grid as int[] instead of int[][] for memory locality
 * Inner class: Direction as int to avoid conditionals (extract with &)
 * Start at left corner of rhombus (does not help with 250+ grids)
 * Start by searching for pairs in column 0, with fallback to full search (this helped a lot)
 * Inner structure: Grid as byte[] for better caching
 * If there are no matches in column 0, there probably is at the last row
 * Inner loop: Remove a few conditionals, use ++i instead of i++, refactor to a single method
 */
@SuppressWarnings("SameParameterValue")
public class VaccineRobot {
    private static Log log = LogFactory.getLog(VaccineRobot.class);

    // Overall principle is that each tile on the grid holds bits for its state

    private static boolean verbose = System.getenv().containsKey("VERBOSE") && Boolean.parseBoolean(System.getenv().get("VERBOSE"));
    //private static Integer minC0 =
    private static Integer minC0 = getEnvInt("MIN_C0");
    private static Integer maxC0 = getEnvInt("MAX_C0");
    private static Integer minRM = getEnvInt("MIN_RM");
    private static Integer maxRM = getEnvInt("MAX_RM");
    private static Integer getEnvInt(String key) {
        return System.getenv().containsKey(key) ? Integer.parseInt(System.getenv().get(key)) : null;
    }

    private static final ThreadFactory ghostFactory = new ThreadFactory() {
        AtomicInteger threadID = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "vaccine_" + threadID.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    };
    private static final int DEFAULT_THREADS =
            Runtime.getRuntime().availableProcessors() < 2 ? 1 : Runtime.getRuntime().availableProcessors()/2;

    public static final String USAGE =
            "Usage: VaccineRobot <threads> <minSide> [maxSide]\n" +
            "  -g <side>\n" +
            "     Show the result of a run without bots\n\n" +

            "  -G <side> [antiIndex [maxX [threads]]]\n" +
            "     Find & show all valid antibot-pairs\n\n" +

            "  -n <side> [threads [antiCount [antiIndex]]]\n" +
            "     Find & show all valid antibot-pairs without any antibots in column 0\n\n" +

            "  -s <minSide> [maxSide [threads]]\n" +
            "     Print statistics for antibots in selcted columns & rows\n\n" +

            "  -d <side> [threads [antiCount [firstAnti0Index (default 0) [lastAnti0Index (default side^2-1)]]]]\n" +
            "     Deep-dive (not preliminary column-0 check)\n\n" +

            "Modifiers:\n" +
            "  VERBOSE=true      Print status at regular intervals\n" +
            "  MIN_C0=true The minimum amount of antibots in column0 to perform a walk\n" +
            "  MAX_C0=true The maximum amount of antibots in column0 to perform a walk\n" +
            "  MIN_RM=true The minimum amount of antibots in the bottom row to perform a walk\n" +
            "  MAX_RM=true The maximum amount of antibots in the bottom row to perform a walk\n";

    // 0, 1 & 2 are simply received vaccine doses
    public static void main(String[] args) {
        if (args.length == 0) {
            fallbackMain();
            return;
        }

        // Show grid with no antis
        if ("-g".equals(args[0]) && args.length == 2) {
            showEmpty(Integer.parseInt(args[1]));
            return;
        }

        // Show grid with antis
        if ("-G".equals(args[0]) && args.length >= 2) {
            if (args.length == 2) {
                showMatches(Integer.parseInt(args[1]), 2);
                return;
            }
            if (args.length == 3) {
                showMatches(Integer.parseInt(args[1]), 2, Integer.parseInt(args[2]), -1);
                return;
            }
            if (args.length == 4) {
                showMatches(Integer.parseInt(args[1]), 2, Integer.parseInt(args[2]), Integer.parseInt(args[3]));
                return;
            }
            if (args.length == 5) {
                showMatches(Integer.parseInt(args[1]), 2, Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                return;
            }
            System.err.println("Too many arguments for -G");
            System.out.println(USAGE);
            System.exit(10);
        }

        // No antibots in column 0
        if ("-n".equals(args[0])) {
            if (args.length < 2 || args.length > 5) {
                System.err.println("Wrong number of arguments for -n");
                System.out.println(USAGE);
                System.exit(11);
            }
            int side = getInt(args, 1, -1);
            int threads = getInt(args, 2, DEFAULT_THREADS);
            int antis = getInt(args, 3, 2);
            int showAntiIndex = getInt(args, 4, -1);
            showMatchesNot0(side, antis, showAntiIndex, threads);
            return;
        }

        // Statistics
        if ("-s".equals(args[0])) {
            if (args.length < 2 || args.length > 4) {
                System.err.println("Wrong number of arguments for -s");
                System.out.println(USAGE);
                System.exit(16);
            }
            int minSide = getInt(args, 1, -1);
            int maxSide = getInt(args, 2, -1);
            int threads = getInt(args, 3, DEFAULT_THREADS);
            positionStats(minSide, maxSide, threads);
            return;
        }

        // Deep-dive
        if ("-d".equals(args[0])) {
            if (args.length < 2 || args.length > 6) {
                System.err.println("Wrong number of arguments for -d");
                System.out.println(USAGE);
                System.exit(11);
            }
            int side = getInt(args, 1, -1);
            int threads = getInt(args, 2, DEFAULT_THREADS);
            int antis = getInt(args, 3, 2);
            int firstAntiIndex = getInt(args, 4, 0);
            int lastAntiIndex = getInt(args, 5, side*side-1);
            deepDive(side, threads, antis, firstAntiIndex, lastAntiIndex);
            return;
        }

        // No arguments is plain processing
        if (args.length < 2 || args.length > 3) {
            System.err.println("Wrong number of arguments");
            System.out.println(USAGE);
            System.exit(12);
        }

        int threads = getInt(args, 0, DEFAULT_THREADS);
        int minSide = getInt(args, 1, -1);
        int maxSide = getInt(args, 2, minSide);
        System.out.printf(Locale.ENGLISH, "threads=%d, minSide=%d, maxSide=%d%n",
                          threads, minSide, maxSide);
        threaded(threads, minSide, maxSide, 3);
    }

    private static int getInt(String[] args, int index, int defaultValue) {
        if (index >= args.length) {
            return defaultValue;
        }
        return Integer.parseInt(args[index]);
    }

    public static void fallbackMain() {
        //threaded(6, 4, 20, 3);
        //threaded(1, 12, 12, 3);
        //showMatches(30, 2, -1, 0);

//        noAnyC0 = true;
//        threaded(6, 20, 20, 2);
        showMatches(30, 2, -1, 30);

        //threaded(4, 124, 127, 3);
//         threaded(4, 4, 300, 3);

        //showMatches(20, 2, -1, -1, 4);

       // positionStats(40, 45, 4);

        //showMatchesExactly0(20, 2, -1, 2, 2);
        //showMatchesExactly0(20, 2, -1, 2, 1);
        //showMatchesExactly0(20, 2, -1, 2, 0);
        //checkSpecific(50, 0, 49, 46, 3);

//        findMiddle();
   //     threaded(4, 170, 300, 3);
//        threaded(4, 4, 200, 3);
//        threaded(4, 4, 100, 3);
        //threaded(4, 232, 232, 3);
        //empties();

//        timeFlatVsTopD();

        //timeTopD(30, 32);
        //deepDive(20, 4, 2, 0, 20*20-1);
  //      for (int r = 0 ; r < 3 ; r++) {
//            deepDive(355, 8, 2, 355 * 355 - 100, 355 * 355);
  //      }
        // 133s, 144, 134, 134, 130 | 132, 132, 126 | 100, 97, 96 | 94, 93, 87
        // 1:30 (origo), 1:17 (byte), 1:08 (code cleanup 1)

//        timeTopD(64, 64); // 3200, 3500, 3450,
        //timeTopD(30, 60); // 1800, 2000, 1840, 1850 | 2023, 1840
        //flatCheck(38, Arrays.asList(new Pos(0, 2), new Pos(5, 28)));

        //[TopD(355, 355) ms=366,505,420, antis=2: [(  7, 329), ( 80, 298)]]
        //flatCheck(355, Arrays.asList(new Pos(7, 329), new Pos(80, 298)));

        //countMatches(13, 2);

        // Problem childs:
        // TopD( 64,  64) ms=     7,257, antis=2: [(  0,  30), (  0,  38)], startYs=[24, 25] *
        // TopD( 67,  67) ms=     1,490, antis=2: [(  0,  27), (  0,  42)], startYs=[26] *
        // TopD( 74,  74) ms=     3,539, antis=2: [(  0,  30), (  0,  53)], startYs=[28, 29] *
        // TopD(126, 126) ms=116806, antis=2: [(0, 56), (18, 79)], startY=47 *
        // 24 is atypical: No antis to the left of 11 although the empty is there
        // 38 same
        //for (int side = 10 ; side <= 40 ; side++) {
        //    showMatches(side, 2);
        //}
//        showMatches(13, 2);
//        showMatches(24, 2);
//        showMatches(38, 2);

//        showMatches(30, 2);
  //      showMatches(35, 2);
        //showAll(10, 2);

//        test4();

//        Grid grid = new Grid(4);
//        grid.mark(new Pos(1, 0));
//        System.out.println(grid.fullRun());
//        System.out.println(grid);
    }

    private static void checkSpecific(int side, int a0x, int a0y, int a1x, int a1y) {
        FlatGrid grid = new FlatGrid(side);
        grid.setMarks(new Pos(a0x, a0y), new Pos(a1x, a1y));
        System.out.printf(Locale.ENGLISH, "grid=%dx%d, antis=[(%d, %d), (%d, %d)]: %b%n",
                          side, side, a0x, a0y, a1x, a1y, grid.fullRun());
    }

    private static void findMiddle() {
        for (int side: new int[]{64, 74, 96, 98, 110, 112}) {
            threaded(4, side, side, 3);
            showEmpty(side);
            System.out.println("startYs: " + guessStartYs(side, side));
            System.out.println("*********************************************************");
        }
    }

    /**
     * Creates an empty grid and runs it. Finds the first row with a 0 count to the farthest left.
     * Top-down iterations has a high chance of matches by starting there.
     */
    private static List<Integer> guessStartYs(int width, int height) {
        FlatGrid grid = new FlatGrid(width, height);
        if (grid.fullRun()) {
            return Collections.singletonList(0);
        }
        return grid.getRowsForLeftmostNontouched();
    }

    private static void showEmpty(int side) {
        FlatGrid grid = new FlatGrid(side);
        grid.fullRun();
        show(grid);
    }

    private static void show(FlatGrid grid) {
        String[] lines = visualiseGridNoCount(grid).split("\n");
        for (int i = 0 ; i < lines.length ; i++) {
            System.out.printf(Locale.ENGLISH, "%3d %s%n", i, lines[i]);
        }
    }

    private static void flatCheck(int side, List<Pos> antis) {
        FlatGrid flat = new FlatGrid(side);
        flat.setMarks(antis.toArray(new Pos[0]));
        System.out.println("Flat: side=" + side + ", antis=" + antis + " returns " + flat.fullRun());
    }

    private static void timeTopD(int minSide, int maxSide) {
        int RUNS = 5;
        int SKIPS = 1;
        long TopDMS = 0;
        for (int r = 0 ; r < RUNS ; r++) {
            long tdms = -System.currentTimeMillis();
            threaded(1, minSide, maxSide, 3, Collections.singletonList(VaccineRobot::systematicFlatTD));
            tdms += System.currentTimeMillis();

            if (r >= SKIPS) {
                TopDMS += tdms;
            }
        }
        System.out.println("Average TopD-time: " + TopDMS/(RUNS-SKIPS));
    }

    private static void positionStats(int minSide, int maxSide, int threads) {
        for (int side = minSide ; side <= maxSide ; side++) {
            positionStats(side, threads, side == minSide);
        }
    }
    private static void positionStats(int side, int threads, boolean showHeader) {
        ExecutorService executor = Executors.newFixedThreadPool(threads, ghostFactory);
        long startMS = System.currentTimeMillis();
        final int antiCount = 2;
        int[][] walks = WalkPlanner.getWalksFull(side, side, antiCount, Collections.singletonList(0));

        List<Match> matches = getMatchesTD(
                side, side, antiCount, Integer.MAX_VALUE, Collections.singletonList(0), walks, executor,
                false, threads,
                false, 0, side*side-1);
        final int[][] cornerGrid = getCornerGrid(side);

        int[] c0 = new int[3];
        int[] rM = new int[3];
        int[] c = new int[5]; // Corners: Not, TL, TR, BR, BL
        matches.forEach(match -> {
            int col0antis = (int) match.antis.stream().filter(anti -> anti.x == 0).count();
            c0[col0antis]++;
            if (col0antis ==0) {
                int bMantis = (int) match.antis.stream().filter(anti -> anti.y == side - 1).count();
                rM[bMantis]++;
                if (bMantis == 0) {
                    match.antis.forEach(anti -> c[cornerGrid[anti.x][anti.y]]++);
                }
            }
        });

        if (showHeader) {
            System.out.println("    side  col0=2  col0=1  rowM=2  rowM=1      tl      tr      br      bl     non   break     all  col0=0  rowM=0        ms");
        }
        System.out.printf("%8d" +
                          "%8d%8d%8d%8d" +
                          "%8d%8d%8d%8d%8d%8d" +
                          "%8d%8d%8d%,10d%n",
                          side,
                          c0[2], c0[1], rM[2], rM[1],
                          c[1], c[2], c[3], c[4], c[0], matches.size()/side,
                          matches.size(), c0[0], rM[0], System.currentTimeMillis()-startMS);
    }

    private static int getCorner(int side, Pos anti) {
        return 0;  // TODO: Implement this
    }
    // Not, TL, TR, BR, BL
    private static int[][] getCornerGrid(int side) {
        int[][] grid = new int[side][side];
        int cornerSide = (int) Math.sqrt(side); // Same area as a colums (or row)
        for (int vertical = 0 ; vertical < cornerSide ; vertical++) {
            for (int horizontal = 0 ; horizontal < cornerSide-vertical ; horizontal++) {
                grid[horizontal][vertical] = 1;
                grid[side-1-horizontal][vertical] = 2;
                grid[side-1-horizontal][side-1-vertical] = 3;
                grid[horizontal][side-1-vertical] = 4;
            }
        }
        return grid;
    }

    // Observation: It seems that the first column has more viable antis than first row. So top-down instead of left-right oriented might be faster?
    private static void showMatches(int side, int antis) {
        showMatches(side, antis, -1, -1);
    }
    private static void showMatches(int side, int antis, int showAntiIndex, int maxXForAnti0) {
        showMatches(side, antis, showAntiIndex, maxXForAnti0, DEFAULT_THREADS);
    }
    private static void showMatches(int side, int antis, int showAntiIndex, int maxXForAnti0, int threads) {
        final int fMax0 = maxXForAnti0 < 0 || maxXForAnti0 > side-1 ? side-1 : maxXForAnti0;
        showMatches(side, antis, showAntiIndex, threads, () -> {
            int[][] walks = new int[antis][];
            for (int antiIndex = 0 ; antiIndex < antis ; antiIndex++) {
                walks[antiIndex] = antiIndex != 0 ? new int[side*side] : new int[side*(fMax0+1)];
                int maxX = antiIndex == 0 ? fMax0 : side-1;
                int walkIndex = 0;
                for (int x = 0 ; x <= maxX ; x++) {
                    for (int y = 0 ; y < side ; y++) {
                        walks[antiIndex][walkIndex++] = x + y*side;
                    }
                }
            }
            return walks;
        }, null);
    }

    private static void showMatchesExactly0(int side, int antis, int showAntiIndex, int threads, int antisIn0) {
        if (antisIn0 == 0) {
            showMatchesNot0(side, antis, showAntiIndex, threads); // Optimized
            return;
        }
        showMatches(side, antis, showAntiIndex, threads, null, (match) ->
            match.antis.stream().filter(pos -> pos.y == 0).count() == antisIn0);
    }

    private static void showMatchesNot0(int side, int antis, int showAntiIndex, int threads) {
        System.out.printf("showMatchesNot0(side=%d, antis=%d, showAntiIndex=%d, threads=%d)%n",
                          side, antis, showAntiIndex, threads);
        showMatches(side, antis, showAntiIndex, threads, () -> {
            int[] walkNot0 = new int[side*(side-1)];
            int walkIndex = 0;
            for (int x = 1 ; x < side ; x++) {
                for (int y = 0 ; y < side ; y++) {
                    walkNot0[walkIndex++] = x + y*side;
                }
            }
            return WalkPlanner.asWalks(walkNot0, antis);
        }, null);
    }


    private static void showMatches(int side, int antis, int showAntiIndex, int threads,
                                    Supplier<int[][]> walkProducer, Predicate<Match> matchFilter) {
        final long startMS = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(threads, ghostFactory);
        matchFilter = matchFilter != null ? matchFilter : match -> true;
        walkProducer = walkProducer != null ? walkProducer : () -> {
            // Walk everything
            int[][] walks = new int[antis][];
            int[] walk = WalkPlanner.getWalkRowBased(side, side);
            for (int antiIndex = 0 ; antiIndex < antis ; antiIndex++) {
                walks[antiIndex] = walk;
            }
            return walks;
        };

        List<Match> matches = getMatchesTD(side, side, antis, Integer.MAX_VALUE, Collections.singletonList(0),
                                           walkProducer.get(),
                                           executor, false, threads,
                                           false, 0, side*side-1);

        FlatGrid flat = new FlatGrid(side);
        flat.fullRun();

        List<List<Pos>> matchFilteredPos = new ArrayList<>(matches.size());
        matches.stream().
                filter(matchFilter).
                peek(match -> matchFilteredPos.add(new ArrayList<>(match.antis))). // Copy as it might be modified later
                peek(match -> {
                    if (showAntiIndex != -1) {
                        List<Pos> newAntis = Collections.singletonList(match.antis.get(showAntiIndex));
                        match.antis.clear();
                        match.antis.addAll(newAntis);
                    }}).
                forEach(match -> flat.addMarks(match.antis.toArray(new Pos[0])));

        show(flat);
        matchFilteredPos.forEach(antiList -> System.out.println(side + ": " + antiList));
        System.out.printf(Locale.ENGLISH, "startYs: %s, time=%,dms%n",
                          guessStartYs(side, side), System.currentTimeMillis()-startMS);
        System.out.println("****************");
    }

    private static String visualiseGridNoCount(FlatGrid flat) {
        return flat.toString().replace("1", " ").replace("2", " ").replace("0", "-");
    }

//    private static void countMatches(int side, int antis) {
//        System.out.println(
//                "Matches for " + side + "x" + side + ": "+ systematicFlat(side, side, antis, Integer.MAX_VALUE).size());
//    }

    private static void showAll(int side, int antis) {
        FlatGrid flat = new FlatGrid(side);
        flat.fullRun();


    }

    private static void empties() {
        for (int side = 4 ; side <= 50 ; side++) {
            showEmpty(side);
        }
    }

    // ------------------------------------------------------------------------------------------------------

    private static void threaded(int threads, int minSide, int maxSide, int maxAntis) {
        threaded(threads, minSide, maxSide, maxAntis, Collections.singletonList(VaccineRobot::systematicFlatTD));
    }
    private static void threaded(int threads, int minSide, int maxSide, int maxAntis, List<Systematic> strategies) {
        ExecutorService executor = Executors.newSingleThreadExecutor(ghostFactory); // TODO: What should this be?
        ExecutorService gridExecutor = Executors.newFixedThreadPool(threads, ghostFactory);
        List<Future<String>> jobs = new ArrayList<>(maxSide-minSide+1);
        for (int side = minSide ; side <= maxSide ; side++) {
            final int finalSide = side;
            jobs.add(executor.submit(() -> {
                for (int antis = 2 ; antis <= maxAntis ; antis++) { // We could do 1, but it is only realistic for small grids
                    int finalAntis = antis;
                    List<List<Match>> results = strategies.stream().
                            map(strategy -> strategy.process(finalSide, finalSide, finalAntis, 1, gridExecutor, threads)).
                            collect(Collectors.toList());
                    if (results.get(0) != null && !results.get(0).isEmpty()) {
                        return results.stream().
                                map(matches -> matches.get(0)).
                                map(Match::toString).
                                collect(Collectors.joining("\n"));
                    }
                }
                return null;
            }));
        }
        jobs.forEach(job -> {
            try {
                System.out.println(job.get());
            } catch (Exception e) {
                log.error("Failed job", e);
            }
        });
        executor.shutdown();
    }

    private static void deepDive(int side, int threads, int antis, int firstAntiIndex, int lastAntiIndex) {
        deepDive(side, threads, antis, firstAntiIndex, lastAntiIndex, VaccineRobot::systematicFlatTD);
    }
    private static void deepDive(int side, int threads, int antis, int firstAnti0Index, int lastAnti0Index, Systematic strategy) {
        if (verbose) {
            System.out.printf(Locale.ENGLISH,
                              "Starting DeepDive(side=%d, threads=%d, antis=%d, firstAntiIndex=%d, lastAntiIndex=%d)",
                              side, threads, antis, firstAnti0Index, lastAnti0Index);
        }
        ExecutorService gridExecutor = Executors.newFixedThreadPool(threads, ghostFactory);
        long time = -System.currentTimeMillis();
        List<Match> matches = strategy.process(side, side, antis, 1, gridExecutor, threads, firstAnti0Index, lastAnti0Index);
        time += System.currentTimeMillis();
        System.out.printf(Locale.ENGLISH, "side=%d, threads=%d, antis=%d, firstAntiIndex=%d, lastAntiIndex=%d, " +
                                          "wallTime=%,dms, CPUTime=%,dms, match=%s%n",
                          side, threads, antis, firstAnti0Index, lastAnti0Index,
                          time, time*threads, matches.isEmpty() ? "none" : matches);
    }

    /* ****************************************************************************************************************/

    private static void test4() {
        Grid grid = new Grid(4);
        grid.setMarks(new Pos(3, 0));
//        System.out.println(grid.fullRun());
//        System.out.println(grid);

        FlatGrid flat = new FlatGrid(4);
        flat.setMarks(new Pos(3, 0));
//        System.out.println(flat.fullRun());
//        System.out.println(flat);

        while (grid.hasNext()) {
            System.out.println("****************");
            grid.next();
            System.out.println(grid);
            System.out.println(grid.hasNext() + " -- " + grid.allImmune());
            flat.next();
            System.out.println(flat);
            System.out.println(flat.hasNext() + " " + grid.allImmune());
        }
    }

    @FunctionalInterface
    public interface Systematic {
        List<Match> process(int width, int height, int antiCount, int maxMatches, ExecutorService executor, int threads, int... hints);
    }

    // hints = [startAnti0Index, [endAnti0Index]
    private static List<Match> systematicFlatTD(int width, int height, int antiCount, int maxMatches, ExecutorService executor, int threads, int... hints) {
        return hints.length == 0 ?
                systematicFlatTDMulti(width, height, antiCount, maxMatches, executor, threads, true) :
                systematicFlatTDLimited(width, height, antiCount, maxMatches, executor, threads, hints[0], hints.length > 1 ? hints[1] : width*height-1);
    }
    // hints = [startAnti0Index, [endAnti0Index]
    private static List<Match> systematicFlatTDMulti(int width, int height, int antiCount, int maxMatches, ExecutorService executor, int threads, boolean optimize) {
        List<Integer> startYs = optimize ? guessStartYs(width, height) : Collections.singletonList(0);

        //int[][] firstRow = getWalksFirstRowOnly(width, height, antiCount, startYs);
        int[][] firstRow = WalkPlanner.getWalksFirstRowOnly(width, height, antiCount);
        List<Match> firstRowMatches = getMatchesTD(width, height, antiCount, maxMatches, startYs, firstRow,
                                                   executor, false, threads,
                                                   false, 0, width*height-1);
        if (firstRowMatches.size() >= maxMatches) {
            return firstRowMatches;
        }
        if (verbose) {
            System.err.printf(Locale.ENGLISH, "(%d, %d /%d)_switchToFull_with_%d_matches",
                              width, height, antiCount, firstRowMatches.size());
        }

        //int[][] full = WalkPlanner.getWalksFull(width, height, antiCount, startYs);
        int[][] full = WalkPlanner.getWalksFullOrdered(width, height, antiCount);
        return getMatchesTD(width, height, antiCount, maxMatches, startYs, full,
                            executor, true, threads,
                            true, 0, width*height-1);
    }

    private static List<Match> systematicFlatTDLimited(
            int width, int height, int antiCount, int maxMatches, ExecutorService executor, int threads,
            int firstAnti0Index, int lastAnti0Index) {
        List<Integer> startYs = Collections.singletonList(0);

        int[][] full = WalkPlanner.getWalksFull(width, height, antiCount, startYs);
        return getMatchesTD(width, height, antiCount, maxMatches, startYs, full,
                            executor, firstAnti0Index != 0, threads,
                            false, firstAnti0Index, lastAnti0Index);
    }

    /**
     *
     * @param width           grid width.
     * @param height          grid height.
     * @param antiCount       the number of anitbots.
     * @param maxMatches      the maximum number of matches to find before returning.
     * @param startYs         startingY positions, only used for feedback.
     * @param walks           the walks to follow.
     * @param executor        the executor for the threads.
     * @param printAnti0      if true and verbose==true, detailed debug is written for each antibot0 position
     * @param threads         the number of threads to use.
     * @param skipCol0Pairs   if true, the test is skipped if all bots are in column 0.
     * @param firstAnti0Index start index in walks.
     * @param lastAnti0Index  end index in walks.
     * @return the matches from the run.
     */
    private static List<Match> getMatchesTD(int width, int height, int antiCount, int maxMatches,
                                            List<Integer> startYs, int[][] walks,
                                            ExecutorService executor, boolean printAnti0, int threads,
                                            boolean skipCol0Pairs,
                                            int firstAnti0Index, int lastAnti0Index) {
        final List<Match> matches = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger zeroBotIndex = new AtomicInteger(firstAnti0Index);
        AtomicBoolean continueWalk = new AtomicBoolean(true);

        if (verbose) {
            System.err.printf(
                    Locale.ENGLISH, "\ngrid(%d, %d /%d), 0=(%d, %d), anti0Index=%d..%d:",
                    width, height, antiCount, walks[0][0]%width, walks[0][0]/width, firstAnti0Index, lastAnti0Index);
        }

        List<Future<List<Match>>> jobs = new ArrayList<>(threads);
        for (int thread = 0 ; thread < threads ; thread++) {
            jobs.add(executor.submit(() -> {
                FlatGrid empty = new FlatGrid(width, height);
                FlatGrid grid = new FlatGrid(width, height, "TopD", startYs);

                systematicFlatTDSingle(grid, empty, new int[antiCount], 0, walks,
                                       zeroBotIndex, skipCol0Pairs, lastAnti0Index, continueWalk, printAnti0,
                                       threads, match -> {
                            matches.add(match);
                            if (matches.size() >= maxMatches) {
                                continueWalk.set(false); // Signal stop to other workers
                            }
                            return matches.size() < maxMatches;
                        });
                return matches;
            }));
        }

        // Wait for jobs to finish until we have found enough matches
        for (Future<List<Match>> job: jobs) {
            if (matches.size() >= maxMatches) {
                break; // No need to wait more
            }
            try {
                job.get(); // Worst case is still poor. Maybe busy wait for matches to be ok or all jobs isDone()
            } catch (Exception e) {
                System.err.println(
                        "Exception retrieving job result for " + width + "x" + height + ": " + e.getMessage());
            }
        }
        if (verbose) {
            System.err.println("\n" + matches);
        }
        return matches;
    }

    private static boolean systematicFlatTDSingle(
            FlatGrid grid, FlatGrid empty, final int[] antis, int antiIndex,
            int[][] walks, AtomicInteger walkOrigo, boolean skipCol0Pairs, int lastAnti0Index,
            AtomicBoolean continueWalk, boolean printAnti0,
            int threads, Predicate<Match> collect) {
        if (antis.length > 1 && antiIndex == antis.length-1) { // Ready to iterate last anti
            empty.clear();
            for (int i = 0 ; i < antis.length-1 ; i++) {
              empty.addMarks(antis[i]);
            }
            empty.fullRun();
        }

        final int[] walk = walks[antiIndex];
        int walkIndex;

        while (continueWalk.get() && (walkIndex = walkOrigo.getAndIncrement()) < walk.length) {
            if (antiIndex == 0 && walkIndex > lastAnti0Index) {
                break;
            }
            if (verbose && antiIndex == 0) {
                System.err.print(printAnti0 ? "<" + walkIndex + Pos.fromInt(walk[walkIndex], grid.width, grid.height).toString().replace(" ", "") + ">" :".");
            }
            antis[antiIndex] = walk[walkIndex];
            if (antiIndex < antis.length - 1) { // More antis to go
                if (!systematicFlatTDSingle(
                        grid, empty, antis, antiIndex + 1,
                        walks, new AtomicInteger(walkIndex+1), skipCol0Pairs, lastAnti0Index,
                        continueWalk, printAnti0,
                        threads, collect)) {
                    return false;
                }
                if (verbose && antiIndex == 0) {
                    System.err.print(printAnti0 ? "</" + walkIndex + ">" :".");
                }
                continue;
            }

            // Reached the bottom

            // Check for bots in column 0
            if (skipCol0Pairs || minC0 != null || maxC0 != null) {
                int col0Count = 0;
                for (int anti : antis) {
                    if (anti % grid.width == 0) { // Column 0
                        col0Count++;
                    }
                }
                if ((skipCol0Pairs && col0Count == antis.length) ||
                    (minC0 != null && col0Count < minC0) ||
                    (maxC0 != null && col0Count > maxC0)) {
                    continue;
                }
            }

            // Check for bots in bottom row
            if (minRM != null || maxRM != null) {
                int rmCount = 0;
                int bottomRowOrigo = (grid.height-1)*grid.width;
                for (int anti : antis) {
                    if (anti >= bottomRowOrigo) {
                        rmCount++;
                    }
                }
                if ((minRM != null && rmCount < minRM) ||
                    (maxRM != null && rmCount > maxRM)) {
                    continue;
                }
            }

            // Check if the last anti is on a position where it will change things
            if (!empty.isPositionMarked(antis[antis.length-1])) {
                continue; // majorLoop
            }

            grid.clear();
            grid.setMarks(antis);
            if (grid.fullRun()) {
                if (!collect.test(grid.getMatch(threads))) {
                    return false;
                }
//                String.format(
//                        Locale.ENGLISH, "Flat(%3d, %3d) moves=%6d, ms=%6d, antis=%d: %s%n",
//                        grid.width, grid.height, grid.move, grid.lastRunMS, antis.length, toPosList(antis, grid.width, grid.height));
            }
        }
        // TODO: Also handle antis.length == 0
        return continueWalk.get();
    }

    /*
/usr/lib/jvm/java-1.11.0-openjdk-amd64/bin/java -javaagent:/home/te/bin/installs/idea-IU-192.7142.36/lib/idea_rt.jar=35699:/home/te/bin/installs/idea-IU-192.7142.36/bin -Dfile.encoding=UTF-8 -classpath /home/te/projects/ponder-this/target/classes:/home/te/.m2/repository/com/ibm/icu/icu4j/59.1/icu4j-59.1.jar:/home/te/.m2/repository/commons-logging/commons-logging/1.1/commons-logging-1.1.jar:/home/te/.m2/repository/javax/servlet/servlet-api/2.3/servlet-api-2.3.jar:/home/te/.m2/repository/it/unimi/dsi/dsiutils/2.4.2/dsiutils-2.4.2.jar:/home/te/.m2/repository/it/unimi/dsi/fastutil/8.1.0/fastutil-8.1.0.jar:/home/te/.m2/repository/com/martiansoftware/jsap/2.1/jsap-2.1.jar:/home/te/.m2/repository/com/google/guava/guava/23.2-jre/guava-23.2-jre.jar:/home/te/.m2/repository/com/google/code/findbugs/jsr305/1.3.9/jsr305-1.3.9.jar:/home/te/.m2/repository/com/google/errorprone/error_prone_annotations/2.0.18/error_prone_annotations-2.0.18.jar:/home/te/.m2/repository/com/google/j2objc/j2objc-annotations/1.1/j2objc-annotations-1.1.jar:/home/te/.m2/repository/org/codehaus/mojo/animal-sniffer-annotations/1.14/animal-sniffer-annotations-1.14.jar:/home/te/.m2/repository/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar:/home/te/.m2/repository/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar:/home/te/.m2/repository/ch/qos/logback/logback-core/1.2.3/logback-core-1.2.3.jar:/home/te/.m2/repository/commons-configuration/commons-configuration/1.10/commons-configuration-1.10.jar:/home/te/.m2/repository/commons-lang/commons-lang/2.6/commons-lang-2.6.jar:/home/te/.m2/repository/commons-io/commons-io/2.5/commons-io-2.5.jar:/home/te/.m2/repository/commons-collections/commons-collections/20040616/commons-collections-20040616.jar:/home/te/.m2/repository/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar:/home/te/.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar:/home/te/.m2/repository/junit/junit/4.10/junit-4.10.jar:/home/te/.m2/repository/org/hamcrest/hamcrest-core/1.1/hamcrest-core-1.1.jar dk.ekot.ibm.VaccineRobot
Flat(  4,   4) moves=    51, ms=     0, antis=1: [(3, 0)]
Flat(  5,   5) moves=    71, ms=     0, antis=0: []
Flat(  6,   6) moves=   139, ms=     0, antis=1: [(1, 0)]
Flat(  7,   7) moves=   200, ms=     1, antis=1: [(1, 0)]
Flat(  8,   8) moves=   263, ms=     9, antis=1: [(5, 6)]
Flat(  9,   9) moves=   319, ms=     2, antis=1: [(6, 7)]
Flat( 10,  10) moves=   451, ms=     1, antis=1: [(9, 1)]
Flat( 11,  11) moves=   485, ms=     0, antis=1: [(0, 4)]
Flat( 12,  12) moves=   894, ms=     0, antis=2: [(0, 0), (9, 0)]
Flat( 13,  13) moves=   887, ms=    13, antis=2: [(1, 0), (3, 0)]
Flat( 14,  14) moves=   967, ms=    40, antis=2: [(1, 0), (11, 2)]
Flat( 15,  15) moves=  1181, ms=    30, antis=1: [(10, 12)]
Flat( 16,  16) moves=  1555, ms=    19, antis=2: [(0, 0), (0, 14)]
Flat( 17,  17) moves=  1838, ms=     2, antis=2: [(0, 0), (16, 13)]
Flat( 18,  18) moves=  1976, ms=   137, antis=2: [(13, 0), (0, 5)]
Flat( 19,  19) moves=  1795, ms=     3, antis=1: [(0, 18)]
Flat( 20,  20) moves=  2354, ms=     1, antis=2: [(0, 0), (19, 1)]
Flat( 21,  21) moves=  2411, ms=     1, antis=2: [(0, 0), (2, 1)]
Flat( 22,  22) moves=  3798, ms=    17, antis=2: [(1, 0), (0, 6)]
Flat( 23,  23) moves=  3679, ms=    10, antis=1: [(22, 22)]
Flat( 24,  24) moves=  4502, ms=   112, antis=2: [(3, 0), (23, 0)]
Flat( 25,  25) moves=  4468, ms=   108, antis=2: [(2, 0), (21, 22)]
Flat( 26,  26) moves=  4841, ms=   283, antis=2: [(11, 0), (0, 23)]
Flat( 27,  27) moves=  5433, ms=    61, antis=2: [(1, 0), (0, 10)]
Flat( 28,  28) moves=  6274, ms=   233, antis=2: [(3, 0), (27, 27)]
Flat( 29,  29) moves=  6450, ms=    63, antis=2: [(1, 0), (2, 2)]
Flat( 30,  30) moves=  7207, ms=   287, antis=2: [(3, 0), (4, 19)]
Flat( 31,  31) moves=  9441, ms=   271, antis=2: [(2, 0), (24, 28)]
Flat( 32,  32) moves=  7145, ms=   106, antis=2: [(1, 0), (2, 1)]
Flat( 33,  33) moves=  8256, ms=    12, antis=2: [(0, 0), (28, 0)]
Flat( 34,  34) moves=  8871, ms=   975, antis=2: [(9, 0), (30, 33)]
Flat( 35,  35) moves=  8643, ms=   111, antis=2: [(0, 0), (0, 32)]
Flat( 36,  36) moves=  9924, ms=   718, antis=2: [(6, 0), (33, 0)]
Flat( 37,  37) moves=  9216, ms=   218, antis=2: [(0, 0), (0, 32)]
Flat( 38,  38) moves= 11893, ms=   808, antis=2: [(4, 0), (3, 37)]
Flat( 39,  39) moves= 14882, ms=   112, antis=2: [(1, 0), (4, 0)]
Flat( 40,  40) moves= 12784, ms=   574, antis=2: [(3, 0), (0, 8)]
Flat( 41,  41) moves= 16824, ms=  4832, antis=2: [(32, 0), (32, 11)]
Flat( 42,  42) moves= 17468, ms=  1983, antis=2: [(11, 0), (0, 33)]
Flat( 43,  43) moves= 18038, ms=  1157, antis=2: [(4, 0), (0, 10)]
Flat( 44,  44) moves= 19382, ms=  1622, antis=2: [(8, 0), (1, 43)]
Flat( 45,  45) moves= 16325, ms=  6791, antis=2: [(37, 0), (0, 11)]
Flat( 46,  46) moves= 18941, ms=  1883, antis=2: [(9, 0), (0, 11)]
Flat( 47,  47) moves= 25372, ms=  1370, antis=2: [(6, 0), (0, 10)]
Flat( 48,  48) moves= 26220, ms=  1640, antis=2: [(6, 0), (0, 14)]
Flat( 49,  49) moves= 24875, ms=    17, antis=2: [(0, 0), (0, 3)]
Flat( 50,  50) moves= 24322, ms=  6055, antis=2: [(34, 0), (1, 48)]
Flat( 51,  51) moves= 24497, ms=  7100, antis=2: [(33, 0), (0, 22)]
Flat( 52,  52) moves= 27189, ms= 10857, antis=2: [(46, 0), (41, 3)]
Flat( 53,  53) moves= 20581, ms=  1174, antis=2: [(2, 0), (0, 16)]
Flat( 54,  54) moves= 30162, ms= 10591, antis=2: [(42, 0), (2, 29)]
Flat( 55,  55) moves= 27533, ms= 12260, antis=2: [(27, 0), (0, 37)]
Flat( 56,  56) moves= 33683, ms= 13835, antis=2: [(45, 0), (5, 49)]
Flat( 57,  57) moves= 25755, ms=  5061, antis=2: [(11, 0), (5, 49)]
Flat( 58,  58) moves= 41383, ms=  2567, antis=2: [(6, 0), (50, 57)]
Flat( 59,  59) moves= 24317, ms=  3362, antis=2: [(7, 0), (0, 33)]
Flat( 60,  60) moves= 32201, ms= 29058, antis=2: [(1, 1), (17, 37)]
Flat( 61,  61) moves= 44500, ms=  3656, antis=2: [(7, 0), (0, 30)]
Flat( 62,  62) moves= 48588, ms= 32416, antis=2: [(1, 1), (4, 52)]
Flat( 63,  63) moves= 26682, ms= 16991, antis=2: [(23, 0), (0, 32)]
Flat( 64,  64) moves= 46171, ms= 17398, antis=2: [(36, 0), (1, 7)]
Flat( 65,  65) moves= 62404, ms= 38504, antis=2: [(50, 0), (43, 60)]
Flat( 66,  66) moves= 57160, ms= 76256, antis=2: [(65, 1), (0, 34)]
Flat( 67,  67) moves= 49408, ms= 18641, antis=2: [(25, 0), (64, 59)]
Flat( 68,  68) moves= 55571, ms= 45085, antis=2: [(62, 0), (43, 18)]
Flat( 69,  69) moves= 56026, ms= 22413, antis=2: [(27, 0), (0, 55)]
Flat( 70,  70) moves= 36513, ms=  2613, antis=2: [(2, 0), (0, 39)]
Flat( 71,  71) moves= 48872, ms= 34007, antis=2: [(28, 0), (0, 20)]
Flat( 72,  72) moves= 63994, ms= 50881, antis=2: [(60, 0), (0, 30)]
Flat( 73,  73) moves= 58381, ms= 13073, antis=2: [(11, 0), (64, 71)]
Flat( 74,  74) moves= 42215, ms= 17366, antis=2: [(16, 0), (0, 31)]
Flat( 75,  75) moves= 75372, ms= 18374, antis=2: [(15, 0), (66, 71)]
Flat( 76,  76) moves= 56351, ms= 93283, antis=2: [(3, 1), (75, 4)]
Flat( 77,  77) moves= 46786, ms= 11970, antis=2: [(9, 0), (0, 38)]
Flat( 78,  78) moves= 62893, ms= 32158, antis=2: [(27, 0), (0, 37)]
Flat( 79,  79) moves= 67541, ms=177477, antis=2: [(23, 1), (0, 34)]
Flat( 80,  80) moves= 80879, ms=372160, antis=2: [(60, 3), (0, 44)]
Flat( 81,  81) moves= 88374, ms=147473, antis=2: [(78, 0), (21, 39)]
Flat( 82,  82) moves= 86374, ms= 77905, antis=2: [(61, 0), (0, 56)]
Flat( 83,  83) moves= 94889, ms=296498, antis=2: [(11, 2), (0, 64)]
Flat( 84,  84) moves= 94331, ms=172889, antis=2: [(11, 1), (69, 79)]
Flat( 85,  85) moves= 45979, ms=118225, antis=2: [(72, 0), (0, 22)]
Flat( 86,  86) moves=108540, ms=153260, antis=2: [(83, 0), (9, 20)]
Flat( 87,  87) moves= 86835, ms= 90311, antis=2: [(39, 0), (0, 41)]
Flat( 88,  88) moves=119793, ms=124539, antis=2: [(70, 0), (72, 59)]
Flat( 89,  89) moves=124968, ms=426816, antis=2: [(65, 1), (85, 80)]
Flat( 90,  90) moves=100611, ms= 79459, antis=2: [(38, 0), (0, 66)]
Flat( 91,  91) moves=102011, ms=164834, antis=2: [(73, 0), (58, 64)]
Flat( 92,  92) moves=126202, ms= 43023, antis=2: [(17, 0), (0, 68)]
Flat( 93,  93) moves=128752, ms=532998, antis=2: [(19, 2), (0, 53)]
Flat( 94,  94) moves=121705, ms=248588, antis=2: [(91, 0), (0, 48)]
Flat( 95,  95) moves= 67762, ms= 19089, antis=2: [(5, 0), (1, 71)]
Flat( 96,  96) moves=117749, ms=283078, antis=2: [(93, 0), (12, 9)]
Flat( 97,  97) moves= 96684, ms=216136, antis=2: [(63, 0), (0, 73)]
Flat( 98,  98) moves=121653, ms=296058, antis=2: [(91, 0), (92, 86)]
Flat( 99,  99) moves=148069, ms=382348, antis=2: [(1, 1), (4, 92)]
Flat(100, 100) moves=130279, ms=257610, antis=2: [(82, 0), (26, 35)]
Flat(101, 101) moves=139990, ms= 13062, antis=2: [(3, 0), (100, 95)]
Flat(102, 102) moves=129050, ms=305218, antis=2: [(88, 0), (0, 26)]
Flat(103, 103) moves=167440, ms=462568, antis=2: [(88, 0), (71, 75)]
Flat(104, 104) moves=169045, ms=276566, antis=2: [(80, 0), (0, 40)]
Flat(105, 105) moves=126847, ms=474630, antis=2: [(90, 0), (32, 65)]
Flat(106, 106) moves=112420, ms=948018, antis=2: [(7, 2), (10, 89)]
Flat(107, 107) moves=182988, ms=968832, antis=2: [(0, 2), (50, 2)]
Flat(108, 108) moves=192786, ms=945579, antis=2: [(96, 1), (15, 82)]
Flat(109, 109) moves=188408, ms=471291, antis=2: [(95, 0), (97, 2)]
Flat(110, 110) moves=190668, ms=475323, antis=2: [(95, 0), (71, 82)]
Flat(111, 111) moves=167119, ms=968849, antis=2: [(17, 1), (0, 37)]
Flat(112, 112) moves=201124, ms=449867, antis=2: [(91, 0), (19, 100)]
Flat(113, 113) moves=160310, ms=667044, antis=2: [(95, 0), (0, 58)]
Flat(114, 114) moves=212429, ms=601943, antis=2: [(101, 0), (0, 58)]
Flat(115, 115) moves=198192, ms=443720, antis=2: [(85, 0), (0, 46)]
Flat(116, 116) moves=198095, ms=601074, antis=2: [(98, 0), (104, 61)]
Flat(117, 117) moves=154901, ms=1130031, antis=2: [(31, 1), (0, 47)]
Flat(118, 118) moves=226976, ms=3578390, antis=2: [(23, 5), (105, 116)]
Flat(119, 119) moves=228885, ms=6956249, antis=2: [(0, 8), (0, 48)]
Flat(120, 120) moves=212478, ms=425858, antis=2: [(78, 0), (0, 65)]
Flat(121, 121) moves=224526, ms=973998, antis=2: [(103, 0), (108, 29)]
Flat(122, 122) moves=254354, ms=1846573, antis=2: [(1, 2), (0, 60)]
Flat(123, 123) moves=240734, ms=1008653, antis=2: [(117, 0), (0, 43)]
Flat(124, 124) moves=382904, ms=1211038, antis=2: [(10, 1), (0, 48)]
Flat(125, 125) moves=275606, ms=748222, antis=2: [(101, 0), (111, 74)]
Flat(126, 126) moves=245051, ms=4045597, antis=2: [(89, 4), (0, 65)]
Flat(127, 127) moves=273760, ms=1300793, antis=2: [(121, 0), (8, 119)]
Flat(128, 128) moves=276339, ms=1091705, antis=2: [(119, 0), (0, 60)]
Flat(129, 129) moves=283441, ms=7524807, antis=2: [(111, 5), (0, 83)]
Flat(130, 130) moves=248979, ms=822699, antis=2: [(105, 0), (20, 14)]
Flat(131, 131) moves=221516, ms=2223013, antis=2: [(116, 1), (0, 63)]
Flat(132, 132) moves=298919, ms=568561, antis=2: [(74, 0), (117, 0)]
Flat(133, 133) moves=334328, ms=3696876, antis=2: [(117, 2), (130, 91)]
Flat(134, 134) moves=226572, ms= 76569, antis=2: [(7, 0), (119, 128)]
Flat(135, 135) moves=291391, ms=2098988, antis=2: [(1, 1), (49, 102)]
Flat(136, 136) moves=294960, ms=1186020, antis=2: [(117, 0), (114, 88)]
Flat(137, 137) moves=340168, ms=1313430, antis=2: [(110, 0), (48, 96)]
Flat(138, 138) moves=368936, ms=2691298, antis=2: [(117, 1), (122, 2)]
---------- TopD on laptop below
TopD(139, 139) moves=234776, ms=801430, antis=2: [(0, 34), (0, 84)]
TopD(140, 140) moves=380743, ms= 79578, antis=2: [(0, 5), (62, 5)]
TopD(141, 141) moves=379432, ms=537106, antis=2: [(0, 23), (74, 23)]
TopD(142, 142) moves=361669, ms=459118, antis=2: [(0, 20), (73, 20)]
TopD(143, 143) moves=371472, ms=255443, antis=2: [(0, 11), (63, 11)]
TopD(144, 144) moves=259217, ms=1160455, antis=2: [(0, 41), (74, 30)]
TopD(145, 145) moves=408822, ms=270592, antis=2: [(0, 11), (73, 11)]
TopD(146, 146) moves=264033, ms=1231544, antis=2: [(0, 41), (72, 41)]
TopD(147, 147) moves=365408, ms=723144, antis=2: [(0, 24), (146, 4)]
TopD(148, 148) moves=380315, ms=1687619, antis=2: [(0, 52), (129, 100)]
TopD(140, 140) moves=380743, ms= 79578, antis=2: [(0, 5), (62, 5)]
TopD(141, 141) moves=379432, ms=537106, antis=2: [(0, 23), (74, 23)]
TopD(142, 142) moves=361669, ms=459118, antis=2: [(0, 20), (73, 20)]
TopD(143, 143) moves=371472, ms=255443, antis=2: [(0, 11), (63, 11)]
TopD(144, 144) moves=259217, ms=1160455, antis=2: [(0, 41), (74, 30)]
TopD(145, 145) moves=408822, ms=270592, antis=2: [(0, 11), (73, 11)]
TopD(146, 146) moves=264033, ms=1231544, antis=2: [(0, 41), (72, 41)]
TopD(147, 147) moves=365408, ms=723144, antis=2: [(0, 24), (146, 4)]
TopD(148, 148) moves=380315, ms=1687619, antis=2: [(0, 52), (129, 100)]
TopD(149, 149) moves=260579, ms=1318040, antis=2: [(0, 41), (31, 74)]
TopD(150, 150) moves=469262, ms=275418, antis=2: [(0, 11), (0, 85)]
TopD(151, 151) moves=462015, ms=103318, antis=2: [(0, 4), (0, 100)]
TopD(152, 152) moves=442896, ms=213963, antis=2: [(0, 8), (75, 8)]
TopD(153, 153) moves=660594, ms=1719598, antis=2: [(0, 47), (61, 10)]
TopD(154, 154) moves=340725, ms=1394039, antis=2: [(0, 38), (0, 87)]
TopD(155, 155) moves=441601, ms=779319, antis=2: [(0, 23), (0, 80)]
TopD(156, 156) moves=441456, ms=1109257, antis=2: [(0, 30), (0, 94)]
TopD(157, 157) moves=483987, ms=374837, antis=2: [(0, 11), (79, 11)]
TopD(158, 158) moves=295663, ms=2007712, antis=2: [(0, 47), (96, 155)]
TopD(159, 159) moves=502760, ms=217592, antis=2: [(0, 8), (0, 85)]
TopD(160, 160) moves=507814, ms=458416, antis=2: [(0, 14), (0, 69)]
TopD(161, 161) moves=428402, ms=1560860, antis=2: [(0, 37), (0, 72)]
TopD(162, 162) moves=525431, ms=367360, antis=2: [(0, 11), (0, 93)]
TopD(163, 163) moves=546664, ms=275212, antis=2: [(0, 8), (71, 8)]
TopD(164, 164) moves=542949, ms=245340, antis=2: [(0, 8), (0, 86)]
TopD(165, 165) moves=566448, ms=254747, antis=2: [(0, 8), (0, 75)]
TopD(166, 166) moves=364749, ms=2561147, antis=2: [(0, 49), (0, 84)]
TopD(167, 167) moves=597842, ms=1311401, antis=2: [(0, 29), (0, 65)]
TopD(168, 168) moves=367820, ms=2975430, antis=2: [(0, 53), (127, 89)]
TopD(169, 169) moves=591161, ms=1431526, antis=2: [(0, 29), (0, 79)]



[TopD(355, 355) ms=366,505,420, antis=2: [(  7, 329), ( 80, 298)]]

     */

    /*
    antibot-stats

     side  col0=2  col0=1  col0=0  rowM=2  rowM=1  rowM=0     all      ms
      20       2      87      62       2      27     122     151     312
      21       5     149      93       4      28     215     247     281
      22      10      59      68       1      28     108     137     295
      23      10      95     138       3      45     195     243     548
      24       7      58      65       4      41      85     130     401
      25      10     112     131       1      32     220     253     659
      26       9      76      68       0      28     125     153     552
      27      11     120      85       0      23     193     216     776
      28       9      67      68       0      20     124     144     980
      29      11     137      93       1      22     218     241   1,150
      30       7      43      46       1      19      76      96   1,292
      31       8      92     105       0      36     169     205   2,505
      32       4      46      45       2      15      78      95   1,881
      33      11     104      87       2      36     164     202   3,037
      34       7      40      41       1      17      70      88   2,720
      35      13      98      82       1      19     173     193   3,447
      36       5      58      56       2      17     100     119   3,942
      37      12      93      45       1       8     141     150   4,721
      38       3      31      41       1      15      59      75   5,020
      39       6     109      87       0      33     169     202  10,083
      40       0      59      34       0       7      86      93   7,444
      41       8     109      81       0      11     187     198  11,144
      42       8      31      39       2      16      60      78   9,274
      43      11     105      39       0      20     135     155  11,366
      44       6      41      34       0       8      73      81  12,968
      45      10      76      35       1      10     110     121  15,235
      46       5      58      22       0       8      77      85  15,655
      47       5      76      49       0      11     119     130  28,409
      48       6      27      29       1      10      51      62  19,919
      49       6     108      44       0       7     151     158  30,770
      50       6      51      20       0       8      69      77  24,909

    side  col0=2  col0=1  rowM=2  rowM=1      tl      tr      br      bl     non   break     all  col0=0  rowM=0        ms
      40       0      59       0       6       4       4       3       3      42       2      93      34      28     6,636
      41       8     109       0       6       2       5       7       2     134       4     198      81      75    10,407
      42       8      31       0      12       1       3       1       0      49       1      78      39      27     8,426
      43      11     105       0      12       1       5       3       1      44       3     155      39      27    10,839
      44       6      41       0       8       1       4       3       1      43       1      81      34      26    12,066
      45      10      76       1       8       3       4       3       0      42       2     121      35      26    13,935
      46       5      58       0       4       2       2       2       0      30       1      85      22      18    14,680
      47       5      76       0       6       0       4      15       1      66       2     130      49      43    27,629
      48       6      27       0       9       1       5       3       0      31       1      62      29      20    19,109
      49       6     108       0       4       3       2       1       4      70       3     158      44      40    28,709
      50       6      51       0       3       2       3       0       1      28       1      77      20      17    23,621
      51       7      74       0       5       0       1       3       0      54       2     115      34      29    29,155
      52       5      47       0       2       0       1       2       2      37       1      75      23      21    31,373
      53       7      85       0       7       2       4       5       2      37       2     124      32      25    36,058
      54       3      39       0      10       1       3       0       2      46       1      78      36      26    37,611
      55       5      92       0       9       1       4       4       6      39       2     133      36      27    72,333
      56       4      27       0       3       1       1       2       0      32       0      52      21      18    46,750
      57       8      83       1       4       2       2       2       1      31       2     115      24      19    69,974
      58       8      29       0       4       0       2       1       2      45       1      66      29      25    56,004
      59       6      93       0       5       3       1       3       1      60       2     138      39      34    68,478
      60       4      38       0       5       2       0       0      16      46       1      79      37      32    73,126
      61       6      76       0       2       1       1       1       1      46       1     109      27      25    82,151
      62       2      24       0       5       2       0       0       0      46       0      55      29      24    84,786
      63       3      80       0       3       0       2       0       2      40       1     108      25      22   153,989
      64       2      21       0       5       0       1       5       0      34       0      48      25      20   103,332


*/
}
