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

import java.util.List;
import java.util.Set;

/**
 * Tries to solve the puzzle by asking for best candidate and selecting the first ones until there are no more
 * candidates. No backtracking or similar.
 *
 * Primarily used for testing.
 */
public class OneWaySolver {
    private static final Logger log = LoggerFactory.getLogger(OneWaySolver.class);

    private final EBoard board;
    private final EPieces pieces;

    public OneWaySolver(EBoard board) {
        this.board = board;
        pieces = board.getPieces();
    }

    public void run() {
        EBoard.Pair<EBoard.Field, List<EBoard.Piece>> free;
        while ((free = board.getFreePieceStrategyA()) != null) {
            if (free.right.isEmpty()) {
                return;
            }
            EBoard.Field field = free.left;
            EBoard.Piece piece = free.right.get(0);
            System.out.printf("Placing at (%d, %d) piece=%d rot=%d\n",
                              field.getX(), field.getY(), piece.piece, piece.rotation);
            if (!board.placePiece(field.getX(), field.getY(), piece.piece, piece.rotation)) {
                System.out.println("Unable to place piece as it invalidates the bag. Stopping run");
                break;
            }
        }
    }
}
