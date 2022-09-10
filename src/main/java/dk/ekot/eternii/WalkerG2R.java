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

import dk.ekot.misc.ExtractionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Clue corners, valids, board edge, edges
 *
 * Deterministic Random as tie-breaker for both fields and pieces
 *
 */
public class WalkerG2R extends WalkerImpl {
    private static final Logger log = LoggerFactory.getLogger(WalkerG2R.class);

    private final Comparator<EBoard.Pair<EBoard.Field, Set<Integer>>> comparatorSet = getFieldComparatorSet();

    public Random random;
    {
        int seed = new Random().nextInt();
        log.info("Random seed=" + seed);
        random = new Random(seed);
    }

    public WalkerG2R(EBoard board) {
        super(board);
    }

    @Override
    public EBoard.Pair<EBoard.Field, List<EBoard.Piece>> getLegacy() {
        List<EBoard.Pair<EBoard.Field, Set<Integer>>> all = getFreeFieldsRaw();
        if (all.isEmpty()) {
            return null;
        }
        List<EBoard.Pair<EBoard.Field, Set<Integer>>> minima = ExtractionUtils.minima(all.stream(), comparatorSet);
        EBoard.Pair<EBoard.Field, Set<Integer>> pair = minima.get(random.nextInt(minima.size()));
        return toRotatedPieces(shuffle(pair));
    }

    @Override
    public Move get() {
        List<Move> minima = ExtractionUtils.minima(getAll(), moveComparator);
        Move move = minima.get(random.nextInt(minima.size()));
        shuffle(move);
        return move;
    }

    // TODO: Implement getAll like get()

    @Override
    public Stream<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> getAllRotated() {
        List<EBoard.Pair<EBoard.Field, Set<Integer>>> all = getFreeFieldsRaw();
        List<EBoard.Pair<EBoard.Field, Set<Integer>>> shuffled = new ArrayList<>(all.size());
        for (EBoard.Pair<EBoard.Field, Set<Integer>> pair: all) {
            shuffled.add(shuffle(pair));
        }
        Collections.shuffle(shuffled, random);
        shuffled.sort(comparatorSet); // Makes sense as Java sort is stable
        return all.isEmpty() ? null : shuffled.stream().sorted(fieldComparator).map(this::toRotatedPieces);
    }

    private EBoard.Pair<EBoard.Field, Set<Integer>> shuffle(EBoard.Pair<EBoard.Field, Set<Integer>> pair) {
        List<Integer> pieces = new ArrayList<>(pair.right);
        Collections.shuffle(pieces, random);
        return new EBoard.Pair<>(pair.left, new LinkedHashSet<>(pieces));
    }

    private void shuffle(Move move) {
        List<Integer> pieceIDs = new ArrayList<>(move.getPieceIDs());
        Collections.shuffle(pieceIDs, random);
        move.setLocalPieceIDs(new LinkedHashSet<>(pieceIDs));
    }

    @Override
    protected Comparator<Move> getMoveComparator() {
        return Comparator.
                comparingInt(Move::clueCornersOrdered)
                .thenComparingInt(Move::validPiecesSize)
                .thenComparingInt(Move::boardEdgeFirst)
                .thenComparingInt(Move::mostSetOuterEdgesFirst) // Least free edges
                .thenComparingInt(Move::topLeftFirst);
    }

    @Override
    protected Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparator() {
        return Comparator.
                comparingInt(this::clueCornersOrdered)
                .thenComparingInt(this::validPieces)
                .thenComparingInt(this::boardEdges)
                .thenComparingInt(pair -> 4-pair.left.getOuterEdgeCount()) // Least free edges
                .thenComparingInt(topLeft());
    }


    protected <C> Comparator<EBoard.Pair<EBoard.Field, Set<C>>> getFieldComparatorSet() {
        return Comparator.<EBoard.Pair<EBoard.Field, Set<C>>>
                comparingInt(this::cornersToClues)
                .thenComparingInt(this::validPieces)
                .thenComparingInt(this::boardEdges)
                .thenComparingInt(pair -> 4-pair.left.getOuterEdgeCount());
    }


}
