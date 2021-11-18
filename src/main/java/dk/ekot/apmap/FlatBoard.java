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

import java.util.ArrayList;
import java.util.List;

/**
 * State for recursive walks on a flat structure, tracking changes in objects and pre-generating all triple-coordinates.
 * The triple-coordinates has way too much memory overhead to be feasible for 150+ edge hexagons.
 */
public class FlatBoard {
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

    public FlatBoard(int edge) {
        this.edge = edge;
        mapper = new Mapper(edge);
        illegalTriples = mapper.getFlatTriples();
        board = mapper.getFlat();
        illegalsBuffer = new int[board.length * 3];
        totalNeutrals = board.length;
    }

    /**
     * Update the {@link #illegalsBuffer} with the flatIndices that should be marked as illegal if a marker is set
     * at the given flatIndex.
     *
     * @param origo the location of a potential marker.
     * @return the illegalCount (number of entries to consider in {@link #illegalsBuffer}.
     */
    public int updateIllegals(int origo) {
        int illegalsIndex = 0;
        int[] illegalCandidates = illegalTriples[origo];
        for (int i = 0; i < illegalCandidates.length; i += 3) { // Iterate triples
            for (int ti = i; ti < i + 3; ti++) { // In the triple
                int triplePart = illegalCandidates[ti];
                if (triplePart != origo && board[triplePart] == MARKER) { // 1+1 = 2 markers. No more room
                    // TODO: If we only store the single relevant illegal, we don't need the check in change(...)
                    illegalsBuffer[illegalsIndex++] = illegalCandidates[i];
                    illegalsBuffer[illegalsIndex++] = illegalCandidates[i + 1];
                    illegalsBuffer[illegalsIndex++] = illegalCandidates[i + 2];
                    break;
                }
            }
        }
        return illegalsIndex;
    }

    /**
     * Set marker and illegals. If an illegal position is already marked as illegal, it is ignored.
     *
     * @param marker       a marker for a set point on the board.
     * @param illegals     the positions that are illegal to set.
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
        for (int i = 0; i < illegalCount; i++) {
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
        Change change = changes.remove(changes.size() - 1);
        board[change.marker] = NEUTRAL;
        --totalMarkers;
        ++totalNeutrals;

        for (int i = 0; i < change.illegals.length; i++) {
            board[change.illegals[i]] = NEUTRAL;
        }
        totalNeutrals += change.illegals.length;
    }

    /**
     * Find the next neutral entry, starting at pos and looking forward on the board.
     *
     * @param pos starting position for the search for neutral entries.
     * @return the next neutral position or -1 if there are none.
     */
    public int nextNeutral(int pos) {
        for (; pos < board.length; pos++)
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
        for (int[] illegalTriple : illegalTriples) {
            ic += illegalTriple.length;
        }
        return "Illegal triples: " + ic + ", board entries: " + board.length;
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
