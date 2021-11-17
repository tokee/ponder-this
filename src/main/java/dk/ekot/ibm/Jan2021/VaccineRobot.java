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
import java.util.concurrent.atomic.AtomicLong;
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

    private static String impl = System.getenv().getOrDefault("IMPL", "v2");
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
            "  IMPL=v1|v2        Use implementation v1 or v2 (v2 is default)\n" +
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
            long firstAntiIndex = getInt(args, 4, 0);
            long lastAntiIndex = getInt(args, 5, -1);
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
        System.out.printf(Locale.ENGLISH, "threads=%d, minSide=%d, maxSide=%d, impl=%s%n",
                          threads, minSide, maxSide, impl);
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


        //threaded(1, 10, 10, 2);
        threaded(6, 106, 106, 2);


        //        showMatches(30, 2, -1, 30);

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
        threaded(threads, minSide, maxSide, maxAntis, Collections.singletonList(getSystematic()));
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

    private static void deepDive(int side, int threads, int antis, long firstAntiIndex, long lastAntiIndex) {
        deepDive(side, threads, antis, firstAntiIndex, lastAntiIndex, getSystematic());
    }

    private static Systematic getSystematic() {
        switch (impl) {
            case "v1" : return VaccineRobot::systematicFlatTD;
            case "v2" : return VaccineRobot::systematicFlatWalk;
            default: throw new IllegalArgumentException("IMPL==" + impl + " is unsupported");
        }
    }

    private static void deepDive(int side, int threads, int antis, long firstAnti0Index, long lastAnti0Index, Systematic strategy) {
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
        List<Match> process(int width, int height, int antiCount, int maxMatches, ExecutorService executor, int threads, long... hints);
    }

    // hints = [startAnti0Index, [endAnti0Index]
    private static List<Match> systematicFlatTD(int width, int height, int antiCount, int maxMatches, ExecutorService executor, int threads, long... hints) {
        return hints.length == 0 ?
                systematicFlatTDMulti(width, height, antiCount, maxMatches, executor, threads, true) :
                systematicFlatTDLimited(width, height, antiCount, maxMatches, executor, threads, hints[0], hints.length > 1 ? hints[1] : width*height-1);
    }
    // hints = [startAnti0Index, [endAnti0Index]
    private static List<Match> systematicFlatWalk(int width, int height, int antiCount, int maxMatches, ExecutorService executor, int threads, long... hints) {
        List<Integer> startYs = Collections.singletonList(0);

        WalkPlanner.Walk walk = WalkPlanner.getFullSegmentedWalk(width, height, antiCount);
        //WalkPlanner.Walk walk = WalkPlanner.getLRTDWalk(width, height, antiCount);

        return getMatchesWalk(width, height, antiCount, maxMatches, startYs, walk,
                            executor, true, threads,
                            hints.length == 0 ? 0 : hints[0], hints.length < 2 ? -1 : hints[1]);
    }

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
            long firstAnti0Index, long lastAnti0Index) {
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
                                            long firstAnti0Index, long lastAnti0Index) {
        final List<Match> matches = Collections.synchronizedList(new ArrayList<>());
        AtomicLong zeroBotIndex = new AtomicLong(firstAnti0Index);
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

    /**
     *
     * @param width           grid width.
     * @param height          grid height.
     * @param antiCount       the number of anitbots.
     * @param maxMatches      the maximum number of matches to find before returning.
     * @param startYs         startingY positions, only used for feedback.
     * @param walk            the walk to follow.
     * @param executor        the executor for the threads.
     * @param printAnti0      if true and verbose==true, detailed debug is written for each antibot0 position
     * @param threads         the number of threads to use.
     * @param firstAnti0Index start index in walks.
     * @param lastAnti0Index  end index in walks.
     * @return the matches from the run.
     */
    private static List<Match> getMatchesWalk(int width, int height, int antiCount, int maxMatches,
                                            List<Integer> startYs, WalkPlanner.Walk walk,
                                            ExecutorService executor, boolean printAnti0, int threads,
                                              long firstAnti0Index, long lastAnti0Index) {
        final List<Match> matches = Collections.synchronizedList(new ArrayList<>());
        AtomicLong zeroBotIndex = new AtomicLong(firstAnti0Index);
        AtomicBoolean continueWalk = new AtomicBoolean(true);
        WalkPlanner.Walk finalWalk = walk.getSubWalk(firstAnti0Index, lastAnti0Index);
        ViabilityChecker viabilityChecker = new ViabilityChecker(width, height);
        final boolean[] hasBeenShown = new boolean[width*height];

        if (verbose) {
            System.err.printf(
                    Locale.ENGLISH, "\ngrid(%d, %d /%d), anti0Index=%d..%d: ",
                    width, height, antiCount, firstAnti0Index,
                    lastAnti0Index == -1 ? WalkPlanner.combinations(width, height, antiCount) : lastAnti0Index);
        }

        List<Future<List<Match>>> jobs = new ArrayList<>(threads);
        for (int thread = 0 ; thread < threads ; thread++) {
            jobs.add(executor.submit(() -> {
                FlatGrid grid = new FlatGrid(width, height, "TopD", startYs);

                systematicFlatWalk(grid, viabilityChecker, finalWalk, hasBeenShown,
                                       zeroBotIndex, continueWalk, printAnti0,
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
            System.err.println(viabilityChecker);
        }
        return matches;
    }

    private static boolean systematicFlatTDSingle(
            FlatGrid grid, FlatGrid empty, final int[] antis, int antiIndex,
            int[][] walks, AtomicLong walkOrigo, boolean skipCol0Pairs, long lastAnti0Index,
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
        long walkIndex;

        while (continueWalk.get() && (walkIndex = walkOrigo.getAndIncrement()) < walk.length) {
            if (antiIndex == 0 && walkIndex > lastAnti0Index) {
                break;
            }
            if (verbose && antiIndex == 0) {
                System.err.print(printAnti0 ? "<" + walkIndex + Pos.fromInt(walk[(int) walkIndex], grid.width, grid.height).toString().replace(" ", "") + ">" :".");
            }
            antis[antiIndex] = walk[(int) walkIndex];
            if (antiIndex < antis.length - 1) { // More antis to go
                if (!systematicFlatTDSingle(
                        grid, empty, antis, antiIndex + 1,
                        walks, new AtomicLong(walkIndex+1), skipCol0Pairs, lastAnti0Index,
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
            if (antisInBottomRow(grid, antis)) continue;

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

    private static boolean systematicFlatWalk(
            FlatGrid grid, ViabilityChecker viabilityChecker,
            WalkPlanner.Walk walk, boolean[] hasBeenShown, AtomicLong walkOrigo,
            AtomicBoolean continueWalk, boolean printAnti0,
            int threads, Predicate<Match> collect) {

        int[] antis = null;
        while (continueWalk.get() && (antis = walk.next(antis)) != null) {
            if (verbose && !hasBeenShown[antis[0]]) {
                hasBeenShown[antis[0]] = true;
                System.err.print(printAnti0 ? "<" + walk.getPos() + Pos.fromInt(antis[0], grid.width, grid.height).toString().replace(" ", "") + ">" : ".");
            }
            if (!viabilityChecker.isViable(antis)) {
                continue;
            }

            // Check for bots in column 0
            if (antisInColumn0(grid, antis)) {
                continue;
            }

            // Check for bots in bottom row
            if (antisInBottomRow(grid, antis)) {
                continue;
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

    private static boolean antisInBottomRow(FlatGrid grid, int[] antis) {
        if (minRM != null || maxRM != null) {
            int rmCount = 0;
            int bottomRowOrigo = (grid.height - 1) * grid.width;
            for (int anti : antis) {
                if (anti >= bottomRowOrigo) {
                    rmCount++;
                }
            }
            if ((minRM != null && rmCount < minRM) ||
                (maxRM != null && rmCount > maxRM)) {
                return true;
            }
        }
        return false;
    }

    private static boolean antisInColumn0(FlatGrid grid, int[] antis) {
        if (minC0 != null || maxC0 != null) {
            int col0Count = 0;
            for (int anti : antis) {
                if (anti % grid.width == 0) { // Column 0
                    col0Count++;
                }
            }
            if ((col0Count == antis.length) ||
                (minC0 != null && col0Count < minC0) ||
                (maxC0 != null && col0Count > maxC0)) {
                return true;
            }
        }
        return false;
    }

    /*



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
