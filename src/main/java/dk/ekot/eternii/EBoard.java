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

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * Eternity II board (flexible size)
 *
 * Principle:
 *
 * The board is made up of fields, holding pieces. Every field on the board has 4 edges; the board itself
 * also has an edge, which is called the board-edge.
 *
 * Edge requirements are handled by the {@link EdgeTracker}. At the start only the board-edges are registered as
 * requirements, each counting negative in the tracker. All free pieces are counted positive. When the edges of an
 * empty field or a free piece is tracked, all combinations of colored edges are registered.
 *
 * An empty field with 3 surrounding pieces with edges ABC are tracked as ABC, AV, BC, A-C, C-A, A, B, C where
 * A-C is opposing sides.
 * Duplicates are allowed for single-sides, so the field with edges ABA is tracked as ABA, AB, BA, A-A, A, B, A.
 *
 * A free piece is always tracked with all combinations:
 * ABCD -> ABCD, ABC, BCD, CDA, DAB, AB, BC, CD, DA, A-C, B-D, C-A, D-B, A, B, C, D
 *
 * When a piece is positioned at (x, y), the algorithm is as follows:
 * 1) For all free fields in the 9 fields from (x-1, y-1) to (x+1, y+1), update the edge tracker positively
 * 2) Position the piece on (x, y)
 * 3) For all free surrounding fields in the 8 fields from (x-1, y-1) to (x+1, y+1), excluding (x, y),
 *    update the edge tracker negatively
 * 4) Update the tracking of the edges for the (formerly) free piece negatively
 * 5) Remove the piece from the free collection
 * If at any point during #3 and #4 any tracker becomes negative, mark the attempt as failed
 * 6) If the attempt was failed, rollback the positioning and the tracking
 *
 * The field for the next piece is selected by locating the pieces surrounded by the most edges, then the field with
 * the least amount of matching pieces, then the top left free field.
 *
 * Known weakness:
 * Tracking all combinations for a piece leads to the same piece counting multiple times:
 * If the board has has fields with edges AB, CD and there is a piece ABCD, it will count both for AB and CD.
 */
public class EBoard {
    private static final Logger log = LoggerFactory.getLogger(EBoard.class);

    private final EPieces pieces;

    private final int width;
    private final int height;

    private final int[][] board; // (rotation << 16 | piece)
    private final EdgeTracker tracker = new EdgeTracker();

    public EBoard(EPieces pieces, int width, int height) {
        this.pieces = pieces;
        this.width = width;
        this.height = height;
        this.board = new int[width][height];
        for (int x = 0 ; x < width ; x++) {
            Arrays.fill(board[x], -1);
        }
        updateTrackerAll(-1);
    }

    /**
     * Positions the given piece on the board and throws IllegalArgumentexception if it is not a valid placement.
     */
    public boolean place(int x, int y, int piece, int rotation) {
        if (board[x][y] != -1) {
            throw new IllegalArgumentException("There is already a piece at (" + x + ", " + y + "): " + board[x][y]);
        }
        if (pieces.getType(piece) == EPieces.CORNER) {
            if (!isCorner(x, y)) {
                throw new IllegalArgumentException("Got a corner piece bit it is not placed at a corner");
            }
            if (x == 0 && y == 0) {
                if (pieces.getLeft(piece, rotation) != EPieces.EDGE_EDGE ||
                    pieces.getTop(piece, rotation) != EPieces.EDGE) {
                    throw new IllegalArgumentException(
                            "The corner piece " + piece + " at (" + x + ", " + y + ") does not face the corner");
                }
            }
        }
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private boolean isCorner(int x, int y) {
        return (x == 0 && y == 0) || (x == width-1 && y == 0) ||
               (x == 0 && y == height-1) || (x == width-1 && y == height-1);
    }

    /**
     * Positions the given piece on the board with no checks for validity.
     */
    public void placeFast(int x, int y, int piece, int rotation) {
        // Remove surrounding registers
        updateTracker9(x, y, -1);
        board[x][y] = (rotation<<16)|piece;
    }

    /**
     * Update the tracker for the fields at (origoX, origoY) and surrounding fields.
     * @param origoX start X.
     * @param origoY start Y.
     * @param delta the amount to update with (typically -1 or 1).
     */
    private void updateTracker9(int origoX, int origoY, int delta) {
        visit9(origoX, origoY, (x, y) -> {
            updateTracker(x, y, delta);
        });
    }

    /**
     * Update the tracker for the field (x, y).
     * @param delta the amount to update with (typically -1 or 1).
     */
    private void updateTracker(int x, int y, int delta) {
        if (board[x][y] != -1) { // No action if occupied
            return;
        }
        int topEdge = lenientGetBottomEdge(x, y-1);
        int rightEdge = lenientGetLeftEdge(x+1, y);
        int bottomEdge = lenientGetTopEdge(x+1, y);
        int leftEdge = lenientGetRightEdge(x-1, y);
        tracker.add(topEdge, rightEdge, bottomEdge, leftEdge, delta);
    }


    /**
     * Update the tracker for all fields.
     * @param delta the amount to update with (typically -1 or 1).
     */
    private void updateTrackerAll(int delta) {
        for (int y = 0 ; y < height ; y++) {
            for (int x = 0 ; x < width ; x++) {
                if (board[x][y] != -1) { // No action if occupied
                    return;
                }
                int topEdge = lenientGetBottomEdge(x, y-1);
                int rightEdge = lenientGetLeftEdge(x+1, y);
                int bottomEdge = lenientGetTopEdge(x, y+1);
                int leftEdge = lenientGetRightEdge(x-1, y);
//                if (topEdge != -1 || rightEdge != -1 || bottomEdge != -1 || leftEdge != -1) {
//                    System.out.println("(" + x + ", " + y + ") " + topEdge + " " + rightEdge + " " + bottomEdge + " " + leftEdge);
//                }
                tracker.add(topEdge, rightEdge, bottomEdge, leftEdge, delta);

            }
        }
    }

    /**
     * Requested edge of the piece at the given position, EPieces.EDGE_EDGE if outside of the board and -1 if no piece.
     */
    private int lenientGetTopEdge(int x, int y) {
        return y >= height ? EPieces.EDGE_EDGE :
                hasPiece(x, y) ? -1 : pieces.getTop(board[x][y]&0xFFFF, board[x][y]>>16);
    }
    /**
     * Requested edge of the piece at the given position, EPieces.EDGE_EDGE if outside of the board and -1 if no piece.
     */
    private int lenientGetRightEdge(int x, int y) {
        return x < 0 ? EPieces.EDGE_EDGE :
                hasPiece(x, y) ? -1 : pieces.getRight(board[x][y]&0xFFFF, board[x][y]>>16);
    }
    /**
     * Requested edge of the piece at the given position, EPieces.EDGE_EDGE if outside of the board and -1 if no piece.
     */
    private int lenientGetBottomEdge(int x, int y) {
        return y < 0 ? EPieces.EDGE_EDGE :
                hasPiece(x, y) ? -1 : pieces.getBottom(board[x][y]&0xFFFF, board[x][y]>>16);
    }
    /**
     * Requested edge of the piece at the given position, EPieces.EDGE_EDGE if outside of the board and -1 if no piece.
     */
    private int lenientGetLeftEdge(int x, int y) {
        return x >= width ? EPieces.EDGE_EDGE :
                hasPiece(x, y) ? -1 : pieces.getLeft(board[x][y]&0xFFFF, board[x][y]>>16);
    }

    private boolean hasPiece(int x, int y) {
        return x < 0 || x >= width || y < 0 || y >= height || board[x][y] == -1;
    }

    private void visit9(int origoX, int origoY, BiConsumer<Integer, Integer> visitor) {
        for (int vY = origoY-1 ; vY < origoY+1 ; vY++) {
            if (vY < 0 || vY >= height) {
                continue;
            }
            for (int vX = origoX-1 ; vX < origoY+1 ; vX++) {
                if (vX < 0 || vX >= width) {
                    continue;
                }
                visitor.accept(vX, vY);
            }
        }
    }

    // -1 = no piece
    public int getPiece(int x, int y) {
        final int compound = board[x][y];
        return compound == -1 ? -1 : compound & 0xFFFF;
    }

    // -1 = no piece
    public int getRotation(int x, int y) {
        final int compound = board[x][y];
        return compound == -1 ? -1 : compound>>16;
    }

    // Format from https://e2.bucas.name/
    // acdaaepcabteacsbacpcacocadtcaendaboeabgbacrbaencabveacvbadpcaabddsdapgmstmrgsutmplouomnlttkmusrtowmshtrwrvmtnwlmuwswvgwwllogbachdgcamtlgrgrttqvgosnqntkskkntrwpkmmrwrmhmmpjnlsnpsphswnspopprcabpcqealhwqruhhvtkunlktkpllnkpgprhkrkmnhvrkjshvnnnshhunsknhppskbaepeteawvwthnlvkvrnkvwvlwvvppvwhhrpmkqhrphkhtvpnqgtvntqnpgnsuvpeaeueqbawsoqlgosrusgwokuvntovoknrrmoqunrhlsuuqrlgqmqttlqgwstvwlweadwbhcaourhoujusuvukjhuttpjkvrtmqlvngwqsqugrpnqmonplnkosvpnlgvvdadgckfarvwkjnnvvhunhlghplulrlsllkilwhqkuqlhnqoqnouqkqmopqrqvprqdaepfhcawushnkouuhwkgimhuksismwkijjmqovjllwoorglujtrmlljrqwlrtrqeadtcrfasmtrorhmwvlrmujvsthuwhptjumhvgouwvjggsgvtopslugowovuriqodadifqeatjoqhhwjlsnhjprshjqppppjmiwpomnijovmggkopniggrinvmkrqhwmdadheseaoiiswquintmqrkgtqtjkplmtwoklnjrovvijkokviqwoiigqklqiwmuldabmelfaijjluhsjmwuhgsjwjgismkigkgikrjkgistjkqjswtqqgigtqooiuisobaeifufajsiussksujosjrijilpririlijurklqjthjljnthqwingiiwommisommeafofwdailiwkvulomjvinjmpgtniwtguiowqphijijptvviijnviggjmpqgmmupfaemdcaaidacufadjbafjbabtfabtdafofadhbafjfabvcafnfacgfafqfafubafeaab
    public void placeAll(String full) {
        if (full.length()/4 != width*height) {
            throw new IllegalArgumentException(
                    "Got full of length " + full.length() + " but expected " + width*height*4);
        }

        for (int y = 0 ; y < height ; y++) {
            for (int x = 0; x < width; x++) {
                int i = (y * width + x) * 4;
                int piece = pieces.getPieceFromString(full.substring(i, i + 4));
                int rotation = pieces.getRotationFromString(full.substring(i, i + 4));
                //System.out.println("G " + piece + " " + rotation + ": " + pieces.toDisplayString(piece, rotation));
                placeFast(x, y, piece, rotation);
            }
        }
    }

    public String getDisplayURL() {
        // https://e2.bucas.name/#puzzle=displayTest&board_w=16&board_h=16&board_edges=
        // acdaaepcabteacsbacpcacocadtcaendaboeabgbacrbaencabveacvbadpcaabddsdapgmstmrgsutmplouomnlttkmusrtowmshtrwrvmtnwlmuwswvgwwllogbachdgcamtlgrgrttqvgosnqntkskkntrwpkmmrwrmhmmpjnlsnpsphswnspopprcabpcqealhwqruhhvtkunlktkpllnkpgprhkrkmnhvrkjshvnnnshhunsknhppskbaepeteawvwthnlvkvrnkvwvlwvvppvwhhrpmkqhrphkhtvpnqgtvntqnpgnsuvpeaeueqbawsoqlgosrusgwokuvntovoknrrmoqunrhlsuuqrlgqmqttlqgwstvwlweadwbhcaourhoujusuvukjhuttpjkvrtmqlvngwqsqugrpnqmonplnkosvpnlgvvdadgckfarvwkjnnvvhunhlghplulrlsllkilwhqkuqlhnqoqnouqkqmopqrqvprqdaepfhcawushnkouuhwkgimhuksismwkijjmqovjllwoorglujtrmlljrqwlrtrqeadtcrfasmtrorhmwvlrmujvsthuwhptjumhvgouwvjggsgvtopslugowovuriqodadifqeatjoqhhwjlsnhjprshjqppppjmiwpomnijovmggkopniggrinvmkrqhwmdadheseaoiiswquintmqrkgtqtjkplmtwoklnjrovvijkokviqwoiigqklqiwmuldabmelfaijjluhsjmwuhgsjwjgismkigkgikrjkgistjkqjswtqqgigtqooiuisobaeifufajsiussksujosjrijilpririlijurklqjthjljnthqwingiiwommisommeafofwdailiwkvulomjvinjmpgtniwtguiowqphijijptvviijnviggjmpqgmmupfaemdcaaidacufadjbafjbabtfabtdafofadhbafjfabvcafnfacgfafqfafubafeaab
        StringBuilder sb = new StringBuilder();
        sb.append("https://e2.bucas.name/#puzzle=displayTest&board_w=").append(width)
                .append("&board_h=").append(height).append("&board_edges=");
        for (int y = 0; y < width; y++) {
            for (int x = 0; x < width; x++) {
                final int compound = board[x][y];
                sb.append(compound == -1 ?
                                  "aaaa" : // Empty
                                  pieces.toDisplayString(compound & 0xFFFF, compound>>16));
            }
        }
        return sb.toString();
    }

    public void displayBoard() {
        BufferedImage edge0 = pieces.getEdgeImage(0);
        final int edgeWidth = edge0.getWidth();
        final int edgeHeight = edge0.getHeight();
        BufferedImage boardImage = new BufferedImage(edgeWidth*width, edgeHeight*height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0 ; y < height ; y++) {
            for (int x = 0 ; x < width ; x++) {
                final int compound = board[x][y];
                if (compound == -1) {
                    continue;
                }
                int piece = compound&0xFFFF;
                int rot = compound>>16;
//                System.out.printf("Drawing (%d, %d) piece=%d, rotation=%d, non-rot=%s, rot=%s\n",
//                                  x, y, piece, rot, pieces.toDisplayString(piece, 0), pieces.toDisplayString(piece, rot));
                boardImage.getGraphics().drawImage(
                        pieces.getPieceImage(compound&0xFFFF, compound>>16), x*edgeWidth, y*edgeHeight, null);
            }
        }
        BaseGraphics.displayImage(boardImage);
    }

    public EdgeTracker getTracker() {
        return tracker;
    }
}
