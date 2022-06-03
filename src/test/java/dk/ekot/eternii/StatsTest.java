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

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Calculate stats for quad (2x2) pieces and similar
 *
 * All of these contains invalids (edges on the inner board)
 * 
 * Corners: 1301
 * Edge:   62807
 * All:   815652
 * Clue 1:  4112
 * Clue 2:  4108
 * Clue 3:  3599
 * Clue 4:  4347
 * Clue c:  3808
 *
 * All permutations of the corner @ clue 1: Clue_1 * Corners * Edge * Edge = 4112 * 1301 * 62807 * 62807 = 21*10^15
 */
public class StatsTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(StatsTest.class);

    public void testQuadCorners() {
        System.out.println("Possible quad corners: " + countSolutions(WalkerQuadCorner::new));
    }

    public void testInner() {
        System.out.println("Possible quad inners: " + countSolutions(WalkerQuadInner::new));
    }

    public void testEdge() {
        System.out.println("Possible quad edge: " + countSolutions(board -> new WalkerQuadClue(
                board, new int[][]{{0, 5}, {1, 5}, {0, 6}, {1, 6}}), 4));
    }

    // This includes invalids, such as grey edges on more than 2 sides
    public void testAll() {
        System.out.println("Possible quad all: " + countSolutions(WalkerQuadAll::new));
    }

    public void testClues() {
        System.out.println("Possible quad clue 1: " + countSolutions(board -> new WalkerQuadClue(
                board, new int[][]{{3, 2}, {2, 3}, {3, 3}}), 3));
        System.out.println("Possible quad clue 2: " + countSolutions(board -> new WalkerQuadClue(
                board, new int[][]{{12, 2}, {12, 3}, {13, 3}}), 3));
        System.out.println("Possible quad clue 3: " + countSolutions(board -> new WalkerQuadClue(
                board, new int[][]{{3, 12}, {2, 13}, {3, 13}}), 3));
        System.out.println("Possible quad clue 4: " + countSolutions(board -> new WalkerQuadClue(
                board, new int[][]{{12, 12}, {12, 13}, {13, 13}}), 3));
        System.out.println("Possible quad clue center: " + countSolutions(board -> new WalkerQuadClue( // (7, 8)
                board, new int[][]{{6, 8}, {6, 9}, {7, 9}}), 3));
    }

    private long countSolutions(Function<EBoard, Walker> walkerFactory) {
        return countSolutions(walkerFactory, 4);
    }
    private long countSolutions(Function<EBoard, Walker> walkerFactory, int max) {
        EPieces pieces = EPieces.getEternii();
        EBoard board = new EBoard(pieces, 16, 16);
        board.registerFreePieces(pieces.getBag());
        pieces.processEterniiClues((x, y, piece, rotation) -> board.placePiece(x, y, piece, rotation, ""));
        Walker walker = walkerFactory.apply(board);

        //new BoardVisualiser(board);

        BacktrackReturnOnBothSolver solver = new BacktrackReturnOnBothSolver(board, walker, max);
        solver.run();
        return solver.getFoundSolutions();
    }

    private static class WalkerQuadCorner extends WalkerImpl {
        public WalkerQuadCorner(EBoard board) {
            super(board);
        }

        @Override
        protected Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparator() {
            return Comparator.<EBoard.Pair<EBoard.Field, ? extends Collection<?>>>
                            comparingInt(pair -> pair.left.getX() < 2 && pair.left.getY() < 2 ? 0 : 1)
                    .thenComparingInt(this::validPieces);
        }
    }

    private static class WalkerQuadAll extends WalkerImpl {
        public WalkerQuadAll(EBoard board) {
            super(board);
        }

        @Override
        protected Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparator() {
            return Comparator.<EBoard.Pair<EBoard.Field, ? extends Collection<?>>>
                            comparingInt(pair -> pair.left.getX() > 0 && pair.left.getX() < 3 &&
                                                 pair.left.getY() > 0 && pair.left.getY() < 3 ? 0 : 1)
                    .thenComparingInt(this::validPieces);
        }
    }

    private static class WalkerQuadClue extends WalkerImpl {
        private final int[][] valids;

        public WalkerQuadClue(EBoard board, int[][] valids) {
            super(board);
            this.valids = valids;
        }

        @Override
        protected Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparator() {
            return Comparator.<EBoard.Pair<EBoard.Field, ? extends Collection<?>>>
                            comparingInt(pair -> isValid(pair.left.getX(), pair.left.getY()) ? 0 : 1)
                    .thenComparingInt(this::validPieces);
        }

        private boolean isValid(int x, int y) {
            for (int[] valid: valids) {
                if (valid[0] == x && valid[1] == y) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class WalkerQuadInner extends WalkerImpl {
        private final Function<EBoard.Pair<EBoard.Field, Set<Integer>>, EBoard.Pair<EBoard.Field, Set<Integer>>>
                onlyInner = getOnlyInner();
        public WalkerQuadInner(EBoard board) {
            super(board);
        }

        @Override
        public EBoard.Pair<EBoard.Field, List<EBoard.Piece>> get() {
            List<EBoard.Pair<EBoard.Field, Set<Integer>>> all = getFreeRaw();
            return all.stream()
                    .map(onlyInner)
                    .filter(pair -> !pair.right.isEmpty())
                    .min(comparator)
                    .map(this::toPieces)
                    .orElse(null);
        }

        private Function<EBoard.Pair<EBoard.Field, Set<Integer>>, EBoard.Pair<EBoard.Field, Set<Integer>>> getOnlyInner() {
            return pair -> new EBoard.Pair<>(pair.left, pair.right.stream()
                    .filter(piece -> pieces.getType(piece) != EPieces.INNER)
                    .collect(Collectors.toSet()));
        }

        @Override
        protected Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparator() {
            return Comparator.<EBoard.Pair<EBoard.Field, ? extends Collection<?>>>
                            comparingInt(pair -> pair.left.getX() > 10 && pair.left.getX() < 13 &&
                                                 pair.left.getY() > 0 && pair.left.getY() < 3 ? 0 : 1)
                    .thenComparingInt(this::validPieces);
        }
    }
}
