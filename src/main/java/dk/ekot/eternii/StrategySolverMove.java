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

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Solver where the behaviour is controlled by a Strategy object.
 */
public class StrategySolverMove implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(StrategySolverMove.class);

    private final EBoard board;
    private StrategySolverState state;
    private final Strategy strategy;

    private long foundSolutions = 0;

    public StrategySolverMove(EBoard board, Strategy strategy) {
        this.board = board;
        this.state = new StrategySolverState(board);
        this.strategy = strategy;
    }

    @Override
    public void run() {
        //board.sanityCheckAll();
        dive(0, 1.0, 1, 0);
        //log.debug(board.getEdgeTracker().toString());
    }

    public long getFoundSolutions() {
        return foundSolutions;
    }

    /**
     * Iterates all possible fields and pieces, recursively calling for each piece.
     * @return true if the bottom was reached, else false.
     */
    private Strategy.Action dive(int depth, double possibilities, long msFromTop, long attemptsFromTop) {
//        board.sanityCheckAll();
        state.incAttemptsTotal();
        state.setPossibilities(possibilities);
        state.setAttemptsFromTop(attemptsFromTop);
        state.setLevel(depth);
        state.setMsFromTop(msFromTop);

        // TODO: Reconsider this
        if (board.getFreeCount() == 0) { // Bottom reached
            return Strategy.Action.continueLocal();
        }

        Strategy.Action action = strategy.getAction(state);
        switch (action.command) {
            case quit: return action;
            case continueLocal: break;
            case continueLevel:
            case restartLevel:
                if (action.level < depth) {
                    return action;
                }
        }

        boolean restart;
        do {
            restart = depth == 0 && strategy.loopLevelZero();

            msFromTop -= System.currentTimeMillis();
            Collection<Walker.Move> candidates = strategy.onlySingleField() ?
                    Collections.singleton(strategy.getWalker().get()) :
                    strategy.getWalker().getAll().collect(Collectors.toList());
            msFromTop += System.currentTimeMillis();

            skipCurrent:
            for (Walker.Move move : candidates) {
                final int[] pieceIDs = move.getPieceIDsArray();
                for (int pieceID : pieceIDs) {
                    for (int rotation : move.getValidRotations(pieceID)) {

                        state.incAttemptsTotal();
                        state.setAttemptsFromTop(++attemptsFromTop);

                        msFromTop -= System.currentTimeMillis();
                        boolean didPlace = board.placePiece(
                                move.getX(), move.getY(), pieceID, rotation,
                                depth + "," + pieceIDs.length, !strategy.acceptsUnresolvable());
                        msFromTop += System.currentTimeMillis();

                        if (didPlace) {
                            action = dive(depth + 1, possibilities * pieceIDs.length, msFromTop, attemptsFromTop);
                            msFromTop -= System.currentTimeMillis();
                            board.removePiece(move.getX(), move.getY(), !strategy.acceptsUnresolvable());
                            msFromTop += System.currentTimeMillis();
                            state.setMsFromTop(msFromTop);
                        } else {
                            // TODO: Collect all non.placed minus current piece, increase counter for each/count(non.placed)
                            action = strategy.getAction(state);
                        }
                        switch (action.command) {
                            case quit:
                                return action;
                            case continueLocal:
                                break;
                            case continueLevel:
                                if (action.level < depth) {
                                    return action;
                                }
                                action = Strategy.Action.continueLocal();
                                break;
                            case restartLevel:
                                if (action.level < depth) {
                                    return action;
                                }
                                action = Strategy.Action.continueLocal();
                                restart = true;
                                break skipCurrent;
                        }
                    }
                }
            }
        } while (restart);
        return action;
    }

}
