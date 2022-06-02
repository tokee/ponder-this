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
    public EBoard.Pair<EBoard.Field, List<EBoard.Piece>> get() {
/*        return getFreePiecesStrategyA()
                .findFirst()
                .orElse(null);*/
        List<EBoard.Pair<EBoard.Field, Set<Integer>>> all = getFreeRaw();
        return all.isEmpty() ? null : toPieces(Collections.min(all, comparator));
    }

    private Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparatorGeneric() {
        return Comparator.<EBoard.Pair<EBoard.Field, ? extends Collection<?>>>
                        comparingInt(this::boardEdges) // Outer
                .thenComparingInt(this::validPieces)                         // Least valid pieces
                .thenComparingInt(pair -> 4-pair.left.getOuterEdgeCount())           // Least free edges
                .thenComparingInt(pair -> pair.left.getY()*board.getWidth() + pair.left.getX()) // Top left
                .thenComparingInt(pair -> pair.left.getX() == 0 || pair.left.getY() == 0 ||
                                        pair.left.getX() == board.getWidth() - 1 ||
                                        pair.left.getY() == board.getHeight() - 1 ? 0 : 1);
    }

}
