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
public class WalkerGCornerClue5 extends WalkerImpl {
    private static final Logger log = LoggerFactory.getLogger(WalkerGCornerClue5.class);

    public WalkerGCornerClue5(EBoard board) {
        super(board);
    }

    // Expects 3x3 corners + edge to be set. Center clue at (7, 8)
    private static final int[][][] BL_TO_TR = new int[][][]{
            {{1, 12}, {3, 14}}, // 1
            {{1, 11}, {2, 12}, {3, 13}, {4, 14}}, // 2
            {{1, 10}, {2, 11}, {3, 12}, {4, 13}, {5, 14}}, // 3
            {{1,  9}, {2, 10}, {3, 11}, {4, 12}, {5, 13}, {6, 14}}, // 4
            {{1,  8}, {2,  9}, {3, 10}, {4, 11}, {5, 12}, {6, 13}, {7, 14}}, // 5
            {{2,  8}, {3,  9}, {4, 10}, {5, 11}, {6, 12}, {7, 13}}, // 6
            {{3,  8}, {4,  9}, {5, 10}, {6, 11}, {7, 12}}, // 7
            {{4,  8}, {5,  9}, {6, 10}, {7, 11}}, // 8
            {{5,  8}, {6,  9}, {7, 10}} // 9
    };

    @Override
    protected Comparator<Move> getMoveComparator() {
        return Comparator.
                comparingInt(this::onClueCornersOrdered)
                .thenComparingInt(this::onBoardEdges)
//                .thenComparingInt(priority(BL_TO_TR))
                .thenComparingInt(onRect(1, 8, 7, 14))
                .thenComparingInt(Move::piecesSize)
                .thenComparingInt(move -> 4-move.getOuterEdgeCount()) // Least free edges
                .thenComparingInt(Move::getTopLeftPos);
    }

    @Override
    protected Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparator() {
        return Comparator.
                comparingInt(this::clueCornersOrdered)
                .thenComparingInt(this::boardEdges)
//                .thenComparingInt(priority(BL_TO_TR))
                .thenComparingInt(rect(1, 8, 7, 14))
                .thenComparingInt(this::validPieces)
                .thenComparingInt(pair -> 4-pair.left.getOuterEdgeCount()) // Least free edges
                .thenComparingInt(topLeft());
    }

}
