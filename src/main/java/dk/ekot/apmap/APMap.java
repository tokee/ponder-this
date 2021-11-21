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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Al Zi*scramble*mmermann's Progra*scramble*mming Contes*scramble*ts
 * http://azs*scramble*pcs.com/Contest/AP*scramble*Math
 */
public class APMap {
    private static final Logger log = LoggerFactory.getLogger(APMap.class);

    public static final int[] TASKS = new int[]{2, 6, 11, 18, 27, 38, 50, 65, 81, 98,
            118, 139, 162, 187, 214, 242, 273, 305, 338, 374, 411, 450, 491, 534, 578};

    // java -cp target/ponder-this-0.1-SNAPSHOT-jar-with-dependencies.jar dk.ekot.apmap.APMap


    public static void main(String[] args) {
//        new APMap().go(6, 10000000);

        //Arrays.stream(TASKS).
          //      boxed().
            //    forEach(task -> new Mapper(task).dumpDeltaStats());

        long startTime = System.currentTimeMillis();
        int RUN[] = new int[]{5, 6, 11, 18, 27, 38, 50};
  //      int RUN[] = TASKS;
        boolean SHOW_BEST = true;
        int STALE_MS = 24*60*60*1000;

//        new Mapper(118).dumpDeltaStats();
        new APMap().goQuadratic(2, true);
        if (1==1) return;

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
        results.forEach(s -> System.out.println(s + "\n\n"));
        results.forEach(s -> System.out.println("edge=" + s.edge + ", marks=" + s.marked + ":\n" + s.toJSON() + ";"));

        System.out.println("Total time: " + (System.currentTimeMillis()-startTime)/1000 + "s");
    }

    private Mapper goQuadratic(int edge) {
        return goQuadratic(edge, Integer.MAX_VALUE, true);
    }
    private Mapper goQuadratic(int edge, boolean showBest) {
        return goQuadratic(edge, Integer.MAX_VALUE, showBest);
    }
    private Mapper goQuadratic(int edge, int maxStaleMS, boolean showBest) {
        log.info("Initializing for edge " + edge + "...");
        long initTime = -System.currentTimeMillis();
        Mapper board = new Mapper(edge);
        //System.out.println("Board stats: " + board.getStats());
        MapWalker walker = new MapWalker(board);
        initTime += System.currentTimeMillis();

        log.info("Walking for edge " + edge + "...");
        long walkTime = -System.currentTimeMillis();
        walker.walkStraight(maxStaleMS, showBest);
        walkTime += System.currentTimeMillis();

        System.out.printf(Locale.ROOT, "edge=%d, marks=%d/%d, initTime=%ds, walkTime=%ds: %s\n",
                          edge, walker.getBestBoard().getMarkedCount(), board.valids,
                          initTime/1000, walkTime/1000, walker.getBestBoard().toJSON());
        return walker.getBestBoard();
    }

    // -----------------------------------------------------------------------------------------------------------------

    private void goFlat(int edge) {
        goFlat(edge, Long.MAX_VALUE);
    }
    private void goFlat(int edge, long maxFulls) {
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
    Idea #13 20211121:

    When setting a mark, increment a counter for each potential triple. When selecting next mark, choose the one with
    the lowest counter.

    Secondarily: Choose the one with the lowest counter, sub sorted by how many areas it effects when set.



    How to multi thread?
    
     */

}
