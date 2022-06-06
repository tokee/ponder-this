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
public class StatsSolver implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(StatsSolver.class);

    private final EBoard board;
    private final Walker walker;

    private int minFree;
    private long attempts = 0;
    private long startTime;
    private long printDeltaMS = 10000;
    private long nextPrintMS = System.currentTimeMillis()+printDeltaMS;
    private String best = "";
    private final int max;
    private long foundSolutions = 0;

    public StatsSolver(EBoard board, Walker walker) {
        this(board, walker, Integer.MAX_VALUE);
    }
    public StatsSolver(EBoard board, Walker walker, int max) {
        this.board = board;
        this.walker = walker;
        this.max = max;
        minFree = board.getFreeCount();
    }

    @Override
    public void run() {
        startTime = System.currentTimeMillis()-1; // -1 to avoid division by zero
        //board.sanityCheckAll();
        dive(0, 1.0);
        //log.debug(board.getEdgeTracker().toString());
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
        if (System.currentTimeMillis() > nextPrintMS) {
            System.out.println("Complete solutions so far: " + foundSolutions);
            nextPrintMS = System.currentTimeMillis() + printDeltaMS;
        }

        EBoard.Pair<EBoard.Field, List<EBoard.Piece>> free = walker.get();
        if (free == null) {
            return false;        }
        EBoard.Field field = free.left;
        for (EBoard.Piece piece : free.right) {
//                log.debug("Placing at ({}, {}) piece={} rot={}",
//                          field.getX(), field.getY(), piece.piece, piece.rotation);
            attempts++;
            if (board.placePiece(field.getX(), field.getY(), piece.piece, piece.rotation, depth + "," + free.right.size())) {
                if (dive(depth+1, possibilities*free.right.size())) {
                    if (piecesPosOK()) {
                        ++foundSolutions;
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

    /**
     * Checks that all positioned pieces matches with edge/no-edge.
     * Does not check if other colors than edge matches.
     * @return true if all places pieces are at a valid position.
     */
    private boolean piecesPosOK() {
        long ok = board.streamAllFields()
                .filter(EBoard.Field::hasPiece)
                .filter(this::piecePosOK)
                .count();
        return ok == board.getFilledCount();
    }

    /**
     * Checks that all the piece with edge/no-edge.
     * Does not check if other colors than edge matches.
     * @return true if the piece is at a valid position.
     */
    private boolean piecePosOK(EBoard.Field field) {
        final int x = field.getX();
        final int y = field.getY();
        final int piece = field.getPiece();
        if ((x == 0 || x == board.getWidth()-1) && ((y == 0 || y == board.getHeight()-1))) {
            return field.getType() == EPieces.CORNER;
        }
        if (x == 0 || x == board.getWidth()-1 || y == 0 || y == board.getHeight()-1) {
            return field.getType() == EPieces.EDGE;
        }
        return field.getType() == EPieces.INNER;
    }
}
