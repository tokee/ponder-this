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
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Provides next Field wih corresponding Pieces to try.
 */
public interface Walker extends Supplier<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> {

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
    default List<EBoard.Pair<EBoard.Field, Set<Integer>>> getFreeRaw(EBoard board) {
        List<EBoard.Pair<EBoard.Field, Set<Integer>>> fields = new ArrayList<>(board.getWidth() * board.getHeight());
        board.visitAll((x, y) -> {
            final long state = board.getState(x, y);
            if (EBits.hasPiece(state)) {
                return;
            }
            EBoard.Field field = board.getField(x, y);
            fields.add(new EBoard.Pair<>(field, field.getBestPiecesNonRotating()));
        });
        return fields;
    }

}
