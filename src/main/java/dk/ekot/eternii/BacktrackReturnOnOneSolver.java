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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tries to solve the puzzle by trying all fields, all pieces until there are no more pieces (or fields) or a piece
 * with only 1 possible piece has been tried.
 * Backtracking.
 *
 * Primarily used for testing.
 */
public class BacktrackReturnOnOneSolver implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(BacktrackReturnOnOneSolver.class);

    private final EBoard board;
    private int minFree;
    private long attempts = 0;
    private long printDelta = 10000000;
    private long nextPrint = printDelta;

    public BacktrackReturnOnOneSolver(EBoard board) {
        this.board = board;
        minFree = board.getFreeCount();
    }

    @Override
    public void run() {
        dive();
        log.debug(board.getEdgeTracker().toString());
    }

    /**
     * Iterates all possible fields and pieces, recursively calling for each piece.
     * @return true if the bottom was reached, else false.
     */
    private boolean dive() {
        if (board.getFreeCount() == 0) { // Bottom reached
            return true;
        }
        if (minFree > board.getFreeCount()) {
            minFree = board.getFreeCount();
            System.out.println("Free: " + minFree + ": " + board.getDisplayURL());
        }
        if (attempts >= nextPrint) {
            System.out.println("Attempts: " + attempts/1000 + "K, free=" + board.getFreeCount() + ", min=" + minFree);
            nextPrint = attempts + printDelta;
        }

        List<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> candidates =
                board.getFreePiecesStrategyA().collect(Collectors.toList());
        for (EBoard.Pair<EBoard.Field, List<EBoard.Piece>> free: candidates) {
            EBoard.Field field = free.left;

            for (EBoard.Piece piece : free.right) {
//                log.debug("Placing at ({}, {}) piece={} rot={}",
//                          field.getX(), field.getY(), piece.piece, piece.rotation);
                attempts++;
                if (board.placePiece(field.getX(), field.getY(), piece.piece, piece.rotation)) {
                    if (dive()) {
                        return true;
                    }
                    board.removePiece(field.getX(), field.getY());
                }
//                log.debug("Failed placement, trying next (if any)");
            }
            // Does not help to permutate when we know the single possible piece does not fit
            if (free.right.size() == 1) {
                return false;
            }
//            if (field.getPiece() == -1) {
//                log.info("Tried all pieces " + free.right + " at " + free.left + " without finding a valid one");
//            }
        }
        return false;
    }
}
