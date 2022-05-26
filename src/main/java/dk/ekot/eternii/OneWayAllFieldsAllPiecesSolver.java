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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tries to solve the puzzle by trying all fields, all pieces until there are no more pieces (or fields).
 * No backtracking.
 *
 * Primarily used for testing.
 */
public class OneWayAllFieldsAllPiecesSolver implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(OneWayAllFieldsAllPiecesSolver.class);

    private final EBoard board;
    private final EPieces pieces;

    public OneWayAllFieldsAllPiecesSolver(EBoard board) {
        this.board = board;
        pieces = board.getPieces();
    }

    @Override
    public void run() {
        while (true) {
            List<EBoard.Pair<EBoard.Field, List<EBoard.Piece>>> frees =
                    board.getFreePiecesStrategyA().collect(Collectors.toList());
            boolean foundOne = false;
            for (EBoard.Pair<EBoard.Field, List<EBoard.Piece>> free: frees) {
                if (free.right.isEmpty()) {
                    return;
                }
                EBoard.Field field = free.left;
                for (EBoard.Piece piece : free.right) {
                    log.info("Placing at ({}, {}) piece={} rot={}", field.getX(), field.getY(), piece.piece, piece.rotation);
                    if (board.placePiece(field.getX(), field.getY(), piece.piece, piece.rotation)) {
                        foundOne = true;
                        break;
                    }
                    log.info("Failed placement, trying next (if any)");
                }
                if (field.getPiece() == -1) {
                    log.info("Tried all pieces " + free.right + " at " + free.left + " without finding a valid one");
                } else {
                    break;
                }
            }
            if (!foundOne) {
                break;
            }
        }
        log.debug(board.getEdgeTracker().toString());
    }
}
