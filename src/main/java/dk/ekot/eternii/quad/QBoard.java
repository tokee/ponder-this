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
package dk.ekot.eternii.quad;

import dk.ekot.eternii.EBoard;
import dk.ekot.eternii.EPieces;
import org.apache.commons.collections.Bag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a board of 8x8 Quads.
 *
 * The boards guaranties a consistent internal state.
 */
public class QBoard {
    private static final Logger log = LoggerFactory.getLogger(QBoard.class);

    public final EPieces epieces;
    public final EBoard eboard;
    private final QuadBagHandler bagHandler;

    private final int WIDTH = 8;
    private final int HEIGHT = 8;

    private final QField[][] fields = new QField[WIDTH][HEIGHT];;

    public QBoard() {
        epieces = EPieces.getEternii();
        eboard = new EBoard(epieces, WIDTH*2, HEIGHT*2);
        bagHandler = new QuadBagHandler();
        bagHandler.assignBagsToFields(fields);
    }

    public EBoard getEboard() {
        return eboard;
    }

    public void testMoveAll() {
        for (int x = 0 ; x <= 7 ; x++) {
            for (int y = 0 ; y <= 7 ;y++) {
                placePiece(fields[x][y].getBag(), y+x*8, x, y);
            }
        }
    }

    private void placePiece(QuadBag bag, int index, int x, int y) {
        int qpiece = bag.getQPiece(index);
        long qedges = bag.getQEdges(index);
        placePiece(qpiece, qedges, x, y);
    }

    private void placePiece(int qpiece, long qedges, int x, int y) {
        if (eboard.getPiece(x << 1, y << 1) != EPieces.NULL_P ||
            eboard.getPiece((x << 1 )+1, y << 1) != EPieces.NULL_P ||
            eboard.getPiece((x << 1 )+1, (y << 1)+1) != EPieces.NULL_P ||
            eboard.getPiece(x << 1, (y << 1)+1) != EPieces.NULL_P) {
            throw new IllegalStateException("Attempting to place quad at (" + x + ", " + y + ") but it was occupied");
        }
        //        System.out.println("Piece: " + QBits.toStringFull(qpiece, qedges));
        //System.out.println("Edges: " + QBits.toStringQEdges(qedges));
        eboard.placeUntrackedPiece(x << 1, y << 1, QBits.getPieceNW(qpiece), QBits.getRotNW(qedges));
        eboard.placeUntrackedPiece((x << 1) + 1, y << 1, QBits.getPieceNE(qpiece), QBits.getRotNE(qedges));
        eboard.placeUntrackedPiece((x << 1) + 1, (y << 1) + 1, QBits.getPieceSE(qpiece), QBits.getRotSE(qedges));
        eboard.placeUntrackedPiece(x << 1, (y << 1) + 1, QBits.getPieceSW(qpiece), QBits.getRotSW(qedges));
        // TODO: Update surrounding qfields
        // TODO: Check overall board validity
    }

}
