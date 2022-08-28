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
import java.util.Collection;

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
    private final Walker walker;

    private int minFree;
    private long attempts = 0;
    private long startTime;
    private long printDeltaMS = 1000;
    private long nextPrintMS = System.currentTimeMillis();
    private String best = "";
    private final int max;
    private long foundSolutions = 0;
    static {
        File ef = new File("eternii");
        if (!ef.exists()) {
            ef.mkdir();
        }
    }
    private PerformanceCollector collector = new PerformanceCollector("eternii/g2");

    public BacktrackReturnOnBothSolver(EBoard board, Walker walker) {
        this(board, walker, Integer.MAX_VALUE);
    }
    public BacktrackReturnOnBothSolver(EBoard board, Walker walker, int max) {
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

        Walker.Move move = walker.get();
        if (move == null) {
            return false;
        }
        Collection<Integer> pieceIDs = move.getPieceIDs();
        for (int pieceID : pieceIDs) {
//                log.debug("Placing at ({}, {}) piece={} rot={}",
//                          field.getX(), field.getY(), piece.piece, piece.rotation);
            for (int rotation : move.getValidRotations(pieceID)) {
                //System.out.println("Placing " + board.getPieces().toDisplayString(pieceID, rotation));
                attempts++;
                if (board.placePiece(move.getX(), move.getY(), pieceID, rotation, depth + "," + pieceIDs.size())) {
                    if (dive(depth + 1, possibilities * pieceIDs.size())) {
                        if (++foundSolutions % 1000 == 0) {
                            System.out.println("Complete solutions so far: " + foundSolutions);
                        }
                    }
                    board.removePiece(move.getX(), move.getY());
                } else {
//                    System.out.println("Unable to place piece. Stopping");
  //                  try {
    //                    Thread.sleep(10000000);
      //              } catch (InterruptedException e) {
        //                throw new RuntimeException(e);
          //          }
                }

            /* else {
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
        }
        return false;
    }
}
