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

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Takes a partly solved puzzle and fills the blanks, even when they produce an illegal solution.
 */
public class FillerTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(FillerTest.class);

    private static final Pattern BUCAS_PIECES = Pattern.compile(".*board_edges=([a-z]*).*");

    public void testFiller() throws InterruptedException {
       String G2_197 = "https://e2.bucas.name/#puzzle=displayTest&board_w=16&board_h=16&board_edges=abdaafhbackfacpcabhcabgbabjbacsbaencadweadgdaendafoeaelfaeueaabedsdahvjskvwvpsuvhwusgiiwjjlisiujngriwvjggtqvnqgtovjqlrwvujtrbafjdpcajnmpwlmnulpluqrliklqlqjkuiwqriliaaaaaaaagsjwjuhswswutgwsfafgcrbamorrmjvopppjropplnkojminwksmlwokaaaaaaaajrijhourwmsowuhmfadubjfaronjvntopmonpqgmkwhqiliwsuhloujuaaaaaaaaiigquksisntkhhundadhfqeangwqtmrgokqmgrjkhmoriwpmhtrwjkqtaaaaaaaaghhlssphtopsuisodacieqbaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaahjqpprsjpnqrspwncabpbtfaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaqorislgoqttlwhptbachfufamhjukrphwpkrloupmmsohmrmijjmaaaaaaaaaaaarkmngtrktkmtpgnkcadgfvcajnnvpgnnkoggunkosnnnrqunjskqaaaaaaaaaaaamkigrvmkmtrvnpgtdaepcrfankvrnvokgsgvksssnhlsaaaaaaaaaaaaaaaaaaaaijurmlljrlslgvvleabvfwdavwtwokuwgikksoiillwoaaaaaaaaaaaaaaaaaaaauvtklwvvsoqwvuwobafudidatvviusuvknhsiomnwuioaaaaaaaaaaaaaaaaaaaattpjvphtqrqpwmmrfaemdofavmjoujvmhhwjmkqhillkaaaaaaaaaaaaaaaaaaaaphiqhruhqvprmqlveacqfhcajlthvhnlwmqhqntmlktnaaaaaaaaaaaaaaaaaaaailprugolpniglomncacocdaatcadnfacqfaftdafteadteaeseaepcaevbacteabpbaeoeabibaemdabcaad";
       EBoard board = loadBoard(G2_197);
       new BoardVisualiser(board);
       Thread.sleep(10000000L);
    }

    private EBoard loadBoard(String bucasURL) {
        Matcher m = BUCAS_PIECES.matcher(bucasURL);
        if (!m.matches()) {
            throw new IllegalArgumentException("Unable to extract pieces from '" + bucasURL + "'");
        }
        String bucasPieces = m.group(1);

        EPieces pieces = EPieces.getEternii();
        EBoard board = new EBoard(pieces, 16, 16);
        board.registerFreePieces(pieces.getBag());

        int x = 0;
        int y = 0;
        for (int i = 0 ; i < bucasPieces.length() ; i+=4) {
            String bc = bucasPieces.substring(i, i+4);
            if (!"aaaa".equals(bc)) {
                int piece = pieces.getPieceFromString(bc);
                int rotation = pieces.getRotationFromString(bc);
                board.placePiece(x, y, piece, rotation);
            }
            x++;
            if (x == board.getWidth()) {
                x = 0;
                y++;
            }
        }
        return board;
    }
}
