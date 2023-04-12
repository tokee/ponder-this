package dk.ekot.eternii;

import junit.framework.TestCase;

import java.util.Comparator;
import java.util.Random;
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

    public void testNullSolver() {
        testSolver(WalkerA::new, (board, walker) -> () -> System.out.println("Doing nothing"));
    }

    public void testOneWaySolver() {
        testSolver(WalkerF::new, OneWaySolver::new);
    }

    public void testOneWayAllPiecesSolver() {
        testSolver(WalkerF::new, OneWayAllPiecesSolver::new);
    }

    public void testOneWayAllFieldsAllPiecesSolver() {
        testSolver(WalkerF::new, OneWayAllFieldsAllPiecesSolver::new);
    }

    public void testBacktrackSolver() {
        testSolver(WalkerF::new, BacktrackSolver::new);
    }

    public void testBacktrackReturnSolver_A() {
        testSolver(WalkerA::new, BacktrackReturnOnOneSolver::new);
    }
    public void testBacktrackReturnSolver_F() {
        testSolver(WalkerF::new, BacktrackReturnOnOneSolver::new);
    }
    // No clues!
    public void testBacktrackReturnSolver_H() {
        testSolver(WalkerH_BT::new, BacktrackReturnOnOneSolver::new, false);
    }

    // ~58K/s laptop
    public void testBacktrackReturnBothSolver_F() {
        testSolver(WalkerF::new, BacktrackReturnOnBothSolver::new);
    }
    public void testExp() {
        testSolver(WalkerExp::new, BacktrackReturnOnBothSolver::new, true);
    }

    /* --------------------------------------------------------------------------------------------------------------- */

    public void testGeneric() {

        testSolver(board -> new WalkerGeneric(board, Comparator.
                           comparingInt(Walker.Move::identity).

                           //thenComparingInt(Walker.Move::topLeftFirst).
                           thenComparingInt(Walker.Move::clueCornersOrdered).
                           thenComparingInt(Walker.Move::boardEdgeFirst).
                           //thenComparingInt(Walker.onSpiralIn(board)).
                           thenComparingInt(Walker.Move::validPiecesSize).

                           thenComparingInt(Walker.Move::identity)
                   ),
                   (board, walker) -> new StrategySolverMove(
                           board,
                           new StrategyLeveledReset(walker, getTopListener(), 0.00001, 5, 500, 30),
                           new Random()),
                   true);
    }

    private Comparator<Walker.Move> getOutsideInComperator() {
        return Comparator.
                comparingInt(Walker.Move::clueCornersOrdered).
                thenComparingInt(Walker.Move.rectFirst(0, 0, 15, 2)).
                thenComparingInt(Walker.Move.rectFirst(0, 13, 15, 15)).
                thenComparingInt(Walker.Move.rectFirst(0, 3, 2, 12)).
                thenComparingInt(Walker.Move.rectFirst(13, 3, 15, 12)).
//                           thenComparingInt(Walker.Move::boardEdgeFirst).
        thenComparingInt(Walker.Move::validPiecesSize).
//                           thenComparingInt(Walker.Move::mostSetOuterEdgesFirst).
        thenComparingInt(Walker.Move::topLeftFirst);
    }

    /* --------------------------------------------------------------------------------------------------------------- */


    private EListener getTopListener() {
        return new EListener() {
            @Override
            public void localBest(String id, Strategy strategy, EBoard board, StrategySolverState state) {
                System.out.println(board.getFilledCount() + ": " + board.getDisplayURL());
            }
        };
    }

    public void testBacktrackReturnBothSolver_B() {
        testSolver(WalkerB::new, BacktrackReturnOnBothSolver::new);
    }

    public void testBacktrackReturnBothSolver_A() {
        testSolver(WalkerA::new, BacktrackReturnOnBothSolver::new);
    }

    public void testBacktrackReturnBothSolver_AFast() {
        testSolver(WalkerAFast::new, BacktrackReturnOnBothSolver::new);
    }

    public void testBacktrackReturnBothSolver_G() {
        testSolver(WalkerG::new, BacktrackReturnOnBothSolver::new);
    }
    // Attempts: 99225129K, free= 93, min= 46|163, att/sec=168K, possible=3e+65 best=https://e2.bucas.name/#puzzle=displayTest&board_w=16&board_h=16&board_edges=abdaafhbackfacocacpcabhcacrbadgcadgdaepdacqeaencadteabmdaeibaabedsdahvjskvwvomjvpjnmhhwjrphhgnkpgrinpnqrqgtnnnpgtmqnmrwmijjrbafjdhdajnthwlmnjmllniomwtgihjltkgrjigtgqmqgtlgmprilqunrwswujgisfafgdweatqqwmokqllwoaaaaaaaaaaaaaaaaaaaaqwlrgiiwisoinqoswsoqiujsfaduepbaqrqpkmnrwuhmaaaaaaaaaaaaaaaaaaaalmtpiommoiqoowuiovuwjnnvdaenbjfaqovjnpmohkrprvmkaaaaaaaarmorhmrmttkmmrgtqrtruhhruqlhnouqeabofqfavgtqmpqgrsjpmwksaaaaaaaaovntrnkvkounggkotnpghsknlrlsujtrbabjfufatmsuqhwmjumhksiuaaaaaaaanvijkokvuisoklqiplulkillliwitvvibacvfubasrtuwvlrmqlviigqaaaaaaaaijjmklqjsnhlqoqnuvgolgvvwwvgvvlwcafvbveatkuvlwoklwvwgsjwaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaafadoeseaujosovmjvijvjjliuoujaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaadadieteaopstmiwpjurilkvuuwokaaaaaaaaaaaavhununhhtkknsntknnsngwqndafweueashwuwkuhrwpkvwtwoiqwaaaaaaaaaaaaurhohmorkqhmtjoqskqjqtjkfabtelfawmulujvmpppjtwhpqlhwaaaaaaaaaaaahsjuowmshqkworiqqvprjgwvbabgfhcausthvpnsprophkprhvrkntqvtlqtsnpljminmupmkjhuistjpgmswstgbacscdaatcadnfacoeafpcaerfacqeafqbaepcabidacpcadhbacteabmfaetdafcaad

    public void testBacktrackReturnBothSolver_G2() {
        testSolver(WalkerG2::new, BacktrackReturnOnBothSolver::new);
    }

    public void testCornerclueFirst() {
        testSolver(WalkerGClueStart::new, BacktrackReturnOnBothSolver::new);
    }

    public void testCornerclue5() {
        testSolver(WalkerGCornerClue5::new, BacktrackReturnOnBothSolver::new);
    }

    public void testBacktrackReturnBothSolver_G2R() {
        testSolver(WalkerG2R::new, BacktrackReturnOnBothSolver::new);
    }

    // No clues!
    public void testBacktrackReturnBothSolver_H() {
        testSolver(WalkerH_BT::new, BacktrackReturnOnBothSolver::new, false);
    }

    public void testSpiralIn() {
        testSolver(WalkerSpiralIn::new, BacktrackReturnOnBothSolver::new, false);
    }

    private void testSolver(Function<EBoard, Walker> walkerFactory, BiFunction<EBoard, Walker, Runnable> solverFactory) {
        testSolver(walkerFactory, solverFactory, true);
    }
    private void testSolver(Function<EBoard, Walker> walkerFactory, BiFunction<EBoard, Walker, Runnable> solverFactory,
                            boolean clues) {
        EBoard board = getBoard(clues);
        Walker walker = walkerFactory.apply(board);
        Runnable solver = solverFactory.apply(board, walker);
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
            //pieces.processEterniiCornerClues((x, y, piece, rotation) -> board.placePiece(x, y, piece, rotation, ""));
        }
        new BoardVisualiser(board, BoardVisualiser.TYPE.live);
        new BoardVisualiser(board, BoardVisualiser.TYPE.best);
        new BoardVisualiser(board, BoardVisualiser.TYPE.best_unplaced);
        return board;
    }

}