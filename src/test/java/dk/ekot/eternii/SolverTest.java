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
        }
        new BoardVisualiser(board);
        new BoardVisualiser(board, true);
        return board;
    }

}