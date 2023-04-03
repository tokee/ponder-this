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
 * The boards guaranties a consistent internal state.
 */
public class QBoard {
    private static final Logger log = LoggerFactory.getLogger(QBoard.class);

    public final EPieces epieces;
    public final EBoard eboard;

    private final int WIDTH = 8;
    private final int HEIGHT = 8;

    private final PieceMap pieceMap = new PieceMap(); // 1 = available for use, 0 = positioned on the board
    private final QField[][] fields;

    private final QuadBag BAG_CORNER_NW;
    private final QuadBag BAG_CORNER_NE;
    private final QuadBag BAG_CORNER_SE;
    private final QuadBag BAG_CORNER_SW;

    private final QuadBag BAG_BORDER_N;
    private final QuadBag BAG_BORDER_E;
    private final QuadBag BAG_BORDER_S;
    private final QuadBag BAG_BORDER_W;

    private final QuadBag BAG_CLUE_NW;
    private final QuadBag BAG_CLUE_NE;
    private final QuadBag BAG_CLUE_SE;
    private final QuadBag BAG_CLUE_SW;
    private final QuadBag BAG_CLUE_C;

    private final QuadBag BAG_INNER;

    public QBoard() {
        // TODO: Share piece masker bitmap and processing between corners (and edges)
        BAG_CORNER_NW = QuadCreator.createCorner(new QuadBag(pieceMap, QuadBag.BAG_TYPE.corner_nw)).trim();
        BAG_CORNER_NE = BAG_CORNER_NW.rotClockwise();
        BAG_CORNER_NE.generateSets();
        BAG_CORNER_SE = BAG_CORNER_NE.rotClockwise();
        BAG_CORNER_SE.generateSets();
        BAG_CORNER_SW = BAG_CORNER_SE.rotClockwise();
        BAG_CORNER_SW.generateSets();

        BAG_BORDER_N = QuadCreator.createEdges(new QuadBag(pieceMap, QuadBag.BAG_TYPE.border_n)).trim();
        BAG_BORDER_E = BAG_BORDER_N.rotClockwise();
        BAG_BORDER_E.generateSets();
        BAG_BORDER_S = BAG_BORDER_E.rotClockwise();
        BAG_BORDER_S.generateSets();
        BAG_BORDER_W = BAG_BORDER_S.rotClockwise();
        BAG_BORDER_W.generateSets();

        BAG_CLUE_NW = QuadCreator.createClueNW(new QuadBag(pieceMap, QuadBag.BAG_TYPE.clue_nw)).trim();
        BAG_CLUE_NE = QuadCreator.createClueNE(new QuadBag(pieceMap, QuadBag.BAG_TYPE.clue_ne)).trim();
        BAG_CLUE_SE = QuadCreator.createClueSE(new QuadBag(pieceMap, QuadBag.BAG_TYPE.clue_se)).trim();
        BAG_CLUE_SW = QuadCreator.createClueSW(new QuadBag(pieceMap, QuadBag.BAG_TYPE.clue_sw)).trim();
        BAG_CLUE_C =  QuadCreator.createClueC( new QuadBag(pieceMap, QuadBag.BAG_TYPE.clue_c)).trim();

        // TODO: Optimize by using createInnersNoQRot and invent new tricks
        BAG_INNER =  QuadCreator.createInners(new QuadBag(pieceMap, QuadBag.BAG_TYPE.inner)).trim();

        epieces = EPieces.getEternii();
        eboard = new EBoard(epieces, WIDTH*2, HEIGHT*2);

        // Assign the correct QuadBags to all fields.
        fields = new QField[WIDTH][HEIGHT];
        for (int x = 0 ; x <= 7 ; x++) {
            for (int y = 0 ; y <= 7 ;y++) {
                fields[x][y] = new QField(BAG_INNER, x, y);
            }
        }
        fields[0][0] = new QField(BAG_CORNER_NW, 0, 0);
        fields[7][0] = new QField(BAG_CORNER_NE, 7, 0);
        fields[7][7] = new QField(BAG_CORNER_SE, 7, 7);
        fields[0][7] = new QField(BAG_CORNER_SW, 0, 7);
        for (int t = 1 ; t <= 6 ; t++) {
            fields[t][0] = new QField(BAG_BORDER_N, t, 0);
            fields[t][7] = new QField(BAG_BORDER_S, t, 7);
            fields[0][t] = new QField(BAG_BORDER_W, 0, t);
            fields[7][t] = new QField(BAG_BORDER_E, 7, t);
        }
        fields[1][1] = new QField(BAG_CLUE_NW, 1, 1);
        fields[6][1] = new QField(BAG_CLUE_NE, 6, 1);
        fields[6][6] = new QField(BAG_CLUE_SE, 6, 6);
        fields[1][6] = new QField(BAG_CLUE_SW, 1, 6);
        fields[3][4] = new QField(BAG_CLUE_C,  3, 4);
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
    public void testMove() {
        placePiece(BAG_CORNER_NW, 0, 0, 0);
        placePiece(BAG_CORNER_NE, 1, 7, 0);
        placePiece(BAG_CORNER_SE, 2, 7, 7);
        placePiece(BAG_CORNER_SW, 3, 0, 7);

        placePiece(BAG_BORDER_N, 0, 2, 0);
        placePiece(BAG_BORDER_E, 3, 7, 2);
        placePiece(BAG_BORDER_S, 5, 5, 7);
        placePiece(BAG_BORDER_W, 7, 0, 5);

        placePiece(BAG_CLUE_NW, 0, 1, 1);
        placePiece(BAG_CLUE_NE, 1, 6, 1);
        placePiece(BAG_CLUE_SE, 2, 6, 6);
        placePiece(BAG_CLUE_SW, 3, 1, 6);
        placePiece(BAG_CLUE_C, 3, 3, 4);

        placePiece(BAG_INNER, 0, 4, 2);
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
