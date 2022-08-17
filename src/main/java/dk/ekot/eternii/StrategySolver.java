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
package dk.ekot.eternii;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Solver where the behaviour is controlled by a Strategy object.
 */
public class StrategySolver implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(StrategySolver.class);

    private final EBoard board;

    private int minFree;
    private long attempts = 0;
    private long startTime;
    private long printDeltaMS = 1000;
    private long nextPrintMS = System.currentTimeMillis();
    private String best = "";
    private long foundSolutions = 0;
    static {
        File ef = new File("eternii");
        if (!ef.exists()) {
            ef.mkdir();
        }
    }
    private PerformanceCollector collector = new PerformanceCollector("eternii/g2");

    public StrategySolver(EBoard board, Strategy strategy) {
        this.board = board;
        this.strategy = strategy;
    }

    @Override
    public void run() {
        startTime = System.currentTimeMillis()-1; // -1 to avoid division by zero
        //board.sanityCheckAll();
        dive(0, 1.0);
        log.debug(board.getEdgeTracker().toString());
    }

    public long getFoundSolutions() {
        return foundSolutions;
    }

    /**
     * Iterates all possible fields and pieces, recursively calling for each piece.
     * @return true if the bottom was reached, else false.
     */
    private boolean dive(int depth, double possibilities) {
//        board.sanityCheckAll();
        if (board.getFreeCount() == 0) { // Bottom reached
            return true;
        }
        if (depth == max) {
            return true;
        }
        if (minFree > board.getFreeCount()) {
            minFree = board.getFreeCount();
            best = board.getDisplayURL();
            collector.collect(board.getFilledCount(), attempts, best);
        }
        if (System.currentTimeMillis() > nextPrintMS) {
            int max = board.getWidth()*board.getHeight();
            System.out.printf("Attempts: %dK, placed=%3d|%3d, att/sec=%dK, possible=%3.0e best=%s\n",
                              attempts/1000, board.getFilledCount(), max-minFree,
                              attempts/(System.currentTimeMillis()-startTime), possibilities, best);
            nextPrintMS = System.currentTimeMillis() + printDeltaMS;
        }

        EBoard.Pair<EBoard.Field, List<EBoard.Piece>> free = walker.get();
        if (free == null) {
            return false;
        }
        EBoard.Field field = free.left;
        for (EBoard.Piece piece : free.right) {
//                log.debug("Placing at ({}, {}) piece={} rot={}",
//                          field.getX(), field.getY(), piece.piece, piece.rotation);
            attempts++;
            if (board.placePiece(field.getX(), field.getY(), piece.piece, piece.rotation, depth + "," + free.right.size())) {
                if (dive(depth+1, possibilities*free.right.size())) {
                    if (++foundSolutions % 1000 == 0) {
                        System.out.println("Complete solutions so far: " + foundSolutions);
                    }
                }
                board.removePiece(field.getX(), field.getY());
            }/* else {
                if (piece.piece > 3) {
                    System.out.println("Solver: Could not place @" + field + ": " + piece + " " + board.getPieces().toDisplayString(piece.piece, piece.rotation));
                    board.placeUntrackedPiece(field.getX()+1, field.getY(), piece.piece, piece.rotation);
                    try {
                        Thread.sleep(100000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }  */
//                log.debug("Failed placement, trying next (if any)");
        }
        return false;
    }

    public interface Strategy {
        /**
         * The log typically contains a seed in the case of a random walker and information about the walker and
         * solver properties. This log is 
         * @param logLine log info relevant for replay or understanding.
         */
        void addToLog(String logLine);

        /**
         * If true, a piece is accepted even if the Solver is able to determine that the complete puzzle can never be
         * solved from the given position. Set this to false to search for "most pieces placed".
         * @return true if the Solver should continue even if the puzzle cannot be solved completely.
         */
        boolean acceptsUnresolvable();

        /**
         * If true, the Solver should only request a single field from the Walker.
         * If false, the solver should request a list of prioritized fields from the Walker.
         * @return true if the solver should only try a a single field before giving up at the current level.
         */
        boolean onlySingleField();

        /**
         * `getMatcher()` is called <strong>every time</strong> the solver needs to call a matcher.
         * This makes it possible to change Matcher on the fly.
         * @return the matcher to use to resolve the next field(s) and pieces to try.
         */
        Matcher getMatcher();

        /**
         * True if processing should continue under the given conditions.
         * @param level the depth aka number of positioned pieces.
         * @param attemptsFromTop number of attempts at placing pieces, measured from the top to the current level.
         * @param attemptsTotal total attempts, including attempts in other sub-trees.
         * @param msFromTop number of milliseconds spend, measured from the top to the current level.
         * @param msTotal number of milliseconds spend on all processing.
         * @return true if processing of the current level should continue.
         */
        boolean shouldProcess(int level, long attemptsFromTop, long attemptsTotal, long msFromTop, long msTotal);
    }
}
