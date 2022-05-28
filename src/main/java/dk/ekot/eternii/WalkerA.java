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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 *
 */
public class WalkerA implements Walker {
    private static final Logger log = LoggerFactory.getLogger(WalkerA.class);

    private final EBoard board;

    public WalkerA(EBoard board) {
        this.board = board;
    }

    @Override
    public EBoard.Pair<EBoard.Field, List<EBoard.Piece>> get() {
        return getFreePiecesStrategyA()
                .findFirst()
                .orElse(null);
    }

    /**
     * @return the free fields with lists of corresponding Pieces. Empty if no free fields.
     */
    public Stream<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> getFreePiecesStrategyA() {
        Comparator<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> comparator = getFieldComparatorA();
        return board.streamAllFields()
                .filter(EBoard.Field::isFree)
                .map(field -> new EBoard.Pair<>(field, field.getBestPieces()))
//                .peek(e -> System.out.println("    field(" + e.left.getX() +  ", " + e.left.getY() + "), pieces=" + e.right))
                .sorted(comparator);
//                .peek(e -> System.out.println("Best field(" + e.left.getX() +  ", " + e.left.getY() + "), pieces=" + e.right));
    }

    private Comparator<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> getFieldComparatorA() {
        return Comparator.<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>>comparingInt(pair -> pair.right.size())                         // Least valid pieces
                .thenComparingInt(pair -> 4-pair.left.getOuterEdgeCount())           // Least free edges
                .thenComparingInt(pair -> pair.left.getY()*board.getWidth() + pair.left.getX());
    }

    private Comparator<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> getFieldComparatorB() {
        return Comparator.<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>>comparingInt(
                pair -> pair.left.getX() == 0 || pair.left.getY() == 0 ||
                        pair.left.getX() == board.getWidth() - 1 ||
                        pair.left.getY() == board.getHeight() - 1 ? 0 : 1) // Edges first
                .thenComparingInt(pair -> pair.right.size())                         // Least valid pieces
                .thenComparingInt(pair -> 4-pair.left.getOuterEdgeCount())           // Least free edges
                .thenComparingInt(pair -> pair.left.getY()*board.getWidth() + pair.left.getX());
    }


}
