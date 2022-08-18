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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Solver where the behaviour is controlled by a Strategy object.
 */
public class StrategySolver implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(StrategySolver.class);

    private final EBoard board;
    private final Strategy strategy;

    private int minFree;
    private long totalAttempts = 0;
    private long startTime;
    private String best = "";
    private long foundSolutions = 0;

    public StrategySolver(EBoard board, Strategy strategy) {
        this.board = board;
        this.strategy = strategy;
    }

    @Override
    public void run() {
        startTime = System.currentTimeMillis()-1; // -1 to avoid division by zero
        //board.sanityCheckAll();
        dive(0, 1.0, 1, 0);
        log.debug(board.getEdgeTracker().toString());
    }

    public long getFoundSolutions() {
        return foundSolutions;
    }

    /**
     * Iterates all possible fields and pieces, recursively calling for each piece.
     * @return true if the bottom was reached, else false.
     */
    private boolean dive(int depth, double possibilities, long msSinceTop, long attemptsFromTop) {
//        board.sanityCheckAll();
        if (board.getFreeCount() == 0) { // Bottom reached
            return true;
        }
        if (!strategy.shouldProcess(board, depth, attemptsFromTop, totalAttempts,
                                    msSinceTop, (System.currentTimeMillis()-startTime))) {
            return false;
        }
        // TODO: Add refreshCandidates-check to Strategy. Maybe the check should return continue|stop|backtrack|refresh?
        long localTimeDelta = -System.currentTimeMillis();
        Collection<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> candidates = strategy.onlySingleField() ?
                Collections.singleton(strategy.getWalker().get()) :
                strategy.getWalker().getAll().collect(Collectors.toList());
        localTimeDelta += System.currentTimeMillis();

        for (EBoard.Pair<EBoard.Field, List<EBoard.Piece>> free: candidates) {
            EBoard.Field field = free.left;
            for (EBoard.Piece piece : free.right) {
                totalAttempts++;
                attemptsFromTop++;
                localTimeDelta -= System.currentTimeMillis();
                boolean didPlace = board.placePiece(field.getX(), field.getY(), piece.piece, piece.rotation, depth + "," + free.right.size());
                localTimeDelta += System.currentTimeMillis();
                if (didPlace) {
                    if (dive(depth + 1, possibilities * free.right.size(),
                             msSinceTop + localTimeDelta, attemptsFromTop)) {
                        if (++foundSolutions % 1000 == 0) {
                            System.out.println("Complete solutions so far: " + foundSolutions);
                        }
                    }
                    localTimeDelta -= System.currentTimeMillis();
                    board.removePiece(field.getX(), field.getY());
                    localTimeDelta += System.currentTimeMillis();
                }
                if (!strategy.shouldProcess(board, depth, attemptsFromTop, totalAttempts,
                                            msSinceTop + localTimeDelta, (System.currentTimeMillis() - startTime))) {
                    return false;
                }
            }
            if (!strategy.shouldProcess(board, depth, attemptsFromTop, totalAttempts,
                                        msSinceTop + localTimeDelta, (System.currentTimeMillis() - startTime))) {
                return false;
            }
        }
        return false;
    }

}
