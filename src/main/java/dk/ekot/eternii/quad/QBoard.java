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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Represents a board of 8x8 Quads.
 *
 * The boards guaranties a consistent internal state.
 */
public class QBoard {
    private static final Logger log = LoggerFactory.getLogger(QBoard.class);

    private final PieceTracker pieceTracker = new PieceTracker(); // 1 = available for use, 0 = positioned on the board
    private final EPieces epieces;
    private final EBoard eboard;
    private final QuadBagHandler bagHandler;

    private final int WIDTH = 8;
    private final int HEIGHT = 8;

    private final QField[][] fields = new QField[WIDTH][HEIGHT];;

    public QBoard() {
        epieces = EPieces.getEternii();
        eboard = new EBoard(epieces, WIDTH*2, HEIGHT*2);
        log.debug("Creating bagHandler");
        bagHandler = new QuadBagHandler(pieceTracker);
        log.debug("Assigning bags to fields");
        bagHandler.assignBagsToFields(fields);
        log.debug("Auto selecting edge maps");
        autoSelectEdgeMaps();
        log.debug("Checking needsSatisfiedAll");
        log.info("Board created with initial needsSatisfiedAll=" + areNeedsSatisfiedAll());
    }


    public EBoard getEboard() {
        return eboard;
    }

    public void testMoveAll() {
        for (int x = 0 ; x <= 7 ; x++) {
            for (int y = 0 ; y <= 7 ;y++) {
                placePiece(x, y, y + x * 8);
            }
        }
    }

    /**
     * Visit all coordinates and call the {@code visitor} with them.
     * @param visitor called with {@code x, y}.
     */
    public void visitAllCoordinates(BiConsumer<Integer, Integer> visitor) {
        for (int y = 0 ; y <= 7 ;y++) {
            for (int x = 0 ; x <= 7 ; x++) {
                visitor.accept(x, y);
            }
        }
    }

    /**
     * Visit all fields and call the {@code visitor} with them.
     * @param visitor called with current {@link QField}.
     */
    public void visitAllFields(Consumer<QField> visitor) {
        visitAllCoordinates((x, y) -> visitor.accept(fields[x][y]));
    }

    /**
     * Visit all fields and check that {@link QField#needsSatisfied()} is true.
     * @return true if {@link QField#needsSatisfied()} is true for all fields.
     */
    public boolean areNeedsSatisfiedAll() {
        AtomicBoolean ok = new AtomicBoolean(true);
        visitAllFields(field -> ok.set(ok.get() && field.needsSatisfied()));
        return ok.get();
    }

    /**
     * Takes the quad with the given ID from the {@link QuadBag} at field {@code (x, y)}
     * and assigns it to the given field.
     *
     * Side effect include:<br/>
     * Removing the 4 pieces from the pool of available pieces.<br/>
     * Updating edge maps for the fields N, E, S, W relative to current potition.
     * @param x position on the board.
     * @param y position on the board.
     * @param quadID the index in the QuadBag at {@code (x, y)}.
     * @return true if all surrounding fields has {@link QField#needsSatisfied()} {@code true}, else false.
     */
    // TODO: Trigger update of surrounding fields
    public boolean placePiece(int x, int y, int quadID) {
        QField field = fields[x][y];
        field.setQuad(quadID);
        pieceTracker.removeQPiece(field.getQPiece()); // Must be done before calling needsSatisfied()

        boolean allNeedsSatisfied = autoSelectAdjacentEdgeMaps(x, y);
//        System.out.println("Attempting to place " + QBits.toStringFull(field.getQPiece(), field.getQEdges()));

        placePieceOnEBoard(x, y, field.getQPiece(), field.getQEdges());
        return allNeedsSatisfied;
    }

    public void removePiece(int x, int y) {
        QField field = fields[x][y];
        if (field.isFree()) {
            throw new IllegalStateException("The field " + fields[x][y] + " was already free");
        }
        pieceTracker.addQPiece(field.getQPiece());
        field.setFree();
        autoSelectEdgeMap(x, y);
        autoSelectAdjacentEdgeMaps(x, y);
        
        removeQPieceFromEBoard(x, y);
    }

    private boolean autoSelectAdjacentEdgeMaps(int x, int y) {
        boolean allNeedsSatisfied = true;
        if (y != 0) {
            autoSelectEdgeMap(x, y -1);
            allNeedsSatisfied &= getField(x, y -1).needsSatisfied();
        }
        if (x != 7) {
            autoSelectEdgeMap(x +1, y);
            allNeedsSatisfied &= getField(x +1, y).needsSatisfied();
        }
        if (y != 7) {
            autoSelectEdgeMap(x, y +1);
            allNeedsSatisfied &= getField(x, y +1).needsSatisfied();
        }
        if (x != 0) {
            autoSelectEdgeMap(x -1, y);
            allNeedsSatisfied &= getField(x -1, y).needsSatisfied();
        }
        return allNeedsSatisfied;
    }

    private void removeQPieceFromEBoard(int x, int y) {
        if (eboard.getPiece(x << 1, y << 1) ==          EPieces.NULL_P ||
            eboard.getPiece((x << 1 )+1, y << 1) ==     EPieces.NULL_P ||
            eboard.getPiece((x << 1 )+1, (y << 1)+1) == EPieces.NULL_P ||
            eboard.getPiece(x << 1, (y << 1)+1) ==      EPieces.NULL_P) {
            throw new IllegalStateException(
                    "Attempting to remove quad at (" + x + ", " + y + ") but it was already free");
        }
        eboard.removeUntrackedPiece(x << 1, y << 1);
        eboard.removeUntrackedPiece((x << 1) + 1, y << 1);
        eboard.removeUntrackedPiece((x << 1) + 1, (y << 1) + 1);
        eboard.removeUntrackedPiece(x << 1, (y << 1) + 1);
    }


    // Update the EBoard only.
    private void placePieceOnEBoard(int x, int y, int qpiece, long qedges) {
        if (eboard.getPiece(x << 1, y << 1) !=          EPieces.NULL_P ||
            eboard.getPiece((x << 1 )+1, y << 1) !=     EPieces.NULL_P ||
            eboard.getPiece((x << 1 )+1, (y << 1)+1) != EPieces.NULL_P ||
            eboard.getPiece(x << 1, (y << 1)+1) !=      EPieces.NULL_P) {
            throw new IllegalStateException("Attempting to place quad at (" + x + ", " + y + ") but it was occupied");
        }
        //        System.out.println("Piece: " + QBits.toStringFull(qpiece, qedges));
        //System.out.println("Edges: " + QBits.toStringQEdges(qedges));
        eboard.placeUntrackedPiece(x << 1, y << 1,             QBits.getPieceNW(qpiece), QBits.getRotNW(qedges));
        eboard.placeUntrackedPiece((x << 1) + 1, y << 1,       QBits.getPieceNE(qpiece), QBits.getRotNE(qedges));
        eboard.placeUntrackedPiece((x << 1) + 1, (y << 1) + 1, QBits.getPieceSE(qpiece), QBits.getRotSE(qedges));
        eboard.placeUntrackedPiece(x << 1, (y << 1) + 1,       QBits.getPieceSW(qpiece), QBits.getRotSW(qedges));
        // TODO: Update surrounding qfields
        // TODO: Check overall board validity
    }

    /**
     * Choose the correct edge map for all fields based on surrounding fields.
     */
    public void autoSelectEdgeMaps() {
        visitAllCoordinates(this::autoSelectEdgeMap);
    }
    /**
     * Choose the correct edge map for the field at {@code (x, y)} based on surrounding fields.
     * @param x horizontal position on the qboard.
     * @param y vertical position on the qboard.
     */
    public void autoSelectEdgeMap(int x, int y) {
        fields[x][y].autoSelectEdgeMap(
                y == 0 ? -1 : fields[x][y-1].getEdgeIfDefinedS(),
                x == 7 ? -1 : fields[x+1][y].getEdgeIfDefinedW(),
                y == 7 ? -1 : fields[x][y+1].getEdgeIfDefinedN(),
                x == 0 ? -1 : fields[x-1][y].getEdgeIfDefinedE());
    }

    /**
     * @return the QField at position {@code (x, y)} on the board.
     */
    public QField getField(int x, int y) {
        return fields[x][y];
    }

    public PieceTracker getPieceTracker() {
        return pieceTracker;
    }
}
