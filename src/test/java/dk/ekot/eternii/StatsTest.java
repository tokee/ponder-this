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

/**
 * Calculate stats for quad (2x2) pieces and similar. With check for proper edge/no-ege
 *
 Possible quad corners: 1216
 Possible quad inners: 760799
 Possible quad edge: 62807
 Possible quad all: 3474
 Possible quad clue 1: 4112
 Possible quad clue 2: 4108
 Possible quad clue 3: 3599
 Possible quad clue 4: 4347
 Possible quad clue center: 3808

 Possible hex top left corners:     31,738,684
 Possible hex top right corners:    39,157,138
 Possible hex top right corners:    39,157,138
 Possible hex bottom left corners:  32,476,466
 Possible hex bottom right corners: 29,493,137

 Center not finished: Complete solutions so far: 1,354,267,040 (a night's work)
 5x5 TL corner:       Complete solutions so far: 1,442,343,927 (~20 hours)
                      Complete solutions so far: 5,524,240,878 (3 days)

 All permutations of the corner @ clue 1: Clue_1 * Corners * Edge * Edge = 4112 * 1301 * 62807 * 62807 = 21*10^15
 */
public class StatsTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(StatsTest.class);

    public void testQuadCorners() {
        System.out.println("Possible quad corners: " + countSolutions(WalkerQuadCorner::new));
    }

/*    public void testInner() {
        System.out.println("Possible quad inners: " + countSolutions(WalkerQuadInner::new));
    }*/

    public void testInner() {
        System.out.println("Possible quad inners: " + countSolutions(board -> new WalkerQuadSelected(
                        board, new int[][]{{5, 2}, {6, 2}, {5, 3}, {6, 3}}), 4));
    }

    public void testEdge() {
        System.out.println("Possible quad edge: " + countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{{0, 5}, {1, 5}, {0, 6}, {1, 6}}), 4));
    }

    public void testCornerHexTL() {
        System.out.println("Possible hex top left corners: " + countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{
                {0, 0}, {1, 0}, {2, 0}, {3, 0},
                {0, 1}, {1, 1}, {2, 1}, {3, 1},
                {0, 2}, {1, 2}, {2, 2}, {3, 2},
                {0, 3}, {1, 3}, {2, 3}, {3, 3}
                }), 15)); // There's already a clue piece
    }


    public void testCornerHexTL3x3() {
        System.out.println("Possible 5x5 top left corners: " + countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{
                {0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0},
                {0, 1}, {1, 1}, {2, 1}, {3, 1}, {4, 1},
                {0, 2}, {1, 2}, {2, 2}, {3, 2}, {4, 2},
                {0, 3}, {1, 3}, {2, 3}, {3, 3}, {4, 3},
                {0, 4}, {1, 4}, {2, 4}, {3, 4}, {4, 4},
                }), 24)); // There's already a clue piece
    }

    // TODO: Make a dedicated walker that only visits the given fields in the given order and start from the corner or clue piece
    public void testCornerHexTR() {
        System.out.println("Possible hex top right corners: " + countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{
                {12, 0}, {13, 0}, {14, 0}, {15, 0},
                {12, 1}, {13, 1}, {14, 1}, {15, 1},
                {12, 2}, {13, 2}, {14, 2}, {15, 2},
                {12, 3}, {13, 3}, {14, 3}, {15, 3}
                }), 15)); // There's already a clue piece
    }

    public void testCornerHexBL() {
        System.out.println("Possible hex bottom left corners: " + countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{
                {0, 12}, {1, 12}, {2, 12}, {3, 12},
                {0, 13}, {1, 13}, {2, 13}, {3, 13},
                {0, 14}, {1, 14}, {2, 14}, {3, 14},
                {0, 15}, {1, 15}, {2, 15}, {3, 15}
                }), 15)); // There's already a clue piece
    }

    public void testCornerHexBR() {
        System.out.println("Possible hex bottom right corners: " + countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{
                {12, 12}, {13, 12}, {14, 12}, {15, 12},
                {12, 13}, {13, 13}, {14, 13}, {15, 13},
                {12, 14}, {13, 14}, {14, 14}, {15, 14},
                {12, 15}, {13, 15}, {14, 15}, {15, 15}
                }), 15)); // There's already a clue piece
    }

    public void testCornerHexC() { // (7, 8)
        System.out.println("Possible hex center clue: " + countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{
                {4,  8}, {5,  8}, {6,  8}, {7,  8},
                {4,  9}, {5,  9}, {6,  9}, {7,  9},
                {4, 10}, {5, 10}, {6, 10}, {7, 10},
                {4, 11}, {5, 11}, {6, 11}, {7, 11}
                }), 15)); // There's already a clue piece
    }

    // This includes invalids, such as grey edges on more than 2 sides
    public void testAll() {
        System.out.println("Possible quad all: " + countSolutions(WalkerQuadAll::new));
    }

    public void testClues() {
        System.out.println("Possible quad clue 1: " + countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{{3, 2}, {2, 3}, {3, 3}}), 3));
        System.out.println("Possible quad clue 2: " + countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{{12, 2}, {12, 3}, {13, 3}}), 3));
        System.out.println("Possible quad clue 3: " + countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{{3, 12}, {2, 13}, {3, 13}}), 3));
        System.out.println("Possible quad clue 4: " + countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{{12, 12}, {12, 13}, {13, 13}}), 3));
        System.out.println("Possible quad clue center: " + countSolutions(board -> new WalkerQuadSelected( // (7, 8)
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

//        new BoardVisualiser(board);
//        new BoardVisualiser(board, true);

        StatsSolver solver = new StatsSolver(board, walker, max);
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

    private static class WalkerQuadSelected extends WalkerImpl {
        private final int[][] valids;

        public WalkerQuadSelected(EBoard board, int[][] valids) {
            super(board);
            this.valids = valids;
        }

        @Override
        protected Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparator() {
            return Comparator.<EBoard.Pair<EBoard.Field, ? extends Collection<?>>>
                            comparingInt(pair -> isValid(pair.left.getX(), pair.left.getY()) ? 0 : 1)
                    .thenComparingInt(this::validPieces)
                    .thenComparingInt(pair -> 4-pair.left.getOuterEdgeCount()); // Least free edges
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
       /* private final Function<EBoard.Pair<EBoard.Field, Set<Integer>>, EBoard.Pair<EBoard.Field, Set<Integer>>>
                onlyInner = getOnlyInner();*/
        public WalkerQuadInner(EBoard board) {
            super(board);
        }

        @Override
        public EBoard.Pair<EBoard.Field, List<EBoard.Piece>> get() {
            List<EBoard.Pair<EBoard.Field, Set<Integer>>> all = getFreeRaw();
            return all.stream()
                    .min(comparator)
                    .map(this::toPieces)
                    .orElse(null);
        }

/*        private Function<EBoard.Pair<EBoard.Field, Set<Integer>>, EBoard.Pair<EBoard.Field, Set<Integer>>> getOnlyInner() {
            return pair -> new EBoard.Pair<>(pair.left, pair.right.stream()
                    .filter(piece -> pieces.getType(piece) != EPieces.INNER)
                    .collect(Collectors.toSet()));
        }*/

        @Override
        protected Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparator() {
            return Comparator.<EBoard.Pair<EBoard.Field, ? extends Collection<?>>>
                            comparingInt(pair -> pair.left.getX() > 10 && pair.left.getX() < 13 &&
                                                 pair.left.getY() > 0 && pair.left.getY() < 3 ? 0 : 1)
                    .thenComparingInt(this::validPieces);
        }
    }
}
