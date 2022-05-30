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
 * Tries to solve the puzzle by trying all fields, all pieces until there are no more pieces (or fields).
 * Backtracking when .
 *
 * Primarily used for testing.
 *
 * Note: Always used WalkerA.
 */
public class BacktrackSolver implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(BacktrackSolver.class);

    private final EBoard board;
    private final WalkerA walker;
    private int minFree;
    private final Set<String> encountered = new HashSet<>();
    private long attempts = 0;
    private long printDelta = 1000000;
    private long nextPrint = printDelta;

    public BacktrackSolver(EBoard board, Walker walker) {
        this.board = board;
        this.walker = new WalkerA(board);
        minFree = board.getFreeCount();
    }

    @Override
    public void run() {
        dive("");
        log.debug(board.getEdgeTracker().toString());
    }

    /**
     * Iterates all possible fields and pieces, recursively calling for each piece.
     * @return true if the bottom was reached, else false.
     */
    private boolean dive(String all) {
        if (board.getFreeCount() == 0) { // Bottom reached
            return true;
        }
        if (minFree > board.getFreeCount()) {
            minFree = board.getFreeCount();
            System.out.println("Free: " + minFree);
        }
        if (attempts >= nextPrint) {
            System.out.println("Attempts: " + attempts);
            nextPrint = attempts + printDelta;
        }
        if (!encountered.add(all)) {
            System.out.println("Duplicate: " + all);
        }

        List<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> candidates =
                walker.getFreePieces().collect(Collectors.toList());
        for (EBoard.Pair<EBoard.Field, List<EBoard.Piece>> free: candidates) {
            EBoard.Field field = free.left;

            for (EBoard.Piece piece : free.right) {
//                log.debug("Placing at ({}, {}) piece={} rot={}",
//                          field.getX(), field.getY(), piece.piece, piece.rotation);
                attempts++;
                if (board.placePiece(field.getX(), field.getY(), piece.piece, piece.rotation, "")) {
                    if (dive(all + " (" + field.getX() + ", " + field.getY() + ")" + board.getPieces().toDisplayString(piece.piece, piece.rotation))) {
                        return true;
                    }
                    board.removePiece(field.getX(), field.getY());
                }
//                log.debug("Failed placement, trying next (if any)");
            }
//            if (field.getPiece() == -1) {
//                log.info("Tried all pieces " + free.right + " at " + free.left + " without finding a valid one");
//            }
        }
        return false;
    }
}
