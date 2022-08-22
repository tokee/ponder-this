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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Responsible for starting and stopping solvers.
 */
public class EHub implements EListener, Runnable {
    private static final Logger log = LoggerFactory.getLogger(EHub.class);

    private static int globalBest = 0;

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0 ; i < threadCount ; i++) {
           executor.submit(new EHub());
        }
        System.out.println("Started " + threadCount + " boards");
        executor.awaitTermination(100, TimeUnit.DAYS);
    }

    @Override
    public void run() {
        testSolver(WalkerG2R::new);
    }

    private void testSolver(Function<EBoard, Walker> walkerFactory) {
        testSolver(walkerFactory, true);
    }
    private void testSolver(Function<EBoard, Walker> walkerFactory, boolean clues) {
        EBoard board = getBoard(clues);
        Walker walker = walkerFactory.apply(board);
        Strategy strategy = new StrategyConservative(walker, this);
        StrategySolver solver = new StrategySolver(board, strategy);

        long runTime = -System.currentTimeMillis();
        solver.run();
        runTime += System.currentTimeMillis();
        System.out.printf("Done. Placed %d pieces in %.2f seconds\n", board.getFilledCount(), runTime/1000.0);
        try {
            Thread.sleep(1000000000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private EBoard getBoard() {
        return getBoard(true);
    }
    private EBoard getBoard(boolean clues) {
        EPieces pieces = EPieces.getEternii();
        EBoard board = new EBoard(pieces, 16, 16);
        board.registerFreePieces(pieces.getBag());
        if (clues) {
            pieces.processEterniiClues((x, y, piece, rotation) -> board.placePiece(x, y, piece, rotation, ""));
        }
//        new BoardVisualiser(board);
//        new BoardVisualiser(board, true);
        return board;
    }


    @Override
    public synchronized void localBest(String id, Strategy strategy, EBoard board, StrategySolverState state) {
        if (board.getFilledCount() <= globalBest) {
            return;
        }
        globalBest = board.getFilledCount();
        System.out.printf("%s (%dK attempts/sec): %d %s\n",
                          id, state.getTotalAttemptsPerMS(), board.getFilledCount(), board.getDisplayURL());
    }
}
