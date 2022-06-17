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
import java.util.Comparator;

/**
 * Special purpose Walker for prioritizong a given rectangle on the board.
 */
class WalkerRectangle extends WalkerImpl {
    private final Rect rect;

    /**
     * @param board
     * @param rect all edges inclusive.
     */
    public WalkerRectangle(EBoard board, Rect rect) {
        super(board);
        this.rect = rect;
    }

    @Override
    protected Comparator<EBoard.Pair<EBoard.Field, ? extends Collection<?>>> getFieldComparator() {
        return Comparator.<EBoard.Pair<EBoard.Field, ? extends Collection<?>>>
                        comparingInt(pair -> rect.isInside(pair.left.getX(), pair.left.getY()) ? 0 : 1)
                .thenComparingInt(this::validPieces)
                .thenComparingInt(pair -> 4 - pair.left.getOuterEdgeCount()); // Least free edges
    }
}
