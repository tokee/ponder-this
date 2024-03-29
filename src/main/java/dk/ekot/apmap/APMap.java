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
package dk.ekot.apmap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Al Zi*scramble*mmermann's Progra*scramble*mming Contes*scramble*ts
 * http://azs*scramble*pcs.com/Contest/AP*scramble*Math
 */
public class APMap {
    private static final Logger log = LoggerFactory.getLogger(APMap.class);

    public enum SHUFFLE_IMPL {s7, s8, s9;

        public static SHUFFLE_IMPL getDefault() {
            return s9;
        }
    };

    public static final int[] EDGES = new int[]{
            2, 6, 11, 18, 27, 38, 50, 65, 81, 98, 118, 139, 162, 187, 214, 242, 273, 305, 338, 374, 411, 450, 491, 534, 578};

    public static final int[][] BESTS = new int[][]{
            // edge, local, global,
            {2, 6, 6}, {6, 33, 33}, {11, 80, 80}, {18, 151, 153}, {27, 253, 266}, {38, 415, 420},
            {50, 548, 621}, {65, 772, 884}, {81, 946, 1193}, {98, 1258, 1512},
            {118, 1716, 1973}, {139, 2001, 2418}, {162, 2274, 2921}, {187, 3072, 3518},
            {214, 3222, 4284}, {242, 3599, 5057}, {273, 4190, 5831},
            {305, 4998, 6753}, {338, 6018, 7783}, {374, 6378, 8962},
            {411, 6962, 10062}, {450, 7938, 11152}, {491, 8851, 12610},
            {534, 10980, 14108}, {578, 12331, 15643}};

    public static final int[] IMPROVABLE = new int[]{
            18, 27, 38, 50, 65, 81, 98, 118, 139, 162, 187, 214, 242, 273, 305, 338, 374, 411, 450, 491, 534, 578};

    public static final int[] FORCED_CORNERS = new int[]{65, 187, 214, 534, 578};

    // java -cp target/ponder-this-0.1-SNAPSHOT-jar-with-dependencies.jar dk.ekot.apmap.APMap

    public static void adHoc(String args[]) {
        boolean SHOW_BEST = true;
        int STALE_MS = 30*1000;
        int RUN[] = new int[]{11};
        //int RUN[] = IMPROVABLE;
        //int RUN[] = EDGES;
        //Arrays.stream(EDGES).parallel().forEach(APMap::saveImage); if (1==1) return;
        //Arrays.stream(RUN).parallel().forEach(APMap::saveImage); if (1==1) return;
        //APMap.saveImage(65); if (1==1) return;
        //System.out.println(map); if (1==1) return;
        
        //Arrays.stream(RUN).boxed().parallel().forEach(APMap::doShuffle); if (1 == 1) return;
        //dumpRelativePosition(); if (1==1) return;
        // 6c: 365/372/360/369/367
        // cb6c: 377/375/370/372/371  375/372/377
        buildThenShuffle(18, SHOW_BEST, STALE_MS, PriorityAdjuster.FILLER.centerBadSixCorners, SHUFFLE_IMPL.s7, 5000, 2000, 10, -5); if (1 == 1) return;

        testMultipleEdges(RUN, SHOW_BEST, STALE_MS, PriorityAdjuster.FILLER.neutral, true); if (1 == 1) return;

        Arrays.stream(RUN).boxed().parallel()
                .map(APMap::loadJSON)
                .forEach(json -> shuffleFromJSON(json, 500, 100, 2, 0, SHUFFLE_IMPL.s7, PriorityAdjuster.FILLER.sixCorners)); if (1 == 1) return;
                //.forEach(json -> shuffleFromJSON(json, 2, 10000, 2, 0, SHUFFLE_IMPL.s9)); if (1 == 1) return;
//        Arrays.stream(RUN).boxed().parallel().forEach(APMap::doShuffle); if (1 == 1) return;

        // testMarking();
        //if (1==1) return;

        //goFlat(6); if (1 == 1) return;

        //System.out.println(new Mapper(3)); if (1==1) return;
        //        new APMap().go(6, 10000000);

        // 118 + 139 responds well to pre-filled priority, 162 + 187 does not



    }

    private static void buildThenShuffle(int edge, boolean showBest, int staleMS, PriorityAdjuster.FILLER filler,
                                         SHUFFLE_IMPL shuffleImpl, int runs, int maxPermutations, int minIndirects, int minGained) {
        int seed = new Random().nextInt();
        Mapper board = new APMap().goQuadratic(edge, staleMS, showBest, true, filler);
        doShuffle(board, runs, seed, maxPermutations, minIndirects, minGained, shuffleImpl);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            long startTime = System.currentTimeMillis();
            adHoc(args);
            System.out.println("adHoc finished in " + (System.currentTimeMillis()-startTime) + "ms");
            return;
        }

        if (args[0].startsWith("s")) {
            shuffle(args, SHUFFLE_IMPL.valueOf(args[0]));
            return;
        }

        if (args.length == 1) {
            System.out.println("Usage: MapperTest [nochanges timeout in seconds] edge*");
            System.out.println("Competition edges: " + Arrays.toString(EDGES));
        }

        boolean SHOW_BEST = true;
        int[] edges = new int[args.length-1];
        for (int i = 1 ; i < args.length ; i++) {
            edges[i-1] = Integer.parseInt(args[i]);
        }

        int STALE_MS = Integer.parseInt(args[0])*1000;

        testMultipleEdges(edges, SHOW_BEST, STALE_MS, PriorityAdjuster.FILLER.getDefault(), false);
    }

    private static void dumpRelativePosition() {
        List<String> sb = new ArrayList<>();
        for (int[] b : BESTS) {
            System.out.printf("edge=%3d, personal=%5d, global=%5d, fraction=%.2f\n",
                              b[0], b[1], b[2], 1.0*b[1]/b[2]);
            sb.add(String.format(Locale.ROOT, "%.2f:edge=%3d, personal=%5d, global=%5d, fraction=%.2f",
                                 1.0*b[1]/b[2], b[0], b[1], b[2], 1.0*b[1]/b[2]));
        }
        Collections.sort(sb);
        System.out.println("----");
        sb.stream().map(s -> s.split("[:]")[1]).forEach(System.out::println);
    }

    // Arguments: <impl> <runs> <permutations> <minIndirect> <minGained> <edge*>
    public static void shuffle(String[] args, SHUFFLE_IMPL impl) {
        if (args.length < 6) {
            System.out.println("shuffle: \"" + impl + "\" <runs> <permutations> <minIndirect> <minGained> <edge*>");
            System.out.println("Valid edges: " + Arrays.toString(EDGES));
            return;
        }
        int index = 1;
        int runs = Integer.parseInt(args[index++]);
        int permutations = Integer.parseInt(args[index++]);
        int minIndirects = Integer.parseInt(args[index++]);
        int minGained = Integer.parseInt(args[index++]);

        int[] edges = new int[args.length-index];
        for (int i = index ; i < args.length ; i++) {
            edges[i-index] = Integer.parseInt(args[i].replace(",", ""));
        }

        System.out.printf(Locale.ROOT, "shuffle %s: runs=%d, permutations=%d, minIndirects=%d, minGained=%d, edges=%s\n",
                          impl, runs, permutations, minIndirects, minGained, Arrays.toString(edges));
        Arrays.stream(edges).boxed().parallel()
                .map(APMap::loadJSON)
                .forEach(json -> shuffleFromJSON(json, runs, permutations, minIndirects, minGained, impl, PriorityAdjuster.FILLER.sixCorners));
    }

    private static void saveImage(int edge) {
        Mapper map = new Mapper(edge);
        String json = loadJSON(edge);
        map.addJSONMarkers(json);
        try {
            map.saveToImage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void doShuffle(int edge) {
        final long startTime = System.currentTimeMillis();
        System.out.println("Performing initial cheap run for edge=" + edge);
        final int seed = new Random().nextInt();
        final int MAX_PERMUTATIONS = 40;
        int RUNS = 1000000;

        // TODO: Fails with edge==50!?
        // TODO: 18 seems to work well (134), 27 (221), 38 (fail),
        Mapper board = new APMap().goQuadratic(edge, 10_000, true, true, PriorityAdjuster.FILLER.getDefault());
        System.out.println("Cheap run finished after " + (System.currentTimeMillis()-startTime)/1000 + " seconds");
        doShuffle(board, RUNS, seed, MAX_PERMUTATIONS);
    }

    public static String loadJSON(int edge) {
        File PERSISTENCE_A = new File("/home/te/projects/ponder-this/src/main/java/dk/ekot/apmap/results_te.txt");
        File PERSISTENCE_B = new File("results_te.txt");
        File PERSISTENCE = PERSISTENCE_A.canRead() ? PERSISTENCE_A : PERSISTENCE_B;

        if (!PERSISTENCE.canRead()) {
            throw new RuntimeException(new FileNotFoundException("Unable to read '" + PERSISTENCE + "'"));
        }
        try (InputStream is = new FileInputStream(PERSISTENCE);
             InputStreamReader ir = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(ir)) {
            String lastValid = null;
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = EDGE_LINE.matcher(line);
                if (m. find() && m.group(1).equals(Integer.toString(edge))) {
                    lastValid = line;
                }
            }
            if (lastValid == null) {
                throw new IllegalArgumentException(
                        "Unable to locate any line starting with 'edge=" + edge + "' in '" + PERSISTENCE + "'");
            }
            return lastValid;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static final Pattern EDGE_LINE = Pattern.compile("^edge=([0-9]*)");

    private static void shuffleFromJSON(String json) {
        final int MAX_PERMUTATIONS = 20;
        int RUNS = 100;
        shuffleFromJSON(json, RUNS, MAX_PERMUTATIONS);
    }
    private static void shuffleFromJSON(String json, int runs, int maxPermutations) {
        shuffleFromJSON(json, runs, maxPermutations, 3, -10,
                        SHUFFLE_IMPL.getDefault(), PriorityAdjuster.FILLER.getDefault());
    }
    private static void shuffleFromJSON(
            String json, int runs, int maxPermutations, int minIndirects, int minGained,
            SHUFFLE_IMPL impl, PriorityAdjuster.FILLER priorityFiller) {
        final long startTime = System.currentTimeMillis();
        final int seed = new Random().nextInt();

        int edge = Mapper.getEdgeFromJSON(json);

        Mapper board = new Mapper(edge);
        PriorityAdjuster.adjustPriorities(board, priorityFiller);
        board.addJSONMarkers(json);

        System.out.printf(Locale.ROOT, "edge=%d loaded and prioritized  with marked=%d in %d seconds\n",
                          board.edge, board.marked, (System.currentTimeMillis()-startTime)/1000);
        doShuffle(board, runs, seed, maxPermutations, minIndirects, minGained, impl);
    }

    private static void doShuffle(Mapper board, int runs, int seed, int maxPermutations) {
        doShuffle(board, runs, seed, maxPermutations, 3, -10, SHUFFLE_IMPL.getDefault());
    }
    private static void doShuffle(
            Mapper board, int runs, int seed, int maxPermutations, int minIndirects, int minGained,
            SHUFFLE_IMPL impl) {
        final long startTime = System.currentTimeMillis();
        Random random = new Random(seed);
        final int initial = board.marked;
        if (board.edge <= 50) {
            System.out.println(board);
            System.out.println("--- " + +board.marked);
        }

        System.out.printf(Locale.ROOT, "Initiating shuffle for edge=%d, seed=%d, maxPermutations=%d, minIndirects=%d, " +
                                       "minGained=%d, impl=%s\n",
                          board.edge, seed, maxPermutations, minIndirects, minGained, impl);
        int best = board.marked;
        int worst = board.marked;
        Mapper bestBoard = board;
        // TODO: Add termination on cycles by keeping a Set of previous toJSONs
        for (int run = 0; run < runs; run++) {
            seed = random.nextInt();
            //int gained = board.shuffle2(seed, maxPermutations);
            //int gained = board.shuffle5(seed, Math.max(2, board.edge/2), 500, maxPermutations, -1);
            int gained;
            switch (impl) {
                case s7:
                    gained = board.shuffle7(seed, minIndirects, maxPermutations, minGained);
                    break;
                case s8:
                    gained = board.shuffle8(seed, minIndirects, maxPermutations, minGained);
                    break;
                case s9:
                    gained = board.shuffle9(seed, minIndirects, maxPermutations, minGained);
                    break;
                default: throw new UnsupportedOperationException(
                        "Shuffle implementation '" + impl + "' not supported yet");
            }
            if (gained == 0 || board.marked <= best) {
                System.out.printf(Locale.ROOT, "edge=%3d, run=%3d/%d, marks:[current=%d, initial=%d, best=%d] (%2d gained)\n",
                                  board.edge, run+1, runs, board.getMarkedCount(), initial, best, gained);
            } else {
                System.out.printf(Locale.ROOT, "edge=%d, seed=%d, perms=%d, run=%d/%d gained %d with total %d/%d%s: %s\n",
                                  board.edge, seed, maxPermutations, run + 1, runs, gained, board.marked, initial,
                                  best < board.marked ? " (IMPROVEMENT)" : "", board.toJSON());
                if (board.edge <= 5) {
                    System.out.println(board);
                }
            }
            if (board.marked > best) {
                best = board.marked;
                bestBoard = board.copy(true);
            }
            worst = Math.min(worst, board.marked);
        }
        System.out.println("=======================================");
        if (board.edge <= 50) {
            System.out.println(bestBoard);
            System.out.println("--- " + bestBoard.marked);
        }
        System.out.printf(Locale.ROOT, "edge=%d, %s" +
                                       "shuffle %s (seed=%d, perms=%d): " +
                                       "worst=%d, initial=%d, best=%d, oldBest=%d, " +
                                       "time=%ss: %s\n",
                          board.edge, getPersonalbest(board.edge) < best ? "IMPROVEMENT " : "",
                          impl, seed, maxPermutations,
                          worst, initial, best, getPersonalbest(board.edge),
                          (System.currentTimeMillis() - startTime) / 1000, bestBoard.toJSON());
    }

    public static int getPersonalbest(int edge) {
        for (int[] vals: BESTS) {
            if (vals[0] == edge) {
                return vals[1];
            }
        }
        return -1;
    }

    private static void testMultipleEdges(int[] edges, boolean showBest, int staleMS, PriorityAdjuster.FILLER filler, boolean returnOnFirstBottom) {
        long startTime = System.currentTimeMillis();

        System.out.printf(Locale.ROOT, "Testing with staleMS=%d: %s", staleMS, Arrays.toString(edges));

        List<Mapper> results = Arrays.stream(edges).parallel().
                boxed().
                map(task -> new APMap().goQuadratic(task, staleMS, showBest, returnOnFirstBottom, filler)).
                collect(Collectors.toList());

        System.out.println("\nAll results:");
        results.forEach(b -> System.out.println(b.getStatus()));

        System.out.println("\nImprovements:");
        Map<Integer, Integer> localBest = Arrays.stream(BESTS).
                map(b -> Arrays.copyOf(b, 2)).
                collect(Collectors.toMap(b -> b[0], b -> b[1]));
        results.stream().
                filter(board -> localBest.containsKey(board.edge)).
                filter(board -> board.marked > localBest.get(board.edge)).
                forEach(b -> System.out.println(b.getStatus()));


        System.out.println("Total time: " + (System.currentTimeMillis()-startTime)/1000 + "s");
    }

    private static void doFlatWalk(int edge) {
        FlatBoard board = new FlatBoard(edge);
        FlatWalker walker = new FlatWalker(board);
        walker.walk(Integer.MAX_VALUE);
        Mapper best = walker.getBestMapper();
        System.out.println(best);
        System.out.printf("edge=%d, marks=%d: %s\n", edge, best.getMarkedCount(), best.toJSON());
    }

    public static void processRemaining(int maxStaleMS) {
        long startTime = System.currentTimeMillis();

        List<Integer> tests = Arrays.stream(BESTS). // edge, local, global
                filter(b -> b[1] < b[2]). // local worse than global
                map(b -> b[0]) // Only edges
                .collect(Collectors.toList());
        log.info("Processing {}/{} edges with maxStaleMS=={}: {}", tests.size(), BESTS.length, maxStaleMS, tests);

        List<Mapper> results = new ArrayList<>(tests.size());
        ForkJoinPool customThreadPool = new ForkJoinPool(3); // Seems to fit in default JVM heap allocation
        customThreadPool.submit(() -> {
            results.addAll(tests.parallelStream().
                                   map(task -> new APMap().goQuadratic(task, maxStaleMS, true, false, PriorityAdjuster.FILLER.getDefault())).
                                   collect(Collectors.toList()));
        }).join();
        
        System.out.println();
//        results.forEach(s -> System.out.println(s + "\n\n"));
        System.out.println("All results:");
        results.forEach(b -> System.out.println(b.getStatus()));

        System.out.println("Improvements:");
        Map<Integer, Integer> localBest = Arrays.stream(BESTS).
                map(b -> Arrays.copyOf(b, 2)).
                collect(Collectors.toMap(b -> b[0], b -> b[1]));
        results.stream().
                filter(board -> board.marked > localBest.get(board.edge)).
                forEach(b -> System.out.println(b.getStatus()));

        System.out.println("Total time: " + (System.currentTimeMillis()-startTime)/1000 + "s");
    }

    private static void testMarking() {
        Mapper board = new Mapper(3);
        Mapper.PriorityPosXY pos = new Mapper.PriorityPosXY(2, 0, 0);
        System.out.println("***********************************");
        System.out.println(board + " setting " + pos);
        board.markAndDeltaExpand(pos, true);
        System.out.println("-----------------------------------");
        System.out.println(board + " after marking");
        board.rollback(true);
        System.out.println("-----------------------------------");
        System.out.println(board + " after rollback");
        board.markAndDeltaExpand(pos, true);
        System.out.println("***********************************");
    }

/*    private Mapper goQuadratic(int edge) {
        return goQuadratic(edge, Integer.MAX_VALUE, true, Integer.MAX_VALUE, false);
    }
    private Mapper goQuadratic(int edge, boolean showBest, boolean returnOnFirstBottom) {
        return goQuadratic(edge, Integer.MAX_VALUE, showBest, Integer.MAX_VALUE, returnOnFirstBottom);
    }*/
    private Mapper goQuadratic(int edge, int maxStaleMS, boolean showBest, boolean returnOnFirstBottom, PriorityAdjuster.FILLER filler) {
        return goQuadratic(edge, maxStaleMS, showBest, Integer.MAX_VALUE, returnOnFirstBottom, filler);
    }
    private Mapper goQuadratic(int edge, int maxStaleMS, boolean showBest, int showBoardIntervalMS, boolean returnOnFirstBottom, PriorityAdjuster.FILLER filler) {
        log.info("Initializing for edge " + edge + "...");
        long initTime = -System.currentTimeMillis();
        Mapper board = new Mapper(edge);
        //System.out.println("Board stats: " + board.getStats());
        MapWalker walker = new MapWalker(board);
        initTime += System.currentTimeMillis();

        log.info("Walking for edge " + edge + "...");
        long walkTime = -System.currentTimeMillis();
        //walker.walkFlexible(maxStaleMS, showBest, showBoardIntervalMS, Comparator.comparing(Mapper.LazyPos::getPriorityChanges).thenComparing(Mapper.LazyPos::getPos),true);
        //walker.walkFlexible(maxStaleMS, showBest, showBoardIntervalMS, Mapper.getPriorityChangesComparator(), true);
        walker.walkFlexible(maxStaleMS, showBest, showBoardIntervalMS, true, returnOnFirstBottom, filler);
        //walker.walkStraight(maxStaleMS, showBest);
        walkTime += System.currentTimeMillis();

        System.out.printf(Locale.ROOT, "edge=%d, marks=%d/%d, initTime=%ds, walkTime=%ds, completed=%b: %s\n",
                          edge, walker.getBestBoard().getMarkedCount(), board.valids,
                          initTime/1000, walkTime/1000, walker.getBestBoard().isCompleted(),
                          walker.getBestBoard().toJSON());
        return walker.getBestBoard();
    }

    // -----------------------------------------------------------------------------------------------------------------

    private static void goFlat(int edge) {
        goFlat(edge, Long.MAX_VALUE);
    }
    private static void goFlat(int edge, long maxFulls) {
        log.info("Initializing...");
        long initTime = -System.currentTimeMillis();
        FlatBoard board = new FlatBoard(edge);
        System.out.println("Board stats: " + board.getStats());
        FlatWalker walker = new FlatWalker(board);
        initTime += System.currentTimeMillis();

        log.info("Walking...");
        long walkTime = -System.currentTimeMillis();
        walker.walk(maxFulls);
        walkTime += System.currentTimeMillis();

        System.out.printf(Locale.ROOT, "%s\nedge=%d, marks=%d/%d, initTime=%ds, walkTime=%ds\n%s\n",
                          walker.getBestMapper(), edge, walker.getBestMarkers(), board.flatLength(),
                          initTime/1000, walkTime/1000, walker.getBestMapper().toJSON());
    }

    /*
    Initial idea 20211117:

    Represent the hexagonal grid as a single array of bytes (or ints if that is faster on the concrete architecture).
    Entries can one of [neutral, marker, illegal]. The board changes all the time, so efficient state change with switch
    to the previous state is needed. Also keep track of the number of markers as well as the number of neutral (for
    maximum possible score from current state).

    For each entry there is a static array of arrays of positions that lines up with the entry: There can only be 1
    marker set in each of the inner arrays. Consequently, when a point is set in one of the inner arrays, all the other
    positions on the board must be set to illegal.

    -----------------
    Second idea 20211117:

    Instead of creating copies of the board, just keep track of the changes: The single marker and the list of illegals.
    With single threading, rollback is deterministic as long as marking an already illegal field as illegal is ignored.

    -----------------
    Idea #3 20211118:

    There is mirroring symmetry, so if column 0 of an edge 5 has been tested with
    X - - - -
    X X - - -
    X - X - -
    X - - X -
    X - - - X
    - X - - -
    - X X - -
    - X - X -
    - - X - -
    then there are no need to test more column 0 permutations.

    -----------------
    Idea #4 20211118:

    There is rotational symmetry, so for any full-depth check for a column 0 permutation, none of the other edges needs
    to be checked with that permutation.

    Easy enough to check if there are only 1 marker (just set an invalid at the 5 other edges), but more tricky with
    2 markers.

    Addendum: See idea #16 for an expansion on this.

    -----------------
    Idea #5 20211118:

    Assume that there will always be at least 1 marker at an edge (corollary with idea #4: In column 0), so skip
    searching when all permutations (see idea 3) has been tested there

    -----------------
    Idea #6 20211118:

    Change tracking does not need an object, only the list of changed elements, so use a shared pointer with offset.

    -----------------
    Idea #7 20211118:

    When a position has been tried, all positions before it (and itself) can be considered invalid for subsequent
    markers. Can this be used to set more invalids on subsequent positions?

    -----------------
    Idea #8 20211118:

    More instrumentation (track number of full depth searches)

    -----------------
    Idea #9 20211118:

    The arrays of illegalTriples are far too large (blows memory around edge=60).

    Alternatively provide fast coordinate translation between flat and quadratic so that only the deltas needs to be
    stored. The deltas can be shared between all origos. This means wasteful checks (1 or 2 points outside of the board)
    so maybe it is possible to have groups of deltas for a quick reduction of the number of checks?

    Or maybe just skip the ongoing coordinate translations fully? Drop the flat representation?

    -----------------
    Idea #10 20211119:

    The MapWalker makes a too-deep recursion for default JVMs. It could be solved by increasing the stack, but
    switching to non-recursive would probably be a bit faster anyway.

    -----------------
    Idea #11 20211119:

    For edge 578 there are 2M delta triples, which is ~ the same amount as the number of elements on the board.
    Each triple is checked when placing a mark, making it insanely expensive.

    On the other hand, storing valid deltas for each element requires a large overhead.

    Idea: Represent deltas as packed longs (4 * 2 bytes).
          For each row and each column, store a sorted list of possible valid deltas.
          Resolve deltas for a coordinate by finding all elements common for the row- and the column deltas.

    -----------------
    Idea #12 20211120:

    Select sub-level positional order based on how many neutrals the marker leaves on the board.
    This requires keeping track of untested fields for sub-levels instead of just iterating left->right, top->down.

    -----------------
    Idea #13a 20211121:

    When setting a mark, increment a counter for each potential triple. When selecting next mark, choose the one with
    the lowest counter.

    -----------------
    Idea #13b 20211121:

    Choose the one with the lowest counter, sub sorted by how many areas it effects when set.


    -----------------
    Idea #13c 20211121:

    Thought: Consider the edge 3 below. The priority 2 is a leftover which does not make sense after the rightmost
    marker in the second row has been set. Ideally it should be reduced to 1.
  0:     X   X   .
  1:   X   X   .   X
  2: .   .   .   2   1
  3:   1   1   1   1
  4:     1   1   1       fulls:0

    -----------------
    Idea #13d 20211121:

    Tracking the priority changes requires far too much memory. Recalculate them instead!

    -----------------
    Idea #14a 20211122:

    Observation: Most of the time is spent calling getRectangular(x, y) to check deltas.
                 The getQuadratic computes quadratic[y*width+x].
    Idea: Round width up to nearest power of 2 and use shift and or to get the array index: quadratic[y<<4|x]
          This spares the multiplication.

    Idea #14a 20211122:

    Revisit idea #11 and couple it with #14a: For each row, store an array of possibly valid deltas, but instead
    of storing (x, y), store only the direct offset in quadratic[]. Out of bounds needs to be checked, but
    there is no wrapping offset problem as the row is given for the concrete deltas. There are (2*edge)^2 valid
    elements, 2*edge rows and (overshooting a lot here) the same amount of possible deltas for each element.
    Each delta needs to be able to hold numbers up to (2*edge)^2.
    For the edge 578, this means (2*578)^2 = 1.3M global deltas and thus (2*576)*1.3M ~=  1.5G row deltas.
    As each deltas must hold numbers up to 1.3M and there are 2 of them, this is 2 * 1.3M integers (4 bytes)
    or about 10GB.

    Moderation: 10GB is unrealistically high due to not all rows having all possible deltas. TODO: Extract real stats
    Still, just half of that is quite heavy. Maybe pack deltas multi-dimensionally as an int[column] for each row?

    -----------------
    Idea #15a 20211123 (Thomas):

    Have a few hot spots where the priority penalties are stacked, when selecting fields.

    -----------------
    Idea #15b 20211123:

    Calculate the number of fields which will have their priority changed if a mark is placed, to determine which
    mark costs less.

    Addon: Consider the concrete priorities changed by the potential mark: If some has a poor priority it does not
    matter mich if they are changed.

    Observation 20211118: edge=374 make it to 29 marks in 10 hours. FAR too slow!

    -----------------
    Idea #16 20211127:

    Expanding on idea #4

    There is rotational symmetry, so when any given position has been fully checked, neither that position, nor any of
    its 5 rotational twins, needs to be tried for any sub-walks.

    Extending addVisitedToCurrent to also set the rotational twins to VISITED seems to be the way.

    -----------------
    Idea #17 20211128:

    Pre-adjust priorities before search so that edge positions are better and center is worst.

    Observation: Seems a markedly improvement for some edges, with the flipside being worse for other edges

    -----------------
    Idea #18 20211128:

    Keeping track of sorted "to visit" positions at each depth requires a lot (too much) memory.
    edge=374 requires 5GB+
    edge=419 has OOM at markers=1874 with 8GB heap


    Since tried positions at the current depth in the current subtree are marked as VISITED, recreating the prioritized
    list of positions can be done directly: The first entry in the recreated list will be the starting point.
    Limiting the number of prioritized list to the last X created ones instead of all created ones seems like a
    fix. As backtracking a lot of levels is not that common, the impact of recreation should not be bad.

    Thought: Maybe it would be better overall to fully skip the caching of positions?

    -----------------
    Idea #19 20211201:

    (after discussion with Thomas Egense)

    When the first bottom has been reached, remove the marker which leaves the most elements as neutral, then try and
    fill from there. Repeat until removal of a single marker does not free more new elements. Then shift to removing
    2 markers at a time, etcetera.

    Tricksies: How to avoid endless loops where the new marker(s) are positioned at the old position?

    Performance: Expand the INVALID to include the count of triples that causes the field to be invalid.
    This makes it simple to check how many elements the removal of a marker will cause to return to NEUTRAL.

    Note: This invalidates the mark/rollback stack.

    -----------------
    Idea #20 20211201:

    Instead of rollbacking just 1 level at a time, rollback x% of the total depth at a time, to cause more diverse
    paths to be explored.

    -----------------
    Idea #21 20211207:

    Look at current solutions, select no-go areas for marks, attempt to place marks that optimizes locking the no-gos.
    
    -----------------
    Idea #22 20211210:

    Cache triple-finding by only caching deltas and only storing all possible index deltas for each column.
    Store only the closest triple point for each entry.
    Sort the deltas, making it fast to skip deltas where index+(delta<<1) < 0 and stop using deltas where
    index+(delta<<1) > width*height.
    When delivering the triples, it must be checked that they are not INVALID.

    Update: Unit test testVisitTriplesSpeed shows this is no improvement over calculated

    -----------------
    Idea #23 20211211:

    Make a snapshot-alternative to rollbacks that stores the full state of Mapper. One way would be to add an
    assign(otherMap) method that requires the otherMap to have matching layout.

    -----------------
    Idea #24 20211214:

    Create trackLocked() that stores an array of ILLEGAL elements for each mark (doable as there are at most ~15K).
    Run trackLocked() at the start of shuffling. This makes it cheaper to remove an element.
    When a permutation has been tried, an assign is performed, restoring the arrays of ILLEGAL elements.

    Overall this should work well when the number of permutations is high.

    Bonus: Store an array of triples with marks for each ILLEGAL element. Problem: High memory use.

    -----------------
    Idea #25 20211218:

    Observation: 65, 187, 214, 534, 578 seems "stuck" in a structure with 3 main areas.
                 The seem to be stuck at local maxima.
                 All other make structures where all 6 corners has clusters and with 3 smaller clusters around the
                 middle.
                 adjustPrioritiesShape6Corners3Inner assigns priorities to these areas, forcing the initial fill
                 to take roughly that shape.

    Idea: Re-run the stuck structures to get the 6corner-initial figure then shuffle to try and break free from the
          local maxima.


     */

}
