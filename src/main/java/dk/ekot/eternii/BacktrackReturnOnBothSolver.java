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

import java.util.List;

/**
 * Tries to solve the puzzle by trying all fields, all pieces until there are no more pieces (or fields) or a piece
 * with only 1 possible piece has been tried.
 * Backtracking.
 *
 * Primarily used for testing.
 */
public class BacktrackReturnOnBothSolver implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(BacktrackReturnOnBothSolver.class);

    private final EBoard board;
    private int minFree;
    private long attempts = 0;
    private long startTime;
    private long printDeltaMS = 1000;
    private long nextPrintMS = System.currentTimeMillis();
    private String best = "";

    public BacktrackReturnOnBothSolver(EBoard board) {
        this.board = board;
        minFree = board.getFreeCount();
    }

    @Override
    public void run() {
        startTime = System.currentTimeMillis()-1; // -1 to avoid division by zero
        dive(0);
        log.debug(board.getEdgeTracker().toString());
    }

    /**
     * Iterates all possible fields and pieces, recursively calling for each piece.
     * @return true if the bottom was reached, else false.
     */
    private boolean dive(int depth) {
        if (board.getFreeCount() == 0) { // Bottom reached
            return true;
        }
        if (minFree > board.getFreeCount()) {
            minFree = board.getFreeCount();
            best = board.getDisplayURL();
            System.out.println("Free: " + minFree + ": " + best);

        }
        if (System.currentTimeMillis() > nextPrintMS) {
            System.out.printf("Attempts: %dK, free=%d, minFree=%d, attempts/sec=%d, best=%s\n",
                              attempts/1000, board.getFreeCount(), minFree,
                              attempts*1000/(System.currentTimeMillis()-startTime), best);
            nextPrintMS = System.currentTimeMillis() + printDeltaMS;
        }

        EBoard.Pair<EBoard.Field, List<EBoard.Piece>> free = board.getFreePieceStrategyA();
        if (free == null) {
            return false;
        }
        EBoard.Field field = free.left;
        for (EBoard.Piece piece : free.right) {
//                log.debug("Placing at ({}, {}) piece={} rot={}",
//                          field.getX(), field.getY(), piece.piece, piece.rotation);
            attempts++;
            if (board.placePiece(field.getX(), field.getY(), piece.piece, piece.rotation, Integer.toString(depth))) {
                if (dive(depth+1)) {
                    return true;
                }
                board.removePiece(field.getX(), field.getY());
            }
//                log.debug("Failed placement, trying next (if any)");
        }
        return false;
    }
}
