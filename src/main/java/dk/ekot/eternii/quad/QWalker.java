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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 *
 */
public interface QWalker {
    int BOARD_SIDE = 8;


    QBoard getBoard();

    /**
     * @return all possible {@link Move}s in order of priority.
     */
    default Stream<Move> getMoves() {
        List<Move> moves = new ArrayList<>(64);
        getBoard().visitAllFields(field -> {
            if (field.isFree()) {
                moves.add(new Move(getBoard(), field.getX(), field.getY()));
            }
        });
        moves.sort(getMoveComparator());
        return moves.stream();
    }

    default Move getMove() {
        return getMoves().findFirst().orElse(null); // Null should never happen so we accept Exceptions there
    }

    Comparator<QWalker.Move> getMoveComparator();

    /**
     * The number of free fields surrounding the move field.
     * <br/>
     * Fields outside of the board are counted as not free (they lock the inner edge).
     * @return amount of free neighbor fields for the move.
     */
    static ToIntFunction<Move> freeNeighbourFields() {
        return move -> (int) Arrays.stream(RELATIVE_NEIGHBOURS).
                map(rel -> move.getField().getBoard().
                        getOptionalField(move.getX()+rel[0], move.getY()+rel[1])).
                filter(Optional::isPresent).
                map(Optional::get).
                map(QField::isFree).
                count();
    }
    int[][] RELATIVE_NEIGHBOURS = new int[][]{{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    /**
     * @return 0 if corner (only the 4 single field corners are considered), else 1.
     */
    static ToIntFunction<Move> corners() {
        return move -> move.isCorner() ? 0 : 1;
    }

    /**
     * Warning: Heavy.
     * @return fewest valid quads for any neighbour for any of the valid quads for the move.
     */
    static ToIntFunction<Move> fewestNeighbourQuads() {
        return move -> move.getNeighbourCompounds().
                mapToInt(compound -> (int) (compound >>> 32)).
                findFirst().orElseThrow(() -> new IllegalStateException("No quads available"));
    }

    /**
     * Conceptually places the quad with {@code quadID} on the {@code field}, then checks the free neighbour fields
     * and return the minimum amount of available quads for those.
     * @return minimum amount of available quads neighbour fields if the quad was placed on the field.
     */
    static long fewestNeighbourQuads(QField field, int quadID) {
        int min = Integer.MAX_VALUE;
        min = Math.min(min, field.getY() == 0 ? Integer.MAX_VALUE : validQuadCountNorthOf(field, quadID));
        min = Math.min(min, field.getX() == 7 ? Integer.MAX_VALUE : validQuadCountEastOf(field, quadID));
        min = Math.min(min, field.getY() == 7 ? Integer.MAX_VALUE : validQuadCountSouthOf(field, quadID));
        min = Math.min(min, field.getX() == 0 ? Integer.MAX_VALUE : validQuadCountWestOf(field, quadID));

        if (min == Integer.MAX_VALUE) {
            System.out.println("Got zero fewest for " + field + " and quadID " + quadID);
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    /**
     * @return the number of valid quads if the field to the north had the quad with {@code quadID} set.
     */
    static int validQuadCountSouthOf(QField field, int quadID) {
        QBoard board = field.getBoard();
        final int x = field.getX();
        final int y = field.getY()+1;
        QuadEdgeMap edgeMap = field.getBag().getQuadEdgeMap(
                true,
                x < 7 && !board.getField(x+1, y).isFree(),
                y < 7 && !board.getField(x, y+1).isFree(),
                x > 0 && !board.getField(x-1, y).isFree());
        long edgeHash = QBits.getHash(
                QBits.getColS(field.getBag().getQEdges(quadID)),
                x == 7 ? -1 : board.getField(x+1, y).getEdgeIfDefinedW(),
                y == 7 ? -1 : board.getField(x, y+1).getEdgeIfDefinedN(),
                x == 0 ? -1 : board.getField(x-1, y).getEdgeIfDefinedE(),
                false);
        return edgeMap.available(edgeHash);
    }
    /**
     * @return the number of valid quads if the field to the east had the quad with {@code quadID} set.
     */
    static int validQuadCountWestOf(QField field, int quadID) {
        QBoard board = field.getBoard();
        final int x = field.getX()-1;
        final int y = field.getY();
        QuadEdgeMap edgeMap = field.getBag().getQuadEdgeMap(
                y > 0 && !board.getField(x, y-1).isFree(),
                true,
                y < 7 && !board.getField(x, y+1).isFree(),
                x > 0 && !board.getField(x-1, y).isFree());
        long edgeHash = QBits.getHash(
                y == 0 ? -1 : board.getField(x, y-1).getEdgeIfDefinedS(),
                QBits.getColW(field.getBag().getQEdges(quadID)),
                y == 7 ? -1 : board.getField(x, y+1).getEdgeIfDefinedN(),
                x == 0 ? -1 : board.getField(x-1, y).getEdgeIfDefinedE(),
                false);
        return edgeMap.available(edgeHash);
    }
    /**
     * @return the number of valid quads if the field to the south had the quad with {@code quadID} set.
     */
    static int validQuadCountNorthOf(QField field, int quadID) {
        QBoard board = field.getBoard();
        final int x = field.getX();
        final int y = field.getY()-1;
        QuadEdgeMap edgeMap = field.getBag().getQuadEdgeMap(
                y > 0 && !board.getField(x, y-1).isFree(),
                x < 7 && !board.getField(x+1, y).isFree(),
                true,
                x > 0 && !board.getField(x-1, y).isFree());
        long edgeHash = QBits.getHash(
                y == 0 ? -1 : board.getField(x, y-1).getEdgeIfDefinedS(),
                x == 7 ? -1 : board.getField(x+1, y).getEdgeIfDefinedW(),
                QBits.getColN(field.getBag().getQEdges(quadID)),
                x == 0 ? -1 : board.getField(x-1, y).getEdgeIfDefinedE(),
                false);
        return edgeMap.available(edgeHash);
    }
    /**
     * @return the number of valid quads if the field to the west had the quad with {@code quadID} set.
     */
    static int validQuadCountEastOf(QField field, int quadID) {
        QBoard board = field.getBoard();
        final int x = field.getX()+1;
        final int y = field.getY();
        QuadEdgeMap edgeMap = field.getBag().getQuadEdgeMap(
                y > 0 && !board.getField(x, y-1).isFree(),
                x < 7 && !board.getField(x+1, y).isFree(),
                y < 7 && !board.getField(x, y+1).isFree(),
                true);
        long edgeHash = QBits.getHash(
                y == 0 ? -1 : board.getField(x, y-1).getEdgeIfDefinedS(),
                x == 7 ? -1 : board.getField(x+1, y).getEdgeIfDefinedW(),
                y == 7 ? -1 : board.getField(x, y+1).getEdgeIfDefinedN(),
                QBits.getColE(field.getBag().getQEdges(quadID)),
                false);
        return edgeMap.available(edgeHash);
    }


    /**
     * Constant mapping to 0 to avoid changing the order when used for comparison.
     * @return 0.
     */
    static ToIntFunction<Move> identity() {
        return move -> 0;
    }

    /**
     * @return is the given move is a border then {@code inner} is called, else {@code Integer.MAX_VALUE} is returned.
     */
    static ToIntFunction<Move> isBorderOrCorner(ToIntFunction<Move> inner) {
        return move -> move.isBorderOrCorner() ? inner.applyAsInt(move) : Integer.MAX_VALUE;
    }

    /**
     * @return nearest to top-left corner, measured row by row.
     */
    static ToIntFunction<Move> topLeft() {
        return move -> move.getY() * BOARD_SIDE + move.getX();
    }

    /**
     * @return lowest maximum number of quads, where quads are all potential quads from the field on a blank board
     */
    static ToIntFunction<Move> minMaxAvailable() {
        return move -> move.getField().getMaxAvailable();
    }

    /**
     * @return lowest number of available valid quads if the field is on the border of the board, else Integer.MAX_VALUE
     */
    static ToIntFunction<Move> bordersByAvailable() {
        return move -> move.isBorderOrCorner() ? move.getField().available() : Integer.MAX_VALUE;
    }

    /**
     * @return true count of available quads. Can be very heavy.
     */
    static ToIntFunction<Move> available() {
        return move -> move.getField().available();
    }

    /**
     * @return borders first, no special order.
     */
    static ToIntFunction<Move> borders() {
        return move -> move.getX() == 0 || move.getX() == 7 || move.getY() == 0 || move.getY() == 7 ? 1 : 2;
    }

    /**
     * @return the border to the border (aka inner "ring)), no special order.
     */
    static ToIntFunction<Move> borderBorders() {
        return move -> move.getX() == 1 || move.getX() == 6 || move.getY() == 1 || move.getY() == 6 ? 1 : 2;
    }

    /**
     * Prioritizes the given coordinates over others and order candidates by the order in coordinates.
     * @param coordinates array of {@code {x, y}}.
     */
    static ToIntFunction<Move> fixedOrder(int[][] coordinates) {
        int[] indices = xysToIndices(coordinates);
        final int[] all = new int[64];
        Arrays.fill(all, 65);
        for (int priority = 0 ; priority < indices.length ; priority++) {
            all[indices[priority]] = priority;
        }
        return move -> all[move.getY()*8+move.getX()];
    }

    /**
     * Prioritizes the given coordinates over others and order candidates by the order in coordinates.
     * @param coordinateGroups array of {@code {{x, y}} where the outer array is group.
     */
    static ToIntFunction<? super Move> fixedOrderGroup(int[][][] coordinateGroups) {
        final int[] all = new int[64];
        Arrays.fill(all, 65);
        for (int group = 0 ; group < coordinateGroups.length ; group++){
            int[][] coordinates = coordinateGroups[0];
            for (int index : xysToIndices(coordinates)) {
                all[index] = group;
            }
        }
        return move -> all[move.getY()*8+move.getX()];
    }

    static int[] xysToIndices(int[][] coordinates) {
        return Arrays.stream(coordinates).
                map(coordinate -> coordinate[1]*8+coordinate[0]).
                mapToInt(Integer::valueOf).
                toArray();
    }


    /**
     * @return 0 if in a quad corner, else 1.
     */
    static ToIntFunction<? super Move> quadCorners() {
        return fixedOrderGroup(new int[][][] {
                { // All in the same group
                        {0, 0}, // NW
                        {1, 0},
                        {1, 1},
                        {0, 1},

                        {7, 0}, // NE
                        {7, 1},
                        {6, 1},
                        {6, 0},

                        {7, 7}, // SE
                        {6, 7},
                        {6, 6},
                        {7, 6},

                        {0, 7}, // SW
                        {0, 6},
                        {1, 6},
                        {1, 7},
                }
        });
    }

    /**
     * @return corners (2x2 quads) in order NW, NE, SE, SW, each corner ordered corner->middle.
     */
    static ToIntFunction<Move> quadCornersClockwise() {
        return fixedOrder(new int[][] {
                {0, 0}, // NW
                {1, 0},
                {1, 1},
                {0, 1},

                {7, 0}, // NE
                {7, 1},
                {6, 1},
                {6, 0},

                {7, 7}, // SE
                {6, 7},
                {6, 6},
                {7, 6},

                {0, 7}, // SW
                {0, 6},
                {1, 6},
                {1, 7},
        });
    }

    class Move {
        final QBoard board;
        final int x;
        final int y;

        public Move(QBoard board, int x, int y) {
            this.board = board;
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public QField getField() {
            return board.getField(x, y);
        }

        public IntStream getAvailableQuadIDs() {
            // TODO: Consider making this an option
            return getField().getAvailableQuadIDsNoCache();
        }

        public IntStream getAvailableQuadIDsByNeighbours() {
            return getNeighbourCompounds().
                    mapToInt(compound -> (int) compound); // Right 32 bits = quad ID
        }

        public boolean isBorderOrCorner() {
            return x == 0 || y == 0 || x == 7 || y== 7;
        }
        public boolean isCorner() {
            return (x == 0 || x == 7) && (y == 0 || y== 7);
        }
        public boolean isClueCorner() {
            return (x == 1 && y == 1) ||
                   (x == 6 && y == 1) ||
                   (x == 6 && y == 6) ||
                   (x == 1 && y == 6);
        }
        public boolean isClueCenter() {
            return x == 3 && y == 4;
        }
        public boolean isClue() {
            return isClueCorner() || isClueCenter();
        }

        @Override
        public String toString() {
            return "Move(" + x + ", " + y + ")";
        }

        /**
         * A compound is a long where the first 32 bits holds the minimum number of possible quads for all
         * neighbour fields and the last 32 bits holds a quad ID.
         * @return a stream of compounds, ordered by neighbours low to high.
         */
        public LongStream getNeighbourCompounds() {
            if (ncompounds == null) {
                ncompounds = getAvailableQuadIDs().
                        mapToLong(quadID -> (fewestNeighbourQuads(getField(), quadID) << 32) | quadID).
                        sorted().
                        toArray();
                //System.out.println("Created " + ncompounds.length + " for " + this);
            }
            return Arrays.stream(ncompounds);
        }
        private long[] ncompounds = null;

    }

}
