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
package dk.ekot.eternii.quad;

import dk.ekot.eternii.Walker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class QSolverBacktrack implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(QSolverBacktrack.class);

    private final QBoard board;
    private final QWalker walker;
    private final int maxDepth;
    private final long maxAttempts;

    private long attempts = 0;
    private int minFree = 256;
    private String best = "";
    private long startTimeMS;
    private long nextPrintMS = System.currentTimeMillis();
    private long printDeltaMS = 1000;


    public QSolverBacktrack(QBoard board, QWalker walker) {
        this(board, walker, Integer.MAX_VALUE, Long.MAX_VALUE);
    }

    public QSolverBacktrack(QBoard board, QWalker walker, int maxDepth, long maxAttempts) {
        this.board = board;
        this.walker = walker;
        this.maxDepth = maxDepth;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public void run() {
        startTimeMS = System.currentTimeMillis();
        dive(0, 1.0);
        log.debug(board.getEboard().getDisplayURL());
    }

    private boolean dive(int depth, double possibilities) {
        //log.debug("Starting dive at depth {} with {} possibilities, attempts {}/{}",
        //        depth, possibilities, attempts, maxAttempts);
        int free = board.getPieceTracker().cardinality();

        if (free == 0) { // Bottom reached
            log.info("Reached bottom! Board: {}", board.getEboard().getDisplayURL());
            return true;
        }
        if (attempts >= maxAttempts) {
            log.info("Reached maxAttempts {}. Board: {}", maxAttempts, board.getEboard().getDisplayURL());
            return false;
        }
        if (depth == maxDepth) {
            return true;
        }
        if (minFree > free) {
            minFree = free;
            best = (256-free) + " free: " + board.getEboard().getDisplayURL();
            // TODO: Add persisting collector as BacktrackReturnOnBothSolver uses
            log.info("free={} / filled={}, board={}", free, 256-free, board.getEboard().getDisplayURL());
        }

        if (System.currentTimeMillis() > nextPrintMS) {
            int max = 256;
            System.out.printf("Attempts: %d, placed=%3d|%3d, att/sec=%d, possible=%3.0e best=%s\n",
                              attempts, max-free, max-minFree,
                              attempts*1000/(System.currentTimeMillis()-startTimeMS), possibilities, best);
            nextPrintMS = System.currentTimeMillis() + printDeltaMS;
        }

        final QWalker.Move move = walker.getMove();
        if (move == null) {
            log.warn("Got no move at depth {}", depth);
            return false;
        }
        final QField field = move.getField();
        //log.debug("Walking depth={}, field=({}, {})", depth, field.getX(), field.getY());
        move.getAvailableQuadIDs().limit(maxAttempts-attempts).forEach(quadID -> {
            attempts++;
            //log.debug("Placing quad {} on ({}, {})", quadID, field.getX(), field.getY());
            board.placePiece(move.getX(), move.getY(), quadID);
            board.setSequence(move.getX(), move.getY(), depth);
            if (board.areNeedsSatisfiedAll()) {
                // TODO: Optimize field.getAvailableQuadIDs().count() and use that instead of 2
                if (dive(depth + 1, possibilities * 2)) {
                    log.debug("Got solution for maxDepth={}: {}", depth, board.getEboard().getDisplayURL());
                }
            //} else {
            //    log.debug("Needs not satisfied when positioning quad {} on ({}, {})",
            //            quadID, field.getX(), field.getY());
            }
            //log.debug("removing quad {} on ({}, {})", quadID, field.getX(), field.getY());
            board.removePiece(move.getX(), move.getY());
        });
        return false;
    }
}
