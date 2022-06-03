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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * Provides next Field wih corresponding Pieces to try.
 */
public interface Walker extends Supplier<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> {

    EBoard getBoard();

    default EBoard.Pair<EBoard.Field, List<EBoard.Piece>> toPieces(EBoard.Pair<EBoard.Field, Set<Integer>> pair) {
        EBoard.Field field = pair.left;
        List<EBoard.Piece> pieces = pair.right.stream()
                .map(p -> new EBoard.Piece(p, field.getValidRotation(p)))
                .collect(Collectors.toList());
        return new EBoard.Pair<>(field, pieces);
    }

    /**
     * @return All free fields, with corresponding valid pieces (not rotated), not sorted.
     */
    default List<EBoard.Pair<EBoard.Field, Set<Integer>>> getFreeRaw() {
        List<EBoard.Pair<EBoard.Field, Set<Integer>>> fields = new ArrayList<>(getBoard().getWidth() * getBoard().getHeight());
        getBoard().visitAll((x, y) -> {
            final long state = getBoard().getState(x, y);
            if (EBits.hasPiece(state)) {
                return;
            }
            EBoard.Field field = getBoard().getField(x, y);
            fields.add(new EBoard.Pair<>(field, field.getBestPiecesNonRotating()));
        });
        return fields;
    }

    /**
     * @return 0 if board edge piece, else 1.
     */
    default int boardEdges(EBoard.Pair<EBoard.Field, ? extends Collection<?>> pair) {
        return pair.left.getX() == 0 || pair.left.getY() == 0 ||
               pair.left.getX() == getBoard().getWidth() - 1 ||
               pair.left.getY() == getBoard().getHeight() - 1 ? 0 : 1;
    }

    /**
     * @return 0 if in one of the four 3x3 corners, else 1.
     */
    default int clueCorners(EBoard.Pair<EBoard.Field, ? extends Collection<?>> pair) {
        final int x = pair.left.getX();
        final int y = pair.left.getY();
        final int w = getBoard().getWidth();
        final int h = getBoard().getHeight();

        return ((x <= 2 || x >= w-3) && (y <= 2 || y >= h-3)) ? 0 : 1;
    }

    /**
     * @return amount of valid pieces.
     */
    default int validPieces(EBoard.Pair<EBoard.Field, ? extends Collection<?>> pair) {
        return pair.right.size();
    }

    /**
     * @return nearest to top-left corner, measured row by row.
     */
    default ToIntFunction<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> topLeft() {
        final int boardWidth = getBoard().getWidth();
        return pair -> pair.left.getY() * boardWidth + pair.left.getX();
    }

    default int clueCornersOrdered(EBoard.Pair<EBoard.Field, ? extends Collection<?>> pair) {
        final int x = pair.left.getX();
        final int y = pair.left.getY();
        final int w = getBoard().getWidth();
        final int h = getBoard().getHeight();

        if (x < 3) {
            if (y < 3) {
                return 1;
            } else if (y >= h-3){
                return 3;
            }
        } else if (x >= w-3) {
            if (y < 3) {
                return 2;
            } else if (y >= h-3){
                return 4;
            }
        }
        return 5; // Not a corner
    }
}
