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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for creating and holding all quad bags.
 */
public class QuadBagHandler {
    private static final Logger log = LoggerFactory.getLogger(QuadBagHandler.class);

    private final PieceTracker pieceTracker; // 1 = available for use, 0 = positioned on the board

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

    public QuadBagHandler(PieceTracker pieceTracker) {
        this.pieceTracker = pieceTracker;
        // TODO: Share piece masker bitmap and processing between corners
        BAG_CORNER_NW = QuadCreator.createCorner(new QuadBag(pieceTracker, QuadBag.BAG_TYPE.corner_nw)).trim();
        BAG_CORNER_NE = BAG_CORNER_NW.rotClockwise();
        BAG_CORNER_NE.generateSets();
        BAG_CORNER_SE = BAG_CORNER_NE.rotClockwise();
        BAG_CORNER_SE.generateSets();
        BAG_CORNER_SW = BAG_CORNER_SE.rotClockwise();
        BAG_CORNER_SW.generateSets();

        // TODO: Share piece masker bitmap and processing between edges
        BAG_BORDER_N = QuadCreator.createEdges(new QuadBag(pieceTracker, QuadBag.BAG_TYPE.border_n)).trim();
        BAG_BORDER_E = BAG_BORDER_N.rotClockwise();
        BAG_BORDER_E.generateSets();
        BAG_BORDER_S = BAG_BORDER_E.rotClockwise();
        BAG_BORDER_S.generateSets();
        BAG_BORDER_W = BAG_BORDER_S.rotClockwise();
        BAG_BORDER_W.generateSets();

        BAG_CLUE_NW = QuadCreator.createClueNW(new QuadBag(pieceTracker, QuadBag.BAG_TYPE.clue_nw)).trim();
        BAG_CLUE_NE = QuadCreator.createClueNE(new QuadBag(pieceTracker, QuadBag.BAG_TYPE.clue_ne)).trim();
        BAG_CLUE_SE = QuadCreator.createClueSE(new QuadBag(pieceTracker, QuadBag.BAG_TYPE.clue_se)).trim();
        BAG_CLUE_SW = QuadCreator.createClueSW(new QuadBag(pieceTracker, QuadBag.BAG_TYPE.clue_sw)).trim();
        BAG_CLUE_C =  QuadCreator.createClueC( new QuadBag(pieceTracker, QuadBag.BAG_TYPE.clue_c)).trim();

        // TODO: Optimize by using createInnersNoQRot and invent new tricks
        BAG_INNER =  QuadCreator.createInners(new QuadBag(pieceTracker, QuadBag.BAG_TYPE.inner)).trim();
    }

    /**
     * Once assigned, the bags for fields never changes.
     */
    public void assignBagsToFields(QBoard board, QField[][] fields) {
        for (int x = 0 ; x <= 7 ; x++) {
            for (int y = 0 ; y <= 7 ;y++) {
                fields[x][y] = new QField(board, BAG_INNER, x, y);
            }
        }
        fields[0][0] = new QField(board, BAG_CORNER_NW, 0, 0);
        fields[7][0] = new QField(board, BAG_CORNER_NE, 7, 0);
        fields[7][7] = new QField(board, BAG_CORNER_SE, 7, 7);
        fields[0][7] = new QField(board, BAG_CORNER_SW, 0, 7);
        for (int t = 1 ; t <= 6 ; t++) {
            fields[t][0] = new QField(board, BAG_BORDER_N, t, 0);
            fields[t][7] = new QField(board, BAG_BORDER_S, t, 7);
            fields[0][t] = new QField(board, BAG_BORDER_W, 0, t);
            fields[7][t] = new QField(board, BAG_BORDER_E, 7, t);
        }
        fields[1][1] = new QField(board, BAG_CLUE_NW, 1, 1);
        fields[6][1] = new QField(board, BAG_CLUE_NE, 6, 1);
        fields[6][6] = new QField(board, BAG_CLUE_SE, 6, 6);
        fields[1][6] = new QField(board, BAG_CLUE_SW, 1, 6);
        fields[3][4] = new QField(board, BAG_CLUE_C,  3, 4);
    }
}
