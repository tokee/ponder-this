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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Al Zi*scramble*mmermann's Progra*scramble*mming Contes*scramble*ts
 * http://azs*scramble*pcs.com/Contest/AP*scramble*Math
 */
public class APMap {
    private static final Logger log = LoggerFactory.getLogger(APMap.class);

    public static final int[] TASKS = new int[]{2, 6, 11, 18, 27, 38, 50, 65, 81, 98,
            118, 139, 162, 187, 214, 242, 273, 305, 338, 374, 411, 450, 491, 534, 578};

    public static void main(String[] args) {
//        new APMap().go(6, 10000000);
        //new APMap().go(4);
   //     new APMap().go(5);
        //new APMap().go(11);
//        new APMap().go(6);
  //      new APMap().go(11);
        //new APMap().go(18);
        Arrays.stream(TASKS).forEach(task -> new APMap().go(task, 100_000_000L/(task*task)));
    }

    private void go(int edge, long maxFulls) {
        log.info("Initializing...");
        long initTime = -System.currentTimeMillis();
        Board board = new Board(edge);
        System.out.println("Board stats: " + board.getStats());
        Walker walker = new Walker(board);
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

    Assume that there will always be at least 1 marker at an edge (corrolary with idea #4: In column 0), so skip
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

    How to multi thread?
    
     */

    public static class Walker {
        final Board board;
        int bestMarkers = 0;
        int[] bestFlat;

        public Walker(Board board) {
            this.board = board;
            bestFlat = board.getFlatCopy();
        }
        public void walk() {
            walk(Long.MAX_VALUE);
        }

        /**
         * Perform exhaustive walk.
         */
        public void walk(long maxFulls) {
            AtomicLong fulls = new AtomicLong(0);
            walk(-1, 0, 0, fulls, maxFulls);
        }

        private void walk(int depth, int position, int setMarkers, AtomicLong fulls, long maxFulls) {
//            System.out.println("ppp " + position + " edge " + ((board.edge>>2)+1));
            if (fulls.get() >= maxFulls ||
                (depth == 0 && position > (board.edge>>2)+1)) { // idea 3 + idea 5
                return;
            }
            if (false && (depth <= 1 || fulls.get()%1000000 == 0)) {
                System.out.printf(
                        Locale.ROOT,
                        "edge=%d, depth=%4d, pos=%5d, nextNeutral=%5d, markers=%d/%d/%d, neutrals=%4d, fulls=%d\n",
                        board.edge, depth, position, board.nextNeutral(position),
                        setMarkers, bestMarkers, board.flatLength(),
                        board.neutralCount(), fulls.get());
            }
//            System.out.println(board);
//            System.out.println();
            if (setMarkers + (board.flatLength()-position) < bestMarkers) {
                return; // We can never beat bestMarkers
            }
            // TODO: Seems we don't iterate the starting point (initial depth)!?
            while ((position = board.nextNeutral(position)) != -1) {

                int illegalCount = board.updateIllegals(position);
                board.change(position, board.illegalsBuffer, illegalCount);
                if (bestMarkers < setMarkers+1) {
                    bestMarkers = setMarkers+1;
                    bestFlat = board.getFlatCopy();
                //    System.out.println(board + " fulls:" + fulls.get());
                //    System.out.println();
                }

                walk(depth+1, position+1, setMarkers+1, fulls, maxFulls);
                board.rollback();
                ++position;
            }
            fulls.incrementAndGet();
        }

        public int getBestMarkers() {
            return bestMarkers;
        }

        public Mapper getBestMapper() {
            Mapper mapper = new Mapper(board.edge);
            mapper.setFlat(bestFlat);
            return mapper;
        }
    }

    public static class Board {
        static final int NEUTRAL = 0;
        static final int MARKER = 1;
        static final int ILLEGAL = 2;

        final int edge;
        final Mapper mapper;
        final int[][] illegalTriples; // Never changes
        final int[] board;
        int totalMarkers = 0;
        int totalNeutrals;
        final List<Change> changes = new ArrayList<>();

        final int[] illegalsBuffer; // To avoid re-allocating it all the time

        public Board(int edge) {
            this.edge = edge;
            mapper = new Mapper(edge);
            illegalTriples = mapper.getFlatTriples();
            board = mapper.getFlat();
            illegalsBuffer = new int[board.length*3];
            totalNeutrals = board.length;
        }

        /**
         * Update the {@link #illegalsBuffer} with the flatIndices that should be marked as illegal if a marker is set
         * at the given flatIndex.
         * @param origo the location of a potential marker.
         * @return the illegalCount (number of entries to consider in {@link #illegalsBuffer}.
         */
        public int updateIllegals(int origo) {
            int illegalsIndex = 0;
            int[] illegalCandidates = illegalTriples[origo];
            for (int i = 0 ; i < illegalCandidates.length ; i+= 3) { // Iterate triples
                for (int ti = i ; ti < i+3 ; ti++) { // In the triple
                    int triplePart = illegalCandidates[ti];
                    if (triplePart != origo && board[triplePart] == MARKER) { // 1+1 = 2 markers. No more room
                        // TODO: If we only store the single relevant illegal, we don't need the check in change(...)
                        illegalsBuffer[illegalsIndex++] = illegalCandidates[i];
                        illegalsBuffer[illegalsIndex++] = illegalCandidates[i+1];
                        illegalsBuffer[illegalsIndex++] = illegalCandidates[i+2];
                        break;
                    }
                }
            }
            return illegalsIndex;
        }

        /**
         * Set marker and illegals. If an illegal position is already marked as illegal, it is ignored.
         * @param marker   a marker for a set point on the board.
         * @param illegals the positions that are illegal to set.
         * @param illegalCount the number of set entries in illegals.
         */
        public void change(int marker, int[] illegals, int illegalCount) {
            // Set the marker
            if (board[marker] != NEUTRAL) {
                throw new IllegalStateException(
                        "Attempted to set marker at " + marker + " which already has state " + board[marker]);
            }
            board[marker] = MARKER;
            ++totalMarkers;
            --totalNeutrals;

            // The state-changing illegals and keep track of them
            final int[] changedIllegals = new int[illegalCount];
            int ciPos = 0;
            for (int i = 0 ; i < illegalCount ; i++) {
                final int illegal = illegals[i];
                if (board[illegal] == NEUTRAL) {
                    board[illegal] = ILLEGAL;
                    changedIllegals[ciPos++] = illegal;
                }
            }
            totalNeutrals -= ciPos;

            // Store the change for later rollback
            final int[] finalIllegals = new int[ciPos];
            System.arraycopy(changedIllegals, 0, finalIllegals, 0, ciPos);
            changes.add(new Change(marker, finalIllegals));
        }

        /**
         * Rollback a previous change. Fails if there are no more rollbacks.
         */
        public void rollback() {
            if (changes.isEmpty()) {
                throw new IllegalStateException("No more changes to roll back");
            }
            Change change = changes.remove(changes.size()-1);
            board[change.marker] = NEUTRAL;
            --totalMarkers;
            ++totalNeutrals;

            for (int i = 0 ; i < change.illegals.length ; i++) {
                board[change.illegals[i]] = NEUTRAL;
            }
            totalNeutrals += change.illegals.length;
        }

        /**
         * Find the next neutral entry, starting at pos and looking forward on the board.
         * @param pos starting position for the search for neutral entries.
         * @return the next neutral position or -1 if there are none.
         */
        public int nextNeutral(int pos) {
            for ( ; pos < board.length ; pos++)
                if (board[pos] == NEUTRAL) {
                    return pos;
                }
            return -1;
        }

        public int getEdge() {
            return edge;
        }

        public int flatLength() {
            return board.length;
        }

        public int markerCount() {
            return totalMarkers;
        }

        public int neutralCount() {
            return totalNeutrals;
        }

        public int getIllegals() {
            return flatLength() - markerCount() - neutralCount();
        }

        /**
         * @return a copy of the flat list of markers and illegals, for use with {@link Mapper#setFlat(int[])}.
         */
        public int[] getFlatCopy() {
            int[] flat = new int[board.length];
            System.arraycopy(board, 0, flat, 0, board.length);
            return flat;
        }

        public String toString() {
            Mapper mapper = new Mapper(edge);
            mapper.setFlat(board);
            return mapper + " side:" + edge + ", marks:" + markerCount() + "/" + flatLength();
        }

        public String getStats() {
            long ic = 0;
            for (int[] illegalTriple: illegalTriples) {
                ic += illegalTriple.length;
            }
            return "Illegal triples: " + ic + ", board entries: " + board.length;
        }
    }

    /**
     * Keeps track of changes to the board for later rollback.
     */
    static final class Change {
        public final int marker;
        public final int[] illegals;

        public Change(int marker, int[] illegals) {
            this.marker = marker;
            this.illegals = illegals;
        }
    }
}
