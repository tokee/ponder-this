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

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.text.AttributedString;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * Opens a windows showing an {@link EBoard} and tracks changes.
 */
// TODO: Print number of valid pieces on non-piece fields
public class BoardVisualiser implements BoardObserver {
    private static final Logger log = LoggerFactory.getLogger(BoardVisualiser.class);

    public static final int UPDATE_INTERVAL = 300; // Milliseconds
    
    private final EBoard board;
    private final EPieces pieces;
    private final JComponent boardDisplayComponent;
    private final BufferedImage boardImage;
    private final int edgeWidth;
    private final int edgeHeight;
    private boolean needsUpdate = false;
    private final String[][] labels;
    private final TYPE type;

    private int best = 0;
    private boolean showPossiblePieces = true;
    private boolean overflow = false;

    public enum TYPE {live, best, best_unplaced}

    public BoardVisualiser(EBoard board) {
        this(board, TYPE.best);
    }
    public BoardVisualiser(EBoard board, boolean onlyUpdateOnBetter) {
        this(board, onlyUpdateOnBetter ? TYPE.best : TYPE.live, null);
    }
    public BoardVisualiser(EBoard board, boolean onlyUpdateOnBetter, String title) {
        this(board, onlyUpdateOnBetter ? TYPE.best : TYPE.live, title);
    }
    public BoardVisualiser(EBoard board, TYPE type) {
        this(board, type, null);
    }
    public BoardVisualiser(EBoard board, TYPE type, String title) {
        this.board = board;
        this.type = type;

        labels = new String[board.getWidth()][board.getHeight()];
        for (int x = 0 ; x < labels.length ; x++) {
            labels[x] = new String[board.getWidth()];
            for (int y = 0 ; y < board.getHeight() ; y++) {
                labels[x][y] = "";
            }
        }
        pieces = board.getPieces();

        BufferedImage edge0 = pieces.getEdgeImage(0);
        edgeWidth = edge0.getWidth();
        edgeHeight = edge0.getHeight();
        boardImage = new BufferedImage(edgeWidth*board.getWidth(), edgeHeight*board.getHeight(), BufferedImage.TYPE_INT_RGB);
        invalidateAll();
        boardDisplayComponent = BaseGraphics.displayImage(boardImage, title);

        if (type == TYPE.live) {
            startThread();
        }
        board.registerObserver(this);
    }

    private void startThread() {
        Thread t = new Thread(() -> {
            while (true) {
                invalidateConditionally();
                try {
                    Thread.sleep(UPDATE_INTERVAL);
                } catch (InterruptedException e) {
                    log.debug("Interrupted while sleeping");
                }
            }
        }, "BoardVisualizer");
        t.setDaemon(true);
        t.start();
    }

    public boolean isShowPossiblePieces() {
        return showPossiblePieces;
    }

    public void setShowPossiblePieces(boolean showPossiblePieces) {
        this.showPossiblePieces = showPossiblePieces;
    }

    private void invalidateConditionally() {
        if (!needsUpdate) {
            return;
        }
        if (type == TYPE.best || type == TYPE.best_unplaced) {
            if (board.getFilledCount() > best) {
                best = board.getFilledCount();
                invalidateAll();
            }
            return;
        }
        invalidateAll();
        //updateTile(x, y);
    }

    private void invalidateAll() {
        if (type == TYPE.best_unplaced) {
            drawUnplaced();
        } else {
            drawPlaced();
        }
    }

    private void drawUnplaced() {
        // Get all unplaced pieceIDs
        LinkedHashSet<Integer > pieceIDs = new LinkedHashSet<>(board.getWidth() * board.getHeight());
        for (int i = 0 ; i < board.getWidth()*board.getHeight() ; i++) {
            pieceIDs.add(i);
        }
        board.visitAllPlaced((x, y) -> {
            pieceIDs.remove(board.getPiece(x, y));
        });
        Iterator<Integer> pieceIDsI = pieceIDs.iterator();
        // Paint tiles for unplaced pieces
        for (int y = 0 ; y < board.getHeight() ; y++) {
            for (int x = 0 ; x < board.getWidth() ; x++) {
                BufferedImage tile = pieces.getBlank();
                if (pieceIDsI.hasNext()) {
                    tile = pieces.getPieceImage(pieceIDsI.next(), 0);
                }
                boardImage.getGraphics().drawImage(tile, x * edgeWidth, y * edgeHeight, null);
            }
        }
        if (boardDisplayComponent != null) {
            boardDisplayComponent.repaint(100L, 0, 0, board.getWidth()*edgeWidth, board.getHeight()*edgeHeight);
        }
    }

    private void drawPlaced() {
        if (overflow) {
            drawPlacedBRTL();
        } else {
            drawPlacedTLBR();
        }
    }

    /**
     * Draw from top left to lower right, limiting text to the concrete tile.
     */
    private void drawPlacedTLBR() {
        for (int y = 0 ; y < board.getHeight() ; y++) {
            for (int x = 0 ; x < board.getWidth() ; x++) {
                updateTile(x, y);
            }
        }
    }

    /**
     * Draw from lower right to top left, allowing text to overflow to next tile.
     */
    private void drawPlacedBRTL() {
        for (int y = board.getHeight()-1 ; y >= 0 ; y--) {
            for (int x = board.getWidth()-1 ; x >= 0 ; x--) {
                updateTile(x, y);
            }
        }
    }

    @Override
    public void boardChanged(int x, int y, String label) {
        labels[x][y] = label;
        if (type == TYPE.best || type == TYPE.best_unplaced) { // Update immediately on better
            if (board.getFilledCount() > best) {
                best = board.getFilledCount();
                invalidateAll();
            }
        } else {
            needsUpdate = true;
        }
    }

    @Override
    public void setText(int x, int y, String label) {
        labels[x][y] = label;
        needsUpdate = true;
    }

    private void updateTile(int x, int y) {
        BufferedImage tile = pieces.getBlank();
        int piece = board.getPiece(x, y);
        if (piece != EPieces.NULL_P) {
            int rotation = board.getRotation(x, y);
            tile = pieces.getPieceImage(piece, rotation);
        }
        boardImage.getGraphics().drawImage(tile, x * edgeWidth, y * edgeHeight, null);
        paintText(x, y, labels[x][y]);
        if (showPossiblePieces && piece == EPieces.NULL_P && board.getField(x, y).getOuterEdgeCount() != 0) {
            paintText(x, y, Integer.toString(board.getField(x, y).getBestPiecesNonRotating().size()));
        }
        if (boardDisplayComponent != null) {
            boardDisplayComponent.repaint(100L, x*edgeHeight, y*edgeHeight, edgeWidth, edgeHeight);
        }
    }

    private void paintText(int x, int y, String text) {
        if (text.isEmpty()) {
            return;
        }
        AttributedString attributedString = new AttributedString(text);
        attributedString.addAttribute(TextAttribute.FOREGROUND, Color.BLACK);
        attributedString.addAttribute(TextAttribute.BACKGROUND, Color.WHITE);
        attributedString.addAttribute(TextAttribute.SIZE, 12);
        boardImage.getGraphics().drawString(attributedString.getIterator(), x * edgeWidth + 3, y * edgeHeight + 30);
    }

    public boolean isOverflow() {
        return overflow;
    }

    public void setOverflow(boolean overflow) {
        this.overflow = overflow;
    }
}
