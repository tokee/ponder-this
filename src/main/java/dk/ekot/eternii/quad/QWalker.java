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
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
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
        return move -> move.isBorder() ? move.getField().available() : Integer.MAX_VALUE;
    }

    /**
     * @return lowest number of available valid quads if the field is on the border of the board or is a corner clue,
     *         else Integer.MAX_VALUE
     */
    static ToIntFunction<Move> bordersOrClueCornerSubAvailable() {
        return move -> move.isBorder() || move.isClueCorner() ? move.getField().available() : Integer.MAX_VALUE;
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

    static int[] xysToIndices(int[][] coordinates) {
        return Arrays.stream(coordinates).
                map(coordinate -> coordinate[1]*8+coordinate[0]).
                mapToInt(Integer::valueOf).
                toArray();
    }

    /**
     * @return corners (2x2 quads) in order NW, NE, SE, SW, each corner ordered corner->middle.
     */
    static ToIntFunction<Move> cornersClockwise() {
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

        public boolean isBorder() {
            return x == 0 || y == 0 || x == 7 || y== 7;
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
    }

}
