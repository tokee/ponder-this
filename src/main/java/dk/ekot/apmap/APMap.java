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
import java.util.List;

/**
 * Al Zimmermann's Programming Contests
 * http://azspcs.com/Contest/APMath
 */
public class APMap {
    private static final Logger log = LoggerFactory.getLogger(APMap.class);

    public static void main(String[] args) {
        new APMap().go();
    }

    private void go() {
        System.out.println("Nothing yet");
    }

    /*
    Initial idea:

    Represent the hexagonal grid as a single array of bytes (or ints if that is faster on the concrete architecture).
    Entries can one of [neutral, marker, illegal]. The board changes all the time, so efficient state change with switch
    to the previous state is needed. Also keep track of the number of markers as well as the number of neutral (for
    maximum possible score from current state).

    For each entry there is a static array of arrays of positions that lines up with the entry: There can only be 1
    marker set in each of the inner arrays. Consequently, when a point is set in one of the inner arrays, all the other
    positions on the board must be set to illegal.

    -----------------
    Second idea:

    Instead of creating copies of the board, just keep track of the changes: The single marker and the list of illegals.
    With single threading, rollback is deterministic as long as marking an already illegal field as illegal is ignored.

    How to multi thread?
    
     */

    public static class Board {
        static final int NEUTRAL = 0;
        static final int MARKER = 1;
        static final int ILLEGAL = 2;

        final int[] board;
        int totalMarkers = 0;
        int totalNeutrals;
        final List<Change> changes = new ArrayList<>();

        public Board(int size) {
            this.board = new int[size];
            totalNeutrals = size;
        }

        /**
         * Mark set af
         * @param marker
         * @param illegals
         */
        public void change(int marker, int[] illegals) {
            // Set the marker
            if (board[marker] != NEUTRAL) {
                throw new IllegalStateException(
                        "Attempted to set marker at " + marker + " which already has state " + board[marker]);
            }
            board[marker] = MARKER;
            ++totalMarkers;
            --totalNeutrals;

            // The state-changing illegals and keep track of them
            final int[] changedIllegals = new int[illegals.length];
            int ciPos = 0;
            for (int i = 0 ; i < illegals.length ; i++) {
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
            --totalNeutrals;

            for (int i = 0 ; i < change.illegals.length ; i++) {
                board[change.illegals[i]] = NEUTRAL;
            }
            totalNeutrals -= change.illegals.length;
        }

        public int size() {
            return board.length;
        }

        public int markerCount() {
            return totalMarkers;
        }

        public int neutralCount() {
            return totalNeutrals;
        }

        public int getIllegals() {
            return size()-markerCount()-neutralCount();
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
