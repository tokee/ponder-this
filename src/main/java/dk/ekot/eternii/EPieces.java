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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Holds the pieces/tiles.
 */
public class EPieces {
    private static final Logger log = LoggerFactory.getLogger(EPieces.class);

    public static final int CORNER = 2;
    public static final int EDGE = 1;
    public static final int INNER = 0;

    public static final int EDGE_EDGE = 0; // The outermost edges (black/grey)
    public static final long NULL_E = 31;  // No edge
    public static final long NULL_P = 256; // No piece
    public static final long QNULL_E = 1023;  // No quad edge (2^10-1)
    public static final long QNULL_P = 1048575; // No quad piece (2^20-1)

    private final int total;
    private final int[] n;
    private final int[] e;
    private final int[] s;
    private final int[] w;
    private final int[] type; // 0=base, 1=edge, 2=corner
    private final long[][] rotEdges; // rot, pieceID

    // Holds pieces (not compounds)
    private final Set<Integer> bag = new HashSet<>();

    private BufferedImage[] edges = null;

    private BufferedImage[] pieceImages = null;
    private BufferedImage[] pieceImages90 = null;
    private BufferedImage[] pieceImages180 = null;
    private BufferedImage[] pieceImages270 = null;
    private BufferedImage blank = null;

    public EPieces(int total) {
        this.total = total;
        n = new int[total];
        e = new int[total];
        s = new int[total];
        w = new int[total];
        type = new int[total];
        rotEdges = new long[4][total];
        if (total == 256) {
            init256();
        } else {
            throw new UnsupportedOperationException("Piece total of " + total + " not supported");
        }
    }

    public static EPieces getEternii() {
        return new EPieces(256);
    }

    /**
     * Delivers clue pieces from https://e2.bucas.name/#puzzle=Clues&board_w=16&board_h=16&board_edges=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaargouaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaartrjaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaavddoaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaajdsoaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaafsknaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa&board_pieces=000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000208000000000000000000000000000000255000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000139000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000181000000000000000000000000000000249000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000&motifs_order=jef
     * TODO: Check that they are removed from the bag when delivered
     */
    public void processEterniiClues(ClueCallback clueCallback) {
        clueCallback.clue(7, 8, getPieceFromString("ijjm"), getRotationFromString("ijjm")); // Center
        clueCallback.clue(2, 2, getPieceFromString("wlmn"), getRotationFromString("wlmn")); // Top left
        clueCallback.clue(13, 2, getPieceFromString("wswu"), getRotationFromString("wswu")); // Top right
        clueCallback.clue(2, 13, getPieceFromString("ujvm"), getRotationFromString("ujvm")); // Bottom left
        clueCallback.clue(13, 13, getPieceFromString("qvpr"), getRotationFromString("qvpr")); // Bottom right
        final String realclues = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaargouaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaartrjaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaavddoaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaajdsoaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaafsknaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        final String clues = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaartrjaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaavddoaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaajdsoaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaafsknaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    }
    public void processEterniiCornerClues(ClueCallback clueCallback) {
        clueCallback.clue(2, 2, getPieceFromString("wlmn"), getRotationFromString("wlmn")); // Top left
        clueCallback.clue(13, 2, getPieceFromString("wswu"), getRotationFromString("wswu")); // Top right
        clueCallback.clue(2, 13, getPieceFromString("ujvm"), getRotationFromString("ujvm")); // Bottom left
        clueCallback.clue(13, 13, getPieceFromString("qvpr"), getRotationFromString("qvpr")); // Bottom right
    }

    /**
     * @return edges in the {@code 4*<5 bits>} format (piece location in bits) from {@link EBits}.
     */
    public long getEdges(int piece, int rotation) {
        return rotEdges[rotation][piece];
    }
    public long getEdgesCalculate(int piece, int rotation) {
        // TODO: Consider precalculating all of this and simply return it
        long edges = 0;
        edges = EBits.setPieceNorthEdge(edges, getTop(piece, rotation));
        edges = EBits.setPieceEastEdge(edges, getRight(piece, rotation));
        edges = EBits.setPieceSouthEdge(edges, getBottom(piece, rotation));
        return EBits.setPieceWestEdge(edges, getLeft(piece, rotation));
    }


    public long getEdgesAsBase(int piece, int rotation) {
        // TODO: Consider precalculating all of this and simply return it
        return getEdges(piece, rotation) >> EBits.PIECE_EDGES_SHIFT;
    }

    public String edgeToString(int edge) {
        return edge == NULL_E ? "N/A" : Character.toString((char)('a' + edge));
    }

    @FunctionalInterface
    public interface ClueCallback {
        void clue(int x, int y, int piece, int rotation);
    }

    // 4x4 sample
    // https://e2.bucas.name/#puzzle=Sample_4x4&board_w=4&board_h=4&board_edges=ajjaajojaetjaaeejoeaootottooeajteojattoootttjajtjeaaoeaeteaejaae&board_pieces=000001002003004005006007008009010011012013014015&board_types=bccbcddccddcbccb

    // Clues
    // https://e2.bucas.name/#puzzle=Clues&board_w=16&board_h=16&board_edges=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaargouaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaartrjaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaavddoaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaajdsoaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaafsknaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa&board_pieces=000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000208000000000000000000000000000000255000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000139000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000181000000000000000000000000000000249000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000&motifs_order=jef

    // https://e2.bucas.name/#puzzle=Joshua_Blackwood_468&board_w=16&board_h=16&board_edges=acdaaepcabteacsbacpcacocadtcaendaboeabgbacrbaencabveacvbadpcaabddsdapgmstmrgsutmplouomnlttkmusrtowmshtrwrvmtnwlmuwswvgwwllogbachdgcamtlgrgrttqvgosnqntkskkntrwpkmmrwrmhmmpjnlsnpsphswnspopprcabpcqealhwqruhhvtkunlktkpllnkpgprhkrkmnhvrkjshvnnnshhunsknhppskbaepeteawvwthnlvkvrnkvwvlwvvppvwhhrpmkqhrphkhtvpnqgtvntqnpgnsuvpeaeueqbawsoqlgosrusgwokuvntovoknrrmoqunrhlsuuqrlgqmqttlqgwstvwlweadwbhcaourhoujusuvukjhuttpjkvrtmqlvngwqsqugrpnqmonplnkosvpnlgvvdadgckfarvwkjnnvvhunhlghplulrlsllkilwhqkuqlhnqoqnouqkqmopqrqvprqdaepfhcawushnkouuhwkgimhuksismwkijjmqovjllwoorglujtrmlljrqwlrtrqeadtcrfasmtrorhmwvlrmujvsthuwhptjumhvgouwvjggsgvtopslugowovuriqodadifqeatjoqhhwjlsnhjprshjqppppjmiwpomnijovmggkopniggrinvmkrqhwmdadheseaoiiswquintmqrkgtqtjkplmtwoklnjrovvijkokviqwoiigqklqiwmuldabmelfaijjluhsjmwuhgsjwjgismkigkgikrjkgistjkqjswtqqgigtqooiuisobaeifufajsiussksujosjrijilpririlijurklqjthjljnthqwingiiwommisommeafofwdailiwkvulomjvinjmpgtniwtguiowqphijijptvviijnviggjmpqgmmupfaemdcaaidacufadjbafjbabtfabtdafofadhbafjfabvcafnfacgfafqfafubafeaab
    private void init256First() {
        final String edges = "acdaaepcabteacsbacpcacocadtcaendaboeabgbacrbaencabveacvbadpcaabddsdapgmstmrgsutmplouomnlttkmusrtowmshtrwrvmtnwlmuwswvgwwllogbachdgcamtlgrgrttqvgosnqntkskkntrwpkmmrwrmhmmpjnlsnpsphswnspopprcabpcqealhwqruhhvtkunlktkpllnkpgprhkrkmnhvrkjshvnnnshhunsknhppskbaepeteawvwthnlvkvrnkvwvlwvvppvwhhrpmkqhrphkhtvpnqgtvntqnpgnsuvpeaeueqbawsoqlgosrusgwokuvntovoknrrmoqunrhlsuuqrlgqmqttlqgwstvwlweadwbhcaourhoujusuvukjhuttpjkvrtmqlvngwqsqugrpnqmonplnkosvpnlgvvdadgckfarvwkjnnvvhunhlghplulrlsllkilwhqkuqlhnqoqnouqkqmopqrqvprqdaepfhcawushnkouuhwkgimhuksismwkijjmqovjllwoorglujtrmlljrqwlrtrqeadtcrfasmtrorhmwvlrmujvsthuwhptjumhvgouwvjggsgvtopslugowovuriqodadifqeatjoqhhwjlsnhjprshjqppppjmiwpomnijovmggkopniggrinvmkrqhwmdadheseaoiiswquintmqrkgtqtjkplmtwoklnjrovvijkokviqwoiigqklqiwmuldabmelfaijjluhsjmwuhgsjwjgismkigkgikrjkgistjkqjswtqqgigtqooiuisobaeifufajsiussksujosjrijilpririlijurklqjthjljnthqwingiiwommisommeafofwdailiwkvulomjvinjmpgtniwtguiowqphijijptvviijnviggjmpqgmmupfaemdcaaidacufadjbafjbabtfabtdafofadhbafjfabvcafnfacgfafqfafubafeaab";
        for (int i = 0 ; i < 256 ; i++) {
            n[i] = edges.charAt(i*4)-'a';
            e[i] = edges.charAt(i*4+1)-'a';
            s[i] = edges.charAt(i*4+2)-'a';
            w[i] = edges.charAt(i*4+3)-'a';
        }
    }

    private void init256() {
        // Maps edges from e2pieces.txt to online viewer format
        final String MAP = "glqj" + "othm" + "rkpu" + "insv" + "wbec" + "fda";
        try (InputStream in = Thread.currentThread().getContextClassLoader().
                getResourceAsStream("eternii/e2pieces.txt");
             InputStreamReader sin = new InputStreamReader(in, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(sin)) {
            String line;
            int piece = 0;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                // 22 22 21 19
                String[] tokens = line.split(" +");
                if (tokens.length != 4) {
                    throw new IllegalStateException("Line '" + line + "' could not be parsed");
                }
                n[piece] = MAP.charAt(Integer.parseInt(tokens[0]))-'a';
                e[piece] = MAP.charAt(Integer.parseInt(tokens[1]))-'a';
                s[piece] = MAP.charAt(Integer.parseInt(tokens[2]))-'a';
                w[piece] = MAP.charAt(Integer.parseInt(tokens[3]))-'a';
                type[piece] = (n[piece] == 0 ? 1 : 0) + (e[piece] == 0 ? 1 : 0) + (s[piece] == 0 ? 1 : 0) + (w[piece] == 0 ? 1 : 0);
                bag.add(piece);
                ++piece;
            }
            // Pre-calculate rotated edges
            for (int rot = 0 ; rot < 4 ; rot++) {
                for (int pID = 0 ; pID < 256 ; pID++) {
                    rotEdges[rot][pID] = getEdgesCalculate(pID, rot);
                }
            }

            edges = new BufferedImage[MAP.length()];
            for (int i = 0 ; i < MAP.length() ; i++) {
                try (InputStream imgIn = Thread.currentThread().getContextClassLoader().
                        getResourceAsStream("eternii/i" + i + ".png")) {
                    edges[MAP.charAt(i) - 'a'] = ImageIO.read(imgIn);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to load piece eternii/i" + i + ".png");
                }
            }
            generatePieceImages();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load pieces information", e);
        }
/*        final String edges = "acdaaepcabteacsbacpcacocadtcaendaboeabgbacrbaencabveacvbadpcaabddsdapgmstmrgsutmplouomnlttkmusrtowmshtrwrvmtnwlmuwswvgwwllogbachdgcamtlgrgrttqvgosnqntkskkntrwpkmmrwrmhmmpjnlsnpsphswnspopprcabpcqealhwqruhhvtkunlktkpllnkpgprhkrkmnhvrkjshvnnnshhunsknhppskbaepeteawvwthnlvkvrnkvwvlwvvppvwhhrpmkqhrphkhtvpnqgtvntqnpgnsuvpeaeueqbawsoqlgosrusgwokuvntovoknrrmoqunrhlsuuqrlgqmqttlqgwstvwlweadwbhcaourhoujusuvukjhuttpjkvrtmqlvngwqsqugrpnqmonplnkosvpnlgvvdadgckfarvwkjnnvvhunhlghplulrlsllkilwhqkuqlhnqoqnouqkqmopqrqvprqdaepfhcawushnkouuhwkgimhuksismwkijjmqovjllwoorglujtrmlljrqwlrtrqeadtcrfasmtrorhmwvlrmujvsthuwhptjumhvgouwvjggsgvtopslugowovuriqodadifqeatjoqhhwjlsnhjprshjqppppjmiwpomnijovmggkopniggrinvmkrqhwmdadheseaoiiswquintmqrkgtqtjkplmtwoklnjrovvijkokviqwoiigqklqiwmuldabmelfaijjluhsjmwuhgsjwjgismkigkgikrjkgistjkqjswtqqgigtqooiuisobaeifufajsiussksujosjrijilpririlijurklqjthjljnthqwingiiwommisommeafofwdailiwkvulomjvinjmpgtniwtguiowqphijijptvviijnviggjmpqgmmupfaemdcaaidacufadjbafjbabtfabtdafofadhbafjfabvcafnfacgfafqfafubafeaab";
        for (int i = 0 ; i < 256 ; i++) {
            n[i] = edges.charAt(i*4)-'a';
            e[i] = edges.charAt(i*4+1)-'a';
            s[i] = edges.charAt(i*4+2)-'a';
            w[i] = edges.charAt(i*4+3)-'a';
        }      */
    }

    private void generatePieceImages() {
        pieceImages = new BufferedImage[total];
        pieceImages90 = new BufferedImage[total];
        pieceImages180 = new BufferedImage[total];
        pieceImages270 = new BufferedImage[total];

        for (int i = 0 ; i < total ; i++) {
            BufferedImage pieceImage =
                    new BufferedImage(edges[0].getWidth(), edges[0].getHeight(), BufferedImage.TYPE_INT_RGB);
            pieceImage.getGraphics().drawImage(edges[n[i]], 0, 0, null);
            pieceImage.getGraphics().drawImage(BaseGraphics.rotate90(edges[e[i]]), 0, 0, null);
            pieceImage.getGraphics().drawImage(BaseGraphics.rotate180(edges[s[i]]), 0, 0, null);
            pieceImage.getGraphics().drawImage(BaseGraphics.rotate270(edges[w[i]]), 0, 0, null);
            pieceImages[i] = pieceImage;
            pieceImages90[i] = BaseGraphics.rotate90(pieceImage);
            pieceImages180[i] = BaseGraphics.rotate180(pieceImage);
            pieceImages270[i] = BaseGraphics.rotate270(pieceImage);
        }
        blank = new BufferedImage(edges[0].getWidth(), edges[0].getHeight(), BufferedImage.TYPE_INT_RGB);
        blank.getGraphics().setColor(Color.BLACK);
        blank.getGraphics().fillRect(0, 0, blank.getWidth()-1, blank.getHeight()-1);
    }

    public String toDisplayString(int piece, int rotation) {
        switch (rotation) {
            case 0: return toCS(n[piece]) + toCS(e[piece]) + toCS(s[piece]) + toCS(w[piece]);
            case 1: return toCS(w[piece]) + toCS(n[piece]) + toCS(e[piece]) + toCS(s[piece]);
            case 2: return toCS(s[piece]) + toCS(w[piece]) + toCS(n[piece]) + toCS(e[piece]);
            case 3: return toCS(e[piece]) + toCS(s[piece]) + toCS(w[piece]) + toCS(n[piece]);
            default: throw new IllegalArgumentException("Invalid rotation " + rotation);
        }
    }

    private static String toCS(int edge) {
        return Character.toString((char)(edge+'a'));
    }

    public BufferedImage getBlank() {
        return blank;
    }

    public BufferedImage getEdgeImage(int edge) {
        return edges[edge];
    }

    public BufferedImage getPieceImage(int piece, int rotation) {
        switch (rotation) {
            case 0: return pieceImages[piece];
            case 1: return pieceImages90[piece];
            case 2: return pieceImages180[piece];
            case 3: return pieceImages270[piece];
        }
        throw new IllegalArgumentException("Invalid rotation " + rotation);
    }

    /**
     * Resolve piece from String format (4 characters) from https://e2.bucas.name/
     */
    public int getPieceFromString(String pieceStr) {
        for (int piece = 0 ; piece < total ; piece++) {
            for (int rotation = 0 ; rotation < 4 ; rotation++) {
                if (pieceStr.equals(toDisplayString(piece, rotation))) {
                    return piece;
                }
            }
        }
        throw new IllegalStateException("Unable to resolve '" + pieceStr + "'");
    }
    public int getRotationFromString(String pieceStr) {
        for (int piece = 0 ; piece < total ; piece++) {
            for (int rotation = 0 ; rotation < 4 ; rotation++) {
                if (pieceStr.equals(toDisplayString(piece, rotation))) {
                    return rotation;
                }
            }
        }
        throw new IllegalStateException("Unable to resolve '" + pieceStr + "'");
    }

    public IntStream allPieces() {
        return IntStream.range(0, total);
    }

    public Set<Integer> getBag() {
        return bag;
    }

    public void putInBag(int piece){
        bag.add(piece);
    }

    public int getType(int piece) {
        return type[piece];
    }

    public int getLeft(int piece, int rotation) {
        switch (rotation) {
            case 0: return w[piece];
            case 1: return s[piece];
            case 2: return e[piece];
            case 3: return n[piece];
            default: throw new IllegalArgumentException("Invalid rotation " + rotation);
        }
    }
    public int getTop(int piece, int rotation) {
        switch (rotation) {
            case 0: return n[piece];
            case 1: return w[piece];
            case 2: return s[piece];
            case 3: return e[piece];
            default: throw new IllegalArgumentException("Invalid rotation " + rotation);
        }
    }
    public int getRight(int piece, int rotation) {
        switch (rotation) {
            case 0: return e[piece];
            case 1: return n[piece];
            case 2: return w[piece];
            case 3: return s[piece];
            default: throw new IllegalArgumentException("Invalid rotation " + rotation);
        }
    }
    public int getBottom(int piece, int rotation) {
        switch (rotation) {
            case 0: return s[piece];
            case 1: return e[piece];
            case 2: return n[piece];
            case 3: return w[piece];
            default: throw new IllegalArgumentException("Invalid rotation " + rotation);
        }
    }
}
