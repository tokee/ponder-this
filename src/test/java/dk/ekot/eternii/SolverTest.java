package dk.ekot.eternii;

import junit.framework.TestCase;

import java.util.function.BiFunction;
import java.util.function.Function;

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
public class SolverTest extends TestCase {

    public void testNullSolver() throws InterruptedException {
        testSolver(WalkerA::new, (board, walker) -> () -> System.out.println("Doing nothing"));
    }

    public void testOneWaySolver() throws InterruptedException {
        testSolver(WalkerF::new, OneWaySolver::new);
    }

    public void testOneWayAllPiecesSolver() throws InterruptedException {
        testSolver(WalkerF::new, OneWayAllPiecesSolver::new);
    }

    public void testOneWayAllFieldsAllPiecesSolver() throws InterruptedException {
        testSolver(WalkerF::new, OneWayAllFieldsAllPiecesSolver::new);
    }

    public void testBacktrackSolver() throws InterruptedException {
        testSolver(WalkerF::new, BacktrackSolver::new);
    }

    public void testBacktrackReturnSolver() throws InterruptedException {
        testSolver(WalkerF::new, BacktrackReturnOnOneSolver::new);
    }

    public void testBacktrackReturnBothSolver_F() throws InterruptedException {
        testSolver(WalkerF::new, BacktrackReturnOnBothSolver::new);
    }

    public void testBacktrackReturnBothSolver_B() throws InterruptedException {
        testSolver(WalkerB::new, BacktrackReturnOnBothSolver::new);
    }

    public void testBacktrackReturnBothSolver_A() throws InterruptedException {
        testSolver(WalkerA::new, BacktrackReturnOnBothSolver::new);
    }

    public void testBacktrackReturnBothSolver_ATest() throws InterruptedException {
        testSolver(WalkerAFast::new, BacktrackReturnOnBothSolver::new);
    }

    public void testBacktrackReturnBothSolver_G() throws InterruptedException {
        testSolver(WalkerG::new, BacktrackReturnOnBothSolver::new);
    }


    private void testSolver(Function<EBoard, Walker> walkerFactory, BiFunction<EBoard, Walker, Runnable> solverFactory)
            throws InterruptedException {
        EBoard board = getBoard();
        Walker walker = walkerFactory.apply(board);
        Runnable solver = solverFactory.apply(board, walker);
        long runTime = -System.currentTimeMillis();
        solver.run();
        runTime += System.currentTimeMillis();
        System.out.printf("Done. Placed %d pieces in %.2f seconds\n", board.getFilledCount(), runTime/1000.0);
        Thread.sleep(1000000L);
    }

    private EBoard getBoard() {
        EPieces pieces = EPieces.getEternii();
        EBoard board = new EBoard(pieces, 16, 16);
        board.registerFreePieces(pieces.getBag());
        pieces.processEterniiClues((x, y, piece, rotation) -> board.placePiece(x, y, piece, rotation, ""));
        new BoardVisualiser(board);
        new BoardVisualiser(board, true);
        return board;
    }

}