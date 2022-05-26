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

    private final EBoard board;
    private final EPieces pieces;
    private final JComponent boardDisplayComponent;
    private final BufferedImage boardImage;
    private final int edgeWidth;
    private final int edgeHeight;

    public BoardVisualiser(EBoard board) {
        this.board = board;
        pieces = board.getPieces();

        BufferedImage edge0 = pieces.getEdgeImage(0);
        edgeWidth = edge0.getWidth();
        edgeHeight = edge0.getHeight();
        boardImage = new BufferedImage(edgeWidth*board.getWidth(), edgeHeight*board.getHeight(), BufferedImage.TYPE_INT_RGB);
        invalidateAll();
        boardDisplayComponent = BaseGraphics.displayImage(boardImage);
        board.registerObserver(this);
    }

    private void invalidateAll() {
        for (int y = 0 ; y < board.getHeight() ; y++) {
            for (int x = 0 ; x < board.getWidth() ; x++) {
                boardChanged(x, y);
            }
        }
    }

    @Override
    public void boardChanged(int x, int y) {
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
