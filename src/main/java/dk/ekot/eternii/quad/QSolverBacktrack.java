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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

/**
 *
 */
public class QSolverBacktrack implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(QSolverBacktrack.class);

    private final QBoard board;
    private final QWalker walker;
    private final QSetup setup;
    /**
     * Called whenever a call to {@link #dive(int, double)} returns true.<br/>
     * Integer: Current depth, counting from 0.<br/>
     * QBoard: The current board.
     * Boolean (return value): If true, continue trying quads at current depth, else return to higher level.
     */
    public final static BiFunction<Integer, QBoard, Boolean> PRINT_ALL = (depth, board) -> {
        log.debug("Got solution for maxDepth={}: {}", (depth+1), board.getEboard().getDisplayURL());
        return true;
    };

    private final long printDeltaMS = 1000;
    // Until this depth, the stats are precise
    private final int fineCountDepth = 25;

    private long attempts = 0;
    private int minFree = 256;
    private String best = "";
    private long startTimeMS;
    private long nextPrintMS = System.currentTimeMillis();


    public QSolverBacktrack(QBoard board, QSetup setup) {
        this.board = board;
        this.setup = setup;
        this.walker = setup.getWalker(board);
    }

    @Override
    public void run() {
        startTimeMS = System.currentTimeMillis()-1; // -1 to avoid devision by zero
        dive(0, 1.0);
        log.debug(board.getEboard().getDisplayURL());
    }
    final DecimalFormat eFormat = new DecimalFormat("##0.0E0");
    private boolean dive(int depth, double possibilities) {
        //log.debug("Starting dive at depth {} with {} possibilities, attempts {}/{}",
        //        depth, possibilities, attempts, maxAttempts);
        int free = board.getPieceTracker().cardinality();

        if (free == 0) { // Bottom reached
            log.info("Reached bottom! Board: {}", board.getEboard().getDisplayURL());
            return true;
        }
        if (attempts >= setup.getMaxAttempts()) {
            log.info("Reached maxAttempts {}. Board: {}", setup.getMaxAttempts(), board.getEboard().getDisplayURL());
            return false;
        }
        if (depth == setup.getMaxDepth()) {
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
        AtomicInteger quadsTried = new AtomicInteger(0);

        final int max = field.getMaxAvailable();
        final int available = depth < fineCountDepth ? field.available() : -1;
        final String maxQuads = depth < fineCountDepth ? "=" + available :
                max > 1000000 ? max / 1000000 + "M-" :
                        max > 1000 ? max / 1000 + "K-" :
                                max + "-";

        // TODO: Consider removing the limit as it is a leftover from experimentation
        IntStream moves = setup.getQuadDelivery().apply(move).limit(setup.getMaxAttempts()-attempts);
        moves.forEach(quadID -> {
            quadsTried.incrementAndGet();
            attempts++;
            //log.debug("Placing quad {} on ({}, {})", quadID, field.getX(), field.getY());
            board.placePiece(move.getX(), move.getY(), quadID);
            board.setFieldText(move.getX(), move.getY(), depth + ": " + quadsTried.get() + "/" + maxQuads,
                               eFormat.format(possibilities));
            if (board.areNeedsSatisfiedAll()) {
                // TODO: Optimize field.getAvailableQuadIDs().count() and use that instead of 2
                if (dive(depth + 1, possibilities * (available == -1 ? max : available))) {
                    setup.getSolutionCallback().apply(depth, board);
                    //log.debug("Got solution for maxDepth={}: {}", (depth+1), board.getEboard().getDisplayURL());
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


    /**
     * Prints solutions, but only every {@code modulo}.
     */
    public static BiFunction<Integer, QBoard, Boolean> getSolutionPrinter(int modulo) {
        final AtomicLong counter = new AtomicLong(0);
        return (depth, board) -> {
            if (counter.getAndIncrement() % modulo == 0) {
                log.debug("Got solution #{} from depth={}: {}",
                          counter, (depth + 1), board.getEboard().getDisplayURL());
            }
            return true;
        };
    }
}
