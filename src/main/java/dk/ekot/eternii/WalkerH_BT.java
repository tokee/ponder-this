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
 * Board edge, top-left, valids, edges
 *
 */
public class WalkerH_BT extends WalkerImpl {
    private static final Logger log = LoggerFactory.getLogger(WalkerH_BT.class);

    public WalkerH_BT(EBoard board) {
        super(board);
    }

    @Override
    protected Comparator<Move> getMoveComparator() {
        return Comparator.
                comparingInt(Move::boardEdgeFirst)
                .thenComparingInt(Move::topLeftFirst)
                .thenComparingInt(Move::validPiecesSize)
                .thenComparingInt(move -> 4-move.leastSetOuterEdgesFirst());
    }

    @Override
    protected Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparator() {
        return Comparator.
                comparingInt(this::boardEdges)
                .thenComparingInt(topLeft())
                .thenComparingInt(this::validPieces)
                .thenComparingInt(pair -> 4-pair.left.getOuterEdgeCount());
    }



}
