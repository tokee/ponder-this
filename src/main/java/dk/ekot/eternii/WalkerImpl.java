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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Super class for Walkers.
 *
 * Requests all free fields with piece-IDs and used {@link #comparator} to return {@code min} in {@link #getLegacy()}.
 *
 */
public abstract class WalkerImpl implements Walker {
    final EBoard board;
    final EPieces pieces;
    final Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> comparator;
    final Comparator<Move> moveComparator;

    public WalkerImpl(EBoard board) {
        this.board = board;
        this.pieces = board.getPieces();
        comparator = getFieldComparator();
        moveComparator = getMoveComparator();
    }

    @Override
    public Stream<Move> getAll() {
        return streamRawMoves().sorted(moveComparator);
    }

    @Override
    public EBoard.Pair<EBoard.Field, List<EBoard.Piece>> getLegacy() {
        List<EBoard.Pair<EBoard.Field, Set<Integer>>> all = getFreeFieldsRaw();
        return all.isEmpty() ? null : toRotatedPieces(Collections.min(all, comparator));
    }

    @Override
    public Stream<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> getAllRotated() {
        List<EBoard.Pair<EBoard.Field, Set<Integer>>> all = getFreeFieldsRaw();
        return all.isEmpty() ? null : all.stream().sorted(comparator).map(this::toRotatedPieces);
    }

    @Override
    public EBoard getBoard() {
        return board;
    }

    /**
     * @deprecated use {@link #getMoveComparator()} instead.
     */
    protected abstract Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparator();

    protected abstract Comparator<Move> getMoveComparator();
}
