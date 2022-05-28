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
import java.awt.image.BufferedImage;

/**
 * Opens a windows showing an {@link EBoard} and tracks changes.
 */
public class BoardVisualiser implements EBoard.Observer {
    private static final Logger log = LoggerFactory.getLogger(BoardVisualiser.class);

    public static final int UPDATE_INTERVAL = 100; // Every 100 ms
    
    private final EBoard board;
    private final EPieces pieces;
    private final JComponent boardDisplayComponent;
    private final BufferedImage boardImage;
    private final int edgeWidth;
    private final int edgeHeight;
    private final boolean onlyUpdateOnBetter;
    private boolean needsUpdate = false;

    private int best = 0;

    public BoardVisualiser(EBoard board) {
        this(board, false);
    }

    public BoardVisualiser(EBoard board, boolean onlyUpdateOnBetter) {
        this.board = board;
        this.onlyUpdateOnBetter = onlyUpdateOnBetter;
        pieces = board.getPieces();

        BufferedImage edge0 = pieces.getEdgeImage(0);
        edgeWidth = edge0.getWidth();
        edgeHeight = edge0.getHeight();
        boardImage = new BufferedImage(edgeWidth*board.getWidth(), edgeHeight*board.getHeight(), BufferedImage.TYPE_INT_RGB);
        invalidateAll();
        boardDisplayComponent = BaseGraphics.displayImage(boardImage);

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
        board.registerObserver(this);
    }

    private void invalidateConditionally() {
        if (!needsUpdate) {
            return;
        }
        if (onlyUpdateOnBetter) {
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
        for (int y = 0 ; y < board.getHeight() ; y++) {
            for (int x = 0 ; x < board.getWidth() ; x++) {
                updateTile(x, y);
            }
        }
    }

    @Override
    public void boardChanged(int x, int y) {
        needsUpdate = true;
    }

    private void updateTile(int x, int y) {
        BufferedImage tile = pieces.getBlank();
        int piece = board.getPiece(x, y);
        if (piece != -1) {
            int rotation = board.getRotation(x, y);
            tile = pieces.getPieceImage(piece, rotation);
        }
        boardImage.getGraphics().drawImage(tile, x * edgeWidth, y * edgeHeight, null);
        if (boardDisplayComponent != null) {
            boardDisplayComponent.repaint(100L, x*edgeHeight, y*edgeHeight, edgeWidth, edgeHeight);
        }
    }

}
