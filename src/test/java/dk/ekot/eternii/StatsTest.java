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
 Possible hex bottom left corners:  32,476,466
 Possible hex bottom right corners: 29,493,137

 Hex center not finished: Complete solutions so far: 1,354,267,040 (a night's work)
 5x5 TL corner:       Complete solutions so far: 1,442,343,927 (~20 hours)
                      Complete solutions so far: 5,524,240,878 (3 days)
6x6 TL Complete solutions so far: 13,587,510,981 (3-4 days? A week?)
7x7 TL Complete solutions so far: 12,171,379,454 (3-4 days? A week?)

 All permutations of the corner @ clue 1: Clue_1 * Corners * Edge * Edge = 4112 * 1301 * 62807 * 62807 = 21*10^15
 */
public class StatsTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(StatsTest.class);

    public void testQuadCorners() {
        System.out.println("Possible quad corners: " + HexCorners.countSolutions(WalkerQuadCorner::new));
    }

/*    public void testInner() {
        System.out.println("Possible quad inners: " + countSolutions(WalkerQuadInner::new));
    }*/

    public void testInner() {
        System.out.println("Possible quad inners: " + HexCorners.countSolutions(board -> new WalkerQuadSelected(
                        board, new int[][]{{5, 2}, {6, 2}, {5, 3}, {6, 3}}), 4));
    }

    public void testEdge() {
        System.out.println("Possible quad edge: " + HexCorners.countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{{0, 5}, {1, 5}, {0, 6}, {1, 6}}), 4));
    }

    public void testEdge3x3() {
        System.out.println("Possible 3x3 edge: " + HexCorners.countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{{0, 5}, {1, 5}, {2, 5}, {0, 6}, {1, 6}, {2, 6}, {0, 7}, {1, 7}, {2, 7}}), 9));
    }

    public void test3x3() {
        System.out.println("Possible 3x3 corners: " + HexCorners.countSolutions(board -> new WalkerRectangle(
                board, new Rect(0, 0, 2, 2)), 9));
        System.out.println("Possible 3x3 edges: " + HexCorners.countSolutions(board -> new WalkerRectangle(
                board, new Rect(1, 0, 3, 2)), 9));
        System.out.println("Possible 3x3 inner: " + HexCorners.countSolutions(board -> new WalkerRectangle(
                board, new Rect(5, 5, 7, 7)), 9));
    }

    public void testCenterClue3x3() {
        System.out.println("Possible 3x3 center clues: " + HexCorners.countSolutions(board -> new WalkerRectangle(
                board, new Rect(6, 7, 8, 9)), 9));
    }

    public void testEdgeHex() {
        System.out.println("Possible hex top edges: " + HexCorners.countSolutions(board -> new WalkerRectangle(
                board, new Rect(4, 0, 7, 3)), 16));
    }

    public void testCornerHexTL5x5() {
        System.out.println("Possible 5x5 top left corners: " + HexCorners.countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{
                {0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0},
                {0, 1}, {1, 1}, {2, 1}, {3, 1}, {4, 1},
                {0, 2}, {1, 2}, {2, 2}, {3, 2}, {4, 2},
                {0, 3}, {1, 3}, {2, 3}, {3, 3}, {4, 3},
                {0, 4}, {1, 4}, {2, 4}, {3, 4}, {4, 4},
                }), 24)); // There's already a clue piece
    }

    public void testCornerHexTL6x6() {
        System.out.println("Possible 6x6 top left corners: " + HexCorners.countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{
                {0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}, {5, 0},
                {0, 1}, {1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1},
                {0, 2}, {1, 2}, {2, 2}, {3, 2}, {4, 2}, {5, 2},
                {0, 3}, {1, 3}, {2, 3}, {3, 3}, {4, 3}, {5, 3},
                {0, 4}, {1, 4}, {2, 4}, {3, 4}, {4, 4}, {5, 4},
                {0, 5}, {1, 5}, {2, 5}, {3, 5}, {4, 5}, {5, 5},
                }), 35)); // There's already a clue piece
    }

    public void testCornerHexTL7x7() {
        System.out.println("Possible 7x7 top left corners: " + HexCorners.countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{
                {0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}, {5, 0}, {6, 0},
                {0, 1}, {1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1}, {6, 1},
                {0, 2}, {1, 2}, {2, 2}, {3, 2}, {4, 2}, {5, 2}, {6, 2},
                {0, 3}, {1, 3}, {2, 3}, {3, 3}, {4, 3}, {5, 3}, {6, 3},
                {0, 4}, {1, 4}, {2, 4}, {3, 4}, {4, 4}, {5, 4}, {6, 4},
                {0, 5}, {1, 5}, {2, 5}, {3, 5}, {4, 5}, {5, 5}, {6, 5},
                {0, 6}, {1, 6}, {2, 6}, {3, 6}, {4, 6}, {5, 6}, {6, 6},
                }), 48)); // There's already a clue piece
    }

    public void testCornerHexTL() {
        System.out.println("Possible hex top left corners: " + HexCorners.countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{
                {0, 0}, {1, 0}, {2, 0}, {3, 0},
                {0, 1}, {1, 1}, {2, 1}, {3, 1},
                {0, 2}, {1, 2}, {2, 2}, {3, 2},
                {0, 3}, {1, 3}, {2, 3}, {3, 3}
                }), 15)); // There's already a clue piece
    }

    // TODO: Make a dedicated walker that only visits the given fields in the given order and start from the corner or clue piece
    public void testCornerHexTR() {
        System.out.println("Possible hex top right corners: " + HexCorners.countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{
                {12, 0}, {13, 0}, {14, 0}, {15, 0},
                {12, 1}, {13, 1}, {14, 1}, {15, 1},
                {12, 2}, {13, 2}, {14, 2}, {15, 2},
                {12, 3}, {13, 3}, {14, 3}, {15, 3}
                }), 15)); // There's already a clue piece
    }

    public void testCornerHexBL() {
        System.out.println("Possible hex bottom left corners: " + HexCorners.countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{
                {0, 12}, {1, 12}, {2, 12}, {3, 12},
                {0, 13}, {1, 13}, {2, 13}, {3, 13},
                {0, 14}, {1, 14}, {2, 14}, {3, 14},
                {0, 15}, {1, 15}, {2, 15}, {3, 15}
                }), 15)); // There's already a clue piece
    }

    public void testCornerHexBR() {
        System.out.println("Possible hex bottom right corners: " + HexCorners.countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{
                {12, 12}, {13, 12}, {14, 12}, {15, 12},
                {12, 13}, {13, 13}, {14, 13}, {15, 13},
                {12, 14}, {13, 14}, {14, 14}, {15, 14},
                {12, 15}, {13, 15}, {14, 15}, {15, 15}
                }), 15)); // There's already a clue piece
    }

    public void testCornerHexC() { // (7, 8)
        System.out.println("Possible hex center clue: " + HexCorners.countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{
                {4,  8}, {5,  8}, {6,  8}, {7,  8},
                {4,  9}, {5,  9}, {6,  9}, {7,  9},
                {4, 10}, {5, 10}, {6, 10}, {7, 10},
                {4, 11}, {5, 11}, {6, 11}, {7, 11}
                }), 15)); // There's already a clue piece
    }

    // This includes invalids, such as grey edges on more than 2 sides
    public void testAll() {
        System.out.println("Possible quad all: " + HexCorners.countSolutions(WalkerQuadAll::new));
    }

    public void testClues() {
        System.out.println("Possible quad clue 1: " + HexCorners.countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{{3, 2}, {2, 3}, {3, 3}}), 3));
        System.out.println("Possible quad clue 2: " + HexCorners.countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{{12, 2}, {12, 3}, {13, 3}}), 3));
        System.out.println("Possible quad clue 3: " + HexCorners.countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{{3, 12}, {2, 13}, {3, 13}}), 3));
        System.out.println("Possible quad clue 4: " + HexCorners.countSolutions(board -> new WalkerQuadSelected(
                board, new int[][]{{12, 12}, {12, 13}, {13, 13}}), 3));
        System.out.println("Possible quad clue center: " + HexCorners.countSolutions(board -> new WalkerQuadSelected( // (7, 8)
                                                                                                                      board, new int[][]{{6, 8}, {6, 9}, {7, 9}}), 3));
    }

    private static class WalkerQuadCorner extends WalkerImpl {
        public WalkerQuadCorner(EBoard board) {
            super(board);
        }

        @Override
        protected Comparator<Move> getMoveComparator() {
            return Comparator.<Move>
                            comparingInt(move -> move.getX() < 2 && move.getY() < 2 ? 0 : 1)
                    .thenComparingInt(Move::validPiecesSize);
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
        protected Comparator<Move> getMoveComparator() {
            return Comparator.<Move>
                            comparingInt(move -> move.getX() > 0 && move.getX() < 3 &&
                                                 move.getY() > 0 && move.getY() < 3 ? 0 : 1)
                    .thenComparingInt(Move::validPiecesSize);
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
        protected Comparator<Move> getMoveComparator() {
            return Comparator.<Move>
                            comparingInt(move -> isValid(move.getX(), move.getY()) ? 0 : 1)
                    .thenComparingInt(Move::validPiecesSize)
                    .thenComparingInt(move -> 4-move.leastSetOuterEdgesFirst()); // Least free edges
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
        public EBoard.Pair<EBoard.Field, List<EBoard.Piece>> getLegacy() {
            List<EBoard.Pair<EBoard.Field, Set<Integer>>> all = getFreeFieldsRaw();
            return all.stream()
                    .min(fieldComparator)
                    .map(this::toRotatedPieces)
                    .orElse(null);
        }

/*        private Function<EBoard.Pair<EBoard.Field, Set<Integer>>, EBoard.Pair<EBoard.Field, Set<Integer>>> getOnlyInner() {
            return pair -> new EBoard.Pair<>(pair.left, pair.right.stream()
                    .filter(piece -> pieces.getType(piece) != EPieces.INNER)
                    .collect(Collectors.toSet()));
        }*/

        @Override
        protected Comparator<Move> getMoveComparator() {
            return Comparator.<Move>
                            comparingInt(move -> move.getX() > 10 && move.getX() < 13 &&
                                                 move.getY() > 0 && move.getY() < 3 ? 0 : 1)
                    .thenComparingInt(Move::validPiecesSize);
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
