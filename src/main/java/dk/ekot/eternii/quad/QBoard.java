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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a board of 8x8 Quads.
 *
 * The boards guarantees a consistent internal state.
 */
public class QBoard {
    private static final Logger log = LoggerFactory.getLogger(QBoard.class);

    public final EPieces epieces;
    public final EBoard eboard;

    private final int WIDTH = 8;
    private final int HEIGHT = 8;

    private final PieceMap pieceMap = new PieceMap(); // 1 = available for use, 0 = positioned on the board

    private final QuadBag BAG_CORNER_NW;
    private final QuadBag BAG_CORNER_NE;
    private final QuadBag BAG_CORNER_SE;
    private final QuadBag BAG_CORNER_SW;

    private final QuadBag BAG_BORDER_N;
    private final QuadBag BAG_BORDER_E;
    private final QuadBag BAG_BORDER_S;
    private final QuadBag BAG_BORDER_W;

    public QBoard() {
        // TODO: Share piece masker bitmap and processing between corners (and edges)
        BAG_CORNER_NW = QuadCreator.createCorner(new QuadBag(pieceMap)).trim();
        BAG_CORNER_NE = BAG_CORNER_NW.rotClockwise();
        BAG_CORNER_SE = BAG_CORNER_NE.rotClockwise();
        BAG_CORNER_SW = BAG_CORNER_SE.rotClockwise();

        BAG_BORDER_N = QuadCreator.createEdges(new QuadBag(pieceMap)).trim();
        BAG_BORDER_E = BAG_BORDER_N.rotClockwise();
        BAG_BORDER_S = BAG_BORDER_E.rotClockwise();
        BAG_BORDER_W = BAG_BORDER_S.rotClockwise();

        epieces = EPieces.getEternii();
        eboard = new EBoard(epieces, 16, 16);
        
    }

    public EBoard getEboard() {
        return eboard;
    }

    public void testMove() {
        placePiece(BAG_CORNER_NW, 0, 0, 0);
        placePiece(BAG_CORNER_NE, 1, 7, 0);
        placePiece(BAG_CORNER_SE, 2, 7, 7);
        placePiece(BAG_CORNER_SW, 3, 0, 7);

        placePiece(BAG_BORDER_N, 0, 3, 0);
        placePiece(BAG_BORDER_N, 1, 5, 0);
        placePiece(BAG_BORDER_E, 3, 7, 3);
        placePiece(BAG_BORDER_E, 4, 7, 5);
        placePiece(BAG_BORDER_S, 5, 5, 7);
        placePiece(BAG_BORDER_S, 6, 3, 7);
        placePiece(BAG_BORDER_W, 7, 0, 5);
        placePiece(BAG_BORDER_W, 8, 0, 3);
    }

    private void placePiece(QuadBag bag, int index, int x, int y) {
        int qpiece = bag.getQPiece(index);
        long qedges = bag.getQEdges(index);
//        System.out.println("Piece: " + QBits.toStringFull(qpiece, qedges));
        //System.out.println("Edges: " + QBits.toStringQEdges(qedges));
        eboard.placeUntrackedPiece(x << 1, y << 1, QBits.getPieceNW(qpiece), QBits.getRotNW(qedges));
        eboard.placeUntrackedPiece((x << 1)+1, y << 1, QBits.getPieceNE(qpiece), QBits.getRotNE(qedges));
        eboard.placeUntrackedPiece((x << 1)+1, (y << 1)+1, QBits.getPieceSE(qpiece), QBits.getRotSE(qedges));
        eboard.placeUntrackedPiece(x << 1, (y << 1)+1, QBits.getPieceSW(qpiece), QBits.getRotSW(qedges));
    }
}
