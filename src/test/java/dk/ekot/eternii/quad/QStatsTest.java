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

import dk.ekot.eternii.BoardVisualiser;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

/*

 NW corner hex:
 09:31:07.293 [main] DEBUG dk.ekot.eternii.quad.QSolverBacktrack - Got solution #34,282,001 from depth=4: https://e2.bucas.name/#puzzle=TokeEskildsen&board_w=16&board_h=16&board_edges=adcaafwdafqfafufaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaacvbawtwvqqwtuiwqaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaabpcawnspwlmnwollaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaacnfasnnnmpjnloupaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
 From Statstest: Possible hex top left corners:     31,738,684

 */
public class QStatsTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(QStatsTest.class);


    // 27.8K/sec, 44M unfinished
    public void testBorder2x4() {
        QuadBag.disabledMaps = new HashSet<>(
                Arrays.asList(0b1111,
                              0b1110, 0b1101, 0b1011, 0b0111,
                              0b1100, 0b0110, 0b0011, 0b1001));
        QBoard board = new QBoard();
        BoardVisualiser visualiser = new BoardVisualiser(board.getEboard(), BoardVisualiser.TYPE.live, "Experimental");
        visualiser.setOverflow(true);
        board.registerObserver(visualiser);

        QWalker walker = new QWalkerImpl(
                board,
                Comparator.comparingInt(QWalker.identity()). // For easier experiments below
                        thenComparingInt(QWalker.fixedOrder(new int[][] {
                        {2, 0},
                        {2, 1}
                })));

        QuadDelivery quadDelivery = QuadDelivery.IDENTITY;

        QSolverBacktrack solver = new QSolverBacktrack(
                board, walker, quadDelivery, QSolverBacktrack.getSolutionPrinter(1000), 2, Long.MAX_VALUE);
        QSolverTest.runSolver(solver);
    }


    public void testCornerNWHex() {
        QuadBag.disabledMaps = new HashSet<>(
                Arrays.asList(0b1111, 0b1110, 0b1101, 0b1011, 0b0111));
        QBoard board = new QBoard();
        BoardVisualiser visualiser = new BoardVisualiser(board.getEboard(), BoardVisualiser.TYPE.live, "Experimental");
        visualiser.setOverflow(true);
        board.registerObserver(visualiser);

        QWalker walker = new QWalkerImpl(
                board,
                Comparator.comparingInt(QWalker.identity()). // For easier experiments below
                        thenComparingInt(QWalker.quadCornersClockwise()));

        QuadDelivery quadDelivery = QuadDelivery.IDENTITY;

        QSolverBacktrack solver = new QSolverBacktrack(
                board, walker, quadDelivery, QSolverBacktrack.getSolutionPrinter(1000), 4, Long.MAX_VALUE);
        QSolverTest.runSolver(solver);
    }

}
