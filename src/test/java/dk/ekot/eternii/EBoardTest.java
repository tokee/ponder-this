package dk.ekot.eternii;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.ibm.icu.impl.ValidIdentifiers.Datatype.x;

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
public class EBoardTest extends TestCase {

    public void DtestDisplayBoardAll() throws InterruptedException {
        EBoard board = new EBoard(EPieces.getEternii(), 16, 16);
        for (int y = 0 ; y < 16 ; y++) {
            for (int x = 0 ; x < 16 ; x++) {
                board.placeUntrackedPiece(x, y, y * 16 + x, 0);
            }
        }
        new BoardVisualiser(board);
        Thread.sleep(100000L);
    }

    public void DtestDisplayBoardSample() throws InterruptedException {
        EBoard board = new EBoard(EPieces.getEternii(), 16, 16);
        board.placeUntrackedAll("acdaaepcabteacsbacpcacocadtcaendaboeabgbacrbaencabveacvbadpcaabddsdapgmstmrgsutmplouomnlttkmusrtowmshtrwrvmtnwlmuwswvgwwllogbachdgcamtlgrgrttqvgosnqntkskkntrwpkmmrwrmhmmpjnlsnpsphswnspopprcabpcqealhwqruhhvtkunlktkpllnkpgprhkrkmnhvrkjshvnnnshhunsknhppskbaepeteawvwthnlvkvrnkvwvlwvvppvwhhrpmkqhrphkhtvpnqgtvntqnpgnsuvpeaeueqbawsoqlgosrusgwokuvntovoknrrmoqunrhlsuuqrlgqmqttlqgwstvwlweadwbhcaourhoujusuvukjhuttpjkvrtmqlvngwqsqugrpnqmonplnkosvpnlgvvdadgckfarvwkjnnvvhunhlghplulrlsllkilwhqkuqlhnqoqnouqkqmopqrqvprqdaepfhcawushnkouuhwkgimhuksismwkijjmqovjllwoorglujtrmlljrqwlrtrqeadtcrfasmtrorhmwvlrmujvsthuwhptjumhvgouwvjggsgvtopslugowovuriqodadifqeatjoqhhwjlsnhjprshjqppppjmiwpomnijovmggkopniggrinvmkrqhwmdadheseaoiiswquintmqrkgtqtjkplmtwoklnjrovvijkokviqwoiigqklqiwmuldabmelfaijjluhsjmwuhgsjwjgismkigkgikrjkgistjkqjswtqqgigtqooiuisobaeifufajsiussksujosjrijilpririlijurklqjthjljnthqwingiiwommisommeafofwdailiwkvulomjvinjmpgtniwtguiowqphijijptvviijnviggjmpqgmmupfaemdcaaidacufadjbafjbabtfabtdafofadhbafjfabvcafnfacgfafqfafubafeaab");
        //board.placeAll("acdaaacddaaccdaaacpcacocadtcaendaboeabgbacrbaencabveacvbadpcaabddsdapgmstmrgsutmplouomnlttkmusrtowmshtrwrvmtnwlmuwswvgwwllogbachdgcamtlgrgrttqvgosnqntkskkntrwpkmmrwrmhmmpjnlsnpsphswnspopprcabpcqealhwqruhhvtkunlktkpllnkpgprhkrkmnhvrkjshvnnnshhunsknhppskbaepeteawvwthnlvkvrnkvwvlwvvppvwhhrpmkqhrphkhtvpnqgtvntqnpgnsuvpeaeueqbawsoqlgosrusgwokuvntovoknrrmoqunrhlsuuqrlgqmqttlqgwstvwlweadwbhcaourhoujusuvukjhuttpjkvrtmqlvngwqsqugrpnqmonplnkosvpnlgvvdadgckfarvwkjnnvvhunhlghplulrlsllkilwhqkuqlhnqoqnouqkqmopqrqvprqdaepfhcawushnkouuhwkgimhuksismwkijjmqovjllwoorglujtrmlljrqwlrtrqeadtcrfasmtrorhmwvlrmujvsthuwhptjumhvgouwvjggsgvtopslugowovuriqodadifqeatjoqhhwjlsnhjprshjqppppjmiwpomnijovmggkopniggrinvmkrqhwmdadheseaoiiswquintmqrkgtqtjkplmtwoklnjrovvijkokviqwoiigqklqiwmuldabmelfaijjluhsjmwuhgsjwjgismkigkgikrjkgistjkqjswtqqgigtqooiuisobaeifufajsiussksujosjrijilpririlijurklqjthjljnthqwingiiwommisommeafofwdailiwkvulomjvinjmpgtniwtguiowqphijijptvviijnviggjmpqgmmupfaemdcaaidacufadjbafjbabtfabtdafofadhbafjfabvcafnfacgfafqfafubafeaab");
        new BoardVisualiser(board);
        Thread.sleep(1000000L);
    }

    public void testRotationCorner() {
        EBoard board = new EBoard(EPieces.getEternii(), 16, 16);
        {
            EBoard.Field field = board.getField(0, 0); // Top left corner
            assertEquals("The rotation of the corner piece should match",
                         3, field.getValidRotation(0));
        }
        {
            EBoard.Field field = board.getField(1, 15); // Bottom edge
            assertEquals("The rotation of the edge piece should match",
                         2, field.getValidRotation(4));
        }
    }

    public void testRotationEdge() {
        EBoard board = new EBoard(EPieces.getEternii(), 16, 16);
        {
            EBoard.Field field = board.getField(1, 15); // Bottom edge
            assertEquals("The rotation of the edge piece should match",
                         2, field.getValidRotation(4));
        }
    }

    public void testDisplayBoardRandom() throws InterruptedException {
        EBoard board = new EBoard(EPieces.getEternii(), 16, 16);
        new BoardVisualiser(board);
        Random r = new Random(87);
        for (int i = 0 ; i < 256 ; i++) {
            board.placeUntrackedPiece(r.nextInt(16), r.nextInt(16), r.nextInt(256), r.nextInt(4));
            Thread.sleep(100);
        }
        Thread.sleep(1000000L);
    }

    public void testBlankTracker() {
        EBoard board = new EBoard(EPieces.getEternii(), 16, 16);
        assertEquals("There should be the expected number of counts for the corner pieces",
                     -4, board.getEdgeTracker().getTwo(EPieces.EDGE_EDGE, EPieces.EDGE_EDGE));
        assertEquals("There should be the expected number of counts for the board edge pieces",
                     -4*16, board.getEdgeTracker().getOne(EPieces.EDGE_EDGE));
    }
    public void testBaggedTracker() {
        EPieces pieces = EPieces.getEternii();
        EBoard board = new EBoard(pieces, 16, 16);
        board.registerFreePieces(pieces.getBag());
        assertEquals("There should be the expected number of counts for the corner pieces",
                     0, board.getEdgeTracker().getTwo(EPieces.EDGE_EDGE, EPieces.EDGE_EDGE));
        assertEquals("There should be the expected number of counts for the board edge pieces",
                     0, board.getEdgeTracker().getOne(EPieces.EDGE_EDGE));
    }
}