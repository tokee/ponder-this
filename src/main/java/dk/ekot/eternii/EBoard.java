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

/**
 * Eternity II board (flexible size)
 */
public class EBoard {
    private static final Logger log = LoggerFactory.getLogger(EBoard.class);

    private final EPieces pieces;

    private final int width;
    private final int height;

    private final int[][] board; // (rotation << 16 | piece)



    public EBoard(EPieces pieces, int width, int height) {
        this.pieces = pieces;
        this.width = width;
        this.height = height;
        this.board = new int[width][height];
        for (int x = 0 ; x < width ; x++) {
            Arrays.fill(board[x], -1);
        }
    }

    /**
     * Positions the given piece on the board and returns true if it was a valid placement (edges matches).
     */
    public boolean place(int x, int y, int piece, int rotation) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Positions the given piece on the board with no checks for validity.
     */
    public void placeFast(int x, int y, int piece, int rotation) {
        board[x][y] = (rotation<<16)|piece;
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
                System.out.printf("Drawing (%d, %d) piece=%d, rotation=%d, non-rot=%s, rot=%s\n",
                                  x, y, piece, rot, pieces.toDisplayString(piece, 0), pieces.toDisplayString(piece, rot));
                boardImage.getGraphics().drawImage(
                        pieces.getPieceImage(compound&0xFFFF, compound>>16), x*edgeWidth, y*edgeHeight, null);
            }
        }
        BaseGraphics.displayImage(boardImage);
    }

}
