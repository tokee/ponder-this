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
 * Board edge, valids, edges, top-left
 *
 */
public class WalkerF implements Walker {
    private static final Logger log = LoggerFactory.getLogger(WalkerF.class);

    private final EBoard board;
    private final Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> comparator;

    public WalkerF(EBoard board) {
        this.board = board;
        comparator = getFieldComparatorGeneric();
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

    private Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparatorGeneric() {
        return Comparator.
                comparingInt(this::boardEdges)
                .thenComparingInt(this::validPieces)
                .thenComparingInt(pair -> 4-pair.left.getOuterEdgeCount()) // Least free edges
                .thenComparingInt(topLeft());
    }


}
