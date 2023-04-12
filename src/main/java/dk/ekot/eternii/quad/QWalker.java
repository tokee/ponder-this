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

import dk.ekot.eternii.EBoard;
import dk.ekot.eternii.Walker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
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
     * @return borders first, no special order.
     */
    static ToIntFunction<Move> borders() {
        return move -> move.getX() == 0 || move.getX() == 7 || move.getY() == 0 || move.getY() == 7 ? 1 : 2;
    }

    /**
     * @return the border to the border (aka inner "ring)), no special order.
     */
    static ToIntFunction<? super Move> borderBorders() {
        return move -> move.getX() == 1 || move.getX() == 6 || move.getY() == 1 || move.getY() == 6 ? 1 : 2;
    }

    /**
     * @return corners (2x2 quads) in order, each corner ordered corner->middle.
     */
    // TODO: Performance: Replace this with efficient array based priority
    static ToIntFunction<Move> cornersOrdered() {
        return move -> {
            final int x = move.getX();
            final int y = move.getY();
            if (x <= 1 && y <= 1) { // NW
                if (x == 0 && y == 0) {
                    return 10;
                }
                if (x == 1) {
                    return y == 0 ? 11 : 12;
                }
                return 13;
            }
            if (x >= 6 && y <= 1) { // NE
                if (x == 7 && y == 0) {
                    return 20;
                }
                if (y == 1) {
                    return x == 7 ? 21 : 22;
                }
                return 23;
            }
            if (x >= 6 && y >= 6) { // SE
                if (x == 7 && y == 7) {
                    return 30;
                }
                if (x == 6) {
                    return y == 7 ? 31 : 32;
                }
                return 33;
            }
            if (x <= 1 && y >= 6) { // SW
                if (x == 0 && y == 7) {
                    return 40;
                }
                if (y == 6) {
                    return x == 0 ? 41 : 42;
                }
                return 43;
            }
            return 50;
        };
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
            return getField().getAvailableQuadIDs();
        }
    }

}
