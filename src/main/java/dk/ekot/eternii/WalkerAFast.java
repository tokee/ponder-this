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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Prioritises least valid pieces.
 *
 * Observation: Three corners fairly quickly, then very slow progress.
 */
public class WalkerAFast implements Walker {
    private static final Logger log = LoggerFactory.getLogger(WalkerAFast.class);

    private final EBoard board;
    private final Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> comparator;

    public WalkerAFast(EBoard board) {
        this.board = board;
        comparator = getFieldComparator();
    }

    @Override
    public EBoard getBoard() {
        return board;
    }

    @Override
    public EBoard.Pair<EBoard.Field, List<EBoard.Piece>> getLegacy() {
        List<EBoard.Pair<EBoard.Field, Set<Integer>>> all = getFreeFieldsRaw();
        return all.isEmpty() ? null : toRotatedPieces(Collections.min(all, comparator));
    }

    @Override
    public Stream<Move> getAll() {
        throw new UnsupportedOperationException("Not implemented for old Walker");
    }

    @Override
    public Stream<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> getAllRotated() {
        List<EBoard.Pair<EBoard.Field, Set<Integer>>> all = getFreeFieldsRaw();
        return all.isEmpty() ? null : all.stream().sorted(comparator).map(this::toRotatedPieces);
    }

    private Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparator() {
        return Comparator.<EBoard.Pair<EBoard.Field, ? extends Collection<?>>>comparingInt(pair -> pair.right.size()) // Least valid pieces
                .thenComparingInt(pair -> 4 - pair.left.getOuterEdgeCount())           // Least free edges
                .thenComparingInt(pair -> pair.left.getY() * board.getWidth() + pair.left.getX());
    }
}
