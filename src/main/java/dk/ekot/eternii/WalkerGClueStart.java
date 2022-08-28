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

/**
 * Clue corners, valids, board edge, edges, top-left
 *
 */
public class WalkerGClueStart extends WalkerImpl {
    private static final Logger log = LoggerFactory.getLogger(WalkerGClueStart.class);

    public WalkerGClueStart(EBoard board) {
        super(board);
    }

    // Around each of the corner clues in order n, e, s, w, ne, se, sw, nw
    private static final int[][] CORNER_CLUES_FIRST = new int[][]{
            // Clues: (2, 2), (13, 2), (2, 13), (13, 13) + (7, 8)
            {2, 1}, {13, 1}, {2, 12}, {13, 12}, // n
            {3, 2}, {14, 2}, {3, 13}, {14, 13}, // e
            {2, 3}, {13, 3}, {2, 14}, {13, 14}, // s
            {1, 2}, {12, 2}, {1, 13}, {12, 13}, // w
            {3, 1}, {14, 1}, {3, 12}, {14, 12}, // ne
            {3, 3}, {14, 3}, {3, 14}, {14, 14}, // se
            {1, 3}, {12, 3}, {1, 14}, {12, 14}, // sw
            {1, 1}, {12, 1}, {1, 12}, {12, 12}  // nw
    };

    @Override
    protected Comparator<Move> getMoveComparator() {
        return Comparator.
                comparingInt(onPriority(CORNER_CLUES_FIRST))
                .thenComparingInt(Move::getTopLeftPos)
                .thenComparingInt(this::onBoardEdges)
                .thenComparingInt(move -> 4-move.getOuterEdgeCount()) // Least free edges
                .thenComparingInt(Move::getTopLeftPos);
    }

    @Override
    protected Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparator() {
        return Comparator.
                comparingInt(priority(CORNER_CLUES_FIRST))
                .thenComparingInt(this::validPieces)
                .thenComparingInt(this::boardEdges)
                .thenComparingInt(pair -> 4-pair.left.getOuterEdgeCount()) // Least free edges
                .thenComparingInt(topLeft());
    }

}
