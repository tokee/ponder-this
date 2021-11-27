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

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * Al Zi*scramble*mmermann's Progra*scramble*mming Contes*scramble*ts
 * http://azs*scramble*pcs.com/Contest/AP*scramble*Math
 */
public class APMap {
    private static final Logger log = LoggerFactory.getLogger(APMap.class);

    public static final int[] TASKS = new int[]{
            2, 6, 11, 18, 27, 38, 50, 65, 81, 98,
            118, 139, 162, 187,
            214, 242, 273,
            305, 338, 374,
            411, 450, 491,
            534, 578};

    public static final int[][] BESTS = new int[][]{
            // edge, local, global,
            {2, 6, 6}, {6, 32, 33}, {11, 70, 80}, {18, 123, 153}, {27, 213, 266}, {38, 370, 420},
            {50, 470, 621}, {65, 768, 884}, {81, 813, 1193}, {98, 1010, 1512},
            {118, 1246, 1973}, {139, 1615, 2418}, {162, 1942, 2915}, {187, 3072, 3515},
            {214, 3072, 4198}, {242, 3085, 4922}, {273, 3353, 5736},
            {305, 3915, 6648}, {338, 4380, 7691}, {374, 4778, 8962},
            {411, 5422, 10060}, {450, 7077, 11123}, {491, 6632, 12534},
            {534, 7239, 14046}, {578, 12288, 15457}};

    // java -cp target/ponder-this-0.1-SNAPSHOT-jar-with-dependencies.jar dk.ekot.apmap.APMap


    public static void main(String[] args) {
       // testMarking();
        //if (1==1) return;

        goFlat(6); if (1 == 1) return;

        //System.out.println(new Mapper(20)); if (1==1) return;
        //        new APMap().go(6, 10000000);

        //Arrays.stream(TASKS).
          //      boxed().
            //    forEach(task -> new Mapper(task).dumpDeltaStats());
        //new Mapper(140).dumpDeltaStats(); if (1==1) return;
        // edge=100, valids=29701, uniqueDeltas=59550, sumDeltas=330772500, minDeltas=7354, averageDeltas=11136, maxDeltas=22200, time=6s
        // edge=120, valids=42841, uniqueDeltas=85860, sumDeltas=688208400, minDeltas=10624, averageDeltas=16064, maxDeltas=32040, time=14s
        // edge=140, valids=58381, uniqueDeltas=116970, sumDeltas=1278062100, minDeltas=14494, averageDeltas=21891, maxDeltas=43680, time=24s


        //new APMap().goQuadratic(50, 30_000, true);
        System.out.println(new APMap().goQuadratic(4, 10_000, true, 2_000));

        if (1==1) return;
        //processRemaining(1_000);


        long startTime = System.currentTimeMillis();
        int RUN[] = new int[]{27, 38, 65, 81};
        //      int RUN[] = TASKS;
        boolean SHOW_BEST = true;
        int STALE_MS = 120*1000; // 2 min

//        new Mapper(118).dumpDeltaStats();
//        new APMap().goQuadratic(534, 120_000, true);
        //new APMap().goQuadratic(3, 120_000, true, 10_000);
//        if (1==1) return;

//        Arrays.stream(TASKS).forEach(task -> new APMap().goQuadratic(task, 100_000_000L / (task * task)));
        //new APMap().goFlat(4);
//        new APMap().goQuadratic(4, 500_000, true);
//        if (1==1) return;
        //new APMap().goQuadratic(6, true);
//        new APMap().goQuadratic(450, true);
        //new APMap().go(11);
//        new APMap().go(6);
  //      new APMap().go(11);
        //new APMap().go(18);

        List<Mapper> results = Arrays.stream(RUN).parallel().
                boxed().
                map(task -> new APMap().goQuadratic(task, STALE_MS, SHOW_BEST)).
                collect(Collectors.toList());
        System.out.println();
//        results.forEach(s -> System.out.println(s + "\n\n"));
        results.forEach(s -> System.out.printf(
                "edge=%d, marks=%d/%d, walkTime=%ds, completed=%b: %s\n",
                s.edge, s.getMarkedCount(), s.valids, s.getWalkTimeMS()/1000, s.isCompleted(), s.toJSON()));

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
                                   map(task -> new APMap().goQuadratic(task, maxStaleMS, true)).
                                   collect(Collectors.toList()));
        }).join();
        
        System.out.println();
//        results.forEach(s -> System.out.println(s + "\n\n"));
        System.out.println("All results:");
        results.forEach(s -> System.out.printf(
                "edge=%d, marks=%d/%d, walkTime=%ds, completed=%b: %s\n",
                s.edge, s.getMarkedCount(), s.valids, s.getWalkTimeMS()/1000, s.isCompleted(), s.toJSON()));

        System.out.println("Improvements:");
        Map<Integer, Integer> localBest = Arrays.stream(BESTS).
                map(b -> Arrays.copyOf(b, 2)).
                collect(Collectors.toMap(b -> b[0], b -> b[1]));
        results.stream().
                filter(board -> board.marked > localBest.get(board.edge)).
                forEach(s -> System.out.printf(
                        "edge=%d, marks=%d/%d, walkTime=%ds, completed=%b: %s\n",
                        s.edge, s.getMarkedCount(), s.valids, s.getWalkTimeMS()/1000, s.isCompleted(), s.toJSON()));

        System.out.println("Total time: " + (System.currentTimeMillis()-startTime)/1000 + "s");
    }

    private static void testMarking() {
        Mapper board = new Mapper(3);
        Mapper.PriorityPos pos = new Mapper.PriorityPos(2, 0, 0);
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

    private Mapper goQuadratic(int edge) {
        return goQuadratic(edge, Integer.MAX_VALUE, true, Integer.MAX_VALUE);
    }
    private Mapper goQuadratic(int edge, boolean showBest) {
        return goQuadratic(edge, Integer.MAX_VALUE, showBest, Integer.MAX_VALUE);
    }
    private Mapper goQuadratic(int edge, int maxStaleMS, boolean showBest) {
        return goQuadratic(edge, maxStaleMS, showBest, Integer.MAX_VALUE);
    }
    private Mapper goQuadratic(int edge, int maxStaleMS, boolean showBest, int showBoardIntervalMS) {
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
        walker.walkFlexible(maxStaleMS, showBest, showBoardIntervalMS, Mapper.getPositionComparator(), false);
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

    -----------------
    Idea #16 20211127:

    Expanding on idea #4

    There is rotational symmetry, so when any given position has been fully checked, neither that position, nor any of
    its 5 rotational twins, needs to be tried for any sub-walks.

    Extending addVisitedToCurrent to also set the rotational twins to VISITED seemt to be the way.


     */

}
