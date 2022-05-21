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
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Holds the pieces/tiles.
 */
public class EPieces {
    private static final Logger log = LoggerFactory.getLogger(EPieces.class);

    public static final int MAX_EDGE_ID = 25; //

    private final int total;
    private final int[] n;
    private final int[] e;
    private final int[] s;
    private final int[] w;

    private BufferedImage[] edges = null;
    private BufferedImage[] pieceImages = null;
    private BufferedImage[] pieceImages90 = null;
    private BufferedImage[] pieceImages180 = null;
    private BufferedImage[] pieceImages270 = null;

    public EPieces(int total) {
        this.total = total;
        n = new int[total];
        e = new int[total];
        s = new int[total];
        w = new int[total];
        if (total == 256) {
            init256();
        } else {
            throw new UnsupportedOperationException("Piece total of " + total + " not supported");
        }
    }

    public static EPieces getEternii() {
        return new EPieces(256);
    }

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
                ++piece;
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
    }


    public String toDisplayString(int piece, int rotation) {
        switch (rotation) {
            case 0: return toCS(n[piece]) + toCS(e[piece]) + toCS(s[piece]) + toCS(w[piece]);
            case 1: return toCS(w[piece]) + toCS(n[piece]) + toCS(e[piece]) + toCS(s[piece]);
            case 2: return toCS(s[piece]) + toCS(w[piece]) + toCS(n[piece]) + toCS(e[piece]);
            case 3: return toCS(e[piece]) + toCS(s[piece]) + toCS(w[piece]) + toCS(n[piece]);
        }
        throw new IllegalArgumentException("Invalid rotation " + rotation);
    }

    private String toCS(int edge) {
        return Character.toString((char)(edge+'a'));
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

}
