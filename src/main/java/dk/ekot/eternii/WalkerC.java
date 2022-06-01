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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * First field edges, then board edges.
 *
 * Observation: Practically same as WalkerB
 */
public class WalkerC implements Walker {
    private static final Logger log = LoggerFactory.getLogger(WalkerC.class);

    private final EBoard board;
    private final Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> comparator;

    public WalkerC(EBoard board) {
        this.board = board;
        comparator = getFieldComparator();
    }

    @Override
    public EBoard.Pair<EBoard.Field, List<EBoard.Piece>> get() {
        List<EBoard.Pair<EBoard.Field, Set<Integer>>> all = getFreeRaw(board);
        all.sort(comparator);
        return all.isEmpty() ? null : toPieces(all.get(0));
    }

    /**
     * @return the free fields with lists of corresponding Pieces. Empty if no free fields.
     */
    public Stream<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> getFreePiecesStrategyA() {
        return board.streamAllFields()
                .filter(EBoard.Field::isFree)
                .map(field -> new EBoard.Pair<>(field, field.getBestPieces()))
                .sorted(comparator);
    }

    private Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparator() {
        return Comparator.<EBoard.Pair<EBoard.Field, ? extends Collection<?>>>comparingInt(pair -> pair.right.size()) // Least valid pieces
                .thenComparingInt(
                        pair -> pair.left.getX() == 0 || pair.left.getY() == 0 ||
                                pair.left.getX() == board.getWidth() - 1 ||
                                pair.left.getY() == board.getHeight() - 1 ? 0 : 1) // Board edges
                .thenComparingInt(pair -> 4-pair.left.getOuterEdgeCount())           // Least free edges
                .thenComparingInt(pair -> pair.left.getY()*board.getWidth() + pair.left.getX());
    }


}
