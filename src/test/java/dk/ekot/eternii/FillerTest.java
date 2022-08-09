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

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Takes a partly solved puzzle and fills the blanks, even when they produce an illegal solution.
 */
public class FillerTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(FillerTest.class);

    private static final Pattern BUCAS_PIECES = Pattern.compile(".*board_edges=([a-z]*).*");

    public void testValidFiller() {
        // teg: 245 https://e2.bucas.name/#puzzle=ThomasEgense&board_w=16&board_h=16&board_edges=adcaaendabteabgbacvbadtcadhdadidafwdafufafgfaelfafqeacrfaencaabecocanjrottpjgwstvwtwtgiwhhlgiqphwtqqusrtgrusluqrqnourqunnqoqbaeqcrbarmorpjnmskqjtnlkiomnlnkopgtnqmqgrwmmuiowqwoiovuwusuvoqwseacqbveaomjvnlomqttllgmtmkigkntktksnqtjkmsutowmsokuwuhwkukjhwhqkcabhepbajijpommitplmmmupijjmtrujsjprjlijugolmspguhlswqlhjklqqhmkbafhbpcajpppmiwpllkiulpljmllulwmprilijjroqtjpqrqlvmqlrwvlslrmtrsfabtckfapllkwvwlkokvpprolsnpwuwsisoujgistgigrtrgmqntwlrqloglrhmobachfoeallwowvvlkvwvrkhvnvokwkrvoggkiiwgigqirjkgnthjrkgtgikkmhgicafhemfawksmvrtkwhtrhunhoujurijuggjiwwvgquiwksiuhwusgsjwkppsgnkpfacnfjbasiujtvvitkuvnhskjumhjvmujgwvvvlgijvvistjuvpsjqovphjqkprhcadpboeaunkovhunusthsphsmonpmmsowuhmlkvuvrnktrqrphkrourhjosuriqodaciepdakrwpuhhrtvphhnlvnnsnspwnhhrpvjshnnvjqosnkqmorpnqstopqvntcafvdgcawqnghwmqppvwloupslgowoklrglosqugvgtqsgvgmpqgnsvpoiisnqwifafqcsbanhlsmrmhvmtraaaagtnqaaaaaaaaaaaaaaaaaaaaqiklaaaaijnvaaaafadubjbaaaaamnwlaaaalhuqaaaailirthjlsjuhovmjntovkmttrgtmnnpgigpndadgbdaasdadweadteaeueaeseaeibaejfabubafmdabofadtdafteadpcaepcacdaac

        String G2_197 = "https://e2.bucas.name/#puzzle=displayTest&board_w=16&board_h=16&board_edges=abdaafhbackfacpcabhcabgbabjbacsbaencadweadgdaendafoeaelfaeueaabedsdahvjskvwvpsuvhwusgiiwjjlisiujngriwvjggtqvnqgtovjqlrwvujtrbafjdpcajnmpwlmnulpluqrliklqlqjkuiwqriliaaaaaaaagsjwjuhswswutgwsfafgcrbamorrmjvopppjropplnkojminwksmlwokaaaaaaaajrijhourwmsowuhmfadubjfaronjvntopmonpqgmkwhqiliwsuhloujuaaaaaaaaiigquksisntkhhundadhfqeangwqtmrgokqmgrjkhmoriwpmhtrwjkqtaaaaaaaaghhlssphtopsuisodacieqbaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaahjqpprsjpnqrspwncabpbtfaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaqorislgoqttlwhptbachfufamhjukrphwpkrloupmmsohmrmijjmaaaaaaaaaaaarkmngtrktkmtpgnkcadgfvcajnnvpgnnkoggunkosnnnrqunjskqaaaaaaaaaaaamkigrvmkmtrvnpgtdaepcrfankvrnvokgsgvksssnhlsaaaaaaaaaaaaaaaaaaaaijurmlljrlslgvvleabvfwdavwtwokuwgikksoiillwoaaaaaaaaaaaaaaaaaaaauvtklwvvsoqwvuwobafudidatvviusuvknhsiomnwuioaaaaaaaaaaaaaaaaaaaattpjvphtqrqpwmmrfaemdofavmjoujvmhhwjmkqhillkaaaaaaaaaaaaaaaaaaaaphiqhruhqvprmqlveacqfhcajlthvhnlwmqhqntmlktnaaaaaaaaaaaaaaaaaaaailprugolpniglomncacocdaatcadnfacqfaftdafteadteaeseaepcaevbacteabpbaeoeabibaemdabcaad";

        // With duplicate reduction
        String G2_202 = "Attempts: 2875124K, placed=155|202, att/sec=110K, possible=2e+59 best=https://e2.bucas.name/#puzzle=displayTest&board_w=16&board_h=16&board_edges=abdaafhbackfacpcabhcabgbabjbacsbaencadweadgdaendafoeaelfaeueaabedsdahvjskvwvpsuvhwusgiiwjjlisiujngriwvjggtqvnqgtovjqlrwvujtrbafjdpcajnmpwlmnulpluqrliklqlqjkuiwqrilijnviqwingsjwjuhswswutgwsfafgcrbamorrmjvopppjropplnkojminwksmlwokaaaaaaaajrijhourwmsowuhmfadubjfaronjvntopmonpqgmkwhqiliwsuhloujuaaaaaaaaiigquksisntkhhundadhfqeangwqtmrgokqmgrjkhmoriwpmhtrwjkqtaaaaaaaaghhlssphtopsuisodacieqbaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaahjqpprsjpnqrspwncabpbtfaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaqorislgoqttlwhptbachfufamhjuhrphwpkrloupmmsohmrmijjmaaaaaaaaaaaarkmngtrktkmtpgnkcadgfvcajnnvpgnnkoggunkosnnnrqunjskqaaaaaaaaaaaamkigrvmkmtrvnpgtdaepcrfankvrnvokgsgvksssnhlsaaaaaaaaaaaaaaaaaaaaijurmlljrlslgvvleabvfwdavwtwokuwgikksoiillwoaaaaaaaaaaaaaaaaaaaauvtklwvvsoqwvuwobafudidatvviusuvknhsiomnwuioaaaaaaaaaaaaaaaaaaaattpjvphtqrqpwmmrfaemdofavmjoujvmhhwjmkqhillkaaaaaaaaaaaaaaaaaaaaphiqhruhqvprmqlveacqfhcajlthvhnlwmqhqntmlktnppskvwppkrvwaaaaaaaailprugolpniglomncacocdaatcadnfacqfaftdafteadseaepcaevbacteabteaepbaeoeabibaemdabcaad";

        // No duplicate reduction
        String G2_207 = "https://e2.bucas.name/#puzzle=displayTest&board_w=16&board_h=16&board_edges=abdaafhbackfacocacpcabhcacrbadgcadgdaepdacqeaencadteabmdaeibaabedsdahvjskvwvomjvpjnmhhwjrphhgnkpgrinpnqrqgtnnnpgtmqnmrwmijjrbafjdhdajnthwlmnjmllnlomaaaaaaaaaaaaiomnqnqotksnprhkqunrwswujgisfafgdweatwvwmsowlrlsonjraaaaaaaaaaaamwksqngwspwnhiqpnqwiwsoqiujsfaduepbavwppoiqwlirijnviaaaaaaaaaaaakigmgqiiwlrqqiklwtgiovntjnnvdaenbjfapppjqgmprusgvgouaaaaaaaaaaaagoluiqoortrqkuvtgsqunqosnouqeabofqfaphjqmrmhsmtrorrmaaaaaaaaaaaaaaaaaaaarwpkvwlwqlhworglujtrbabjfufajosummiottkmrkgtaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaatvvibacvfubasuvuiowuaaaaaaaaaaaaaaaaijjmaaaaaaaaaaaaaaaaaaaaaaaavvlgcafvbveavijvwiliaaaaaaaaaaaaaaaaaaaavlrwhuqlsrtutrgrvmkrtlgmloglfadoeseajprsllkpghhlvrkhuhhraaaaaaaarkmnqmoktrvmgtmrkvrtgwwvgiiwdadietearwhtkuhwhsjukppshsspaaaaaaaamsutouisvpsumonprhmowushiwqudafweueahmwuhjumjlijplulsnhlaaaaaaaauksiillksuhlnvhumjovskqjqtjkfabtelfawmulujvmijpjurijhouraaaaaaaasksslqjkhwmqhqkworiqqvprjgwvbabgfhcausthvpnspropilpruqrlwtqqtlqtsnpljminmupmkjhuistjpgmswstgbacscdaatcadnfacoeafpcaerfacqeafqbaepcabidacpcadhbacteabmfaetdafcaad";
        // Filled: 234 https://e2.bucas.name/#puzzle=displayTest&board_w=16&board_h=16&board_edges=abdaafhbackfacocacpcabhcacrbadgcadgdaepdacqeaencadteabmdaeibaabedsdahvjskvwvomjvpjnmhhwjrphhgnkpgrinpnqrqgtnnnpgtmqnmrwmijjrbafjdhdajnthwlmnjmllnlomwoklaaaakntkiomnqnqotksnprhkqunrwswujgisfafgdweatwvwmsowlrlsonjrkvrngtqvaaaamwksqngwspwnhiqpnqwiwsoqiujsfaduepbavwppoiqwlirijnviaaaaaaaaimhgkigmgqiiwlrqqiklwtgiovntjnnvdaenbjfapppjqgmprusgvgousgvggkogaaaagoluiqoortrqkuvtgsqunqosnouqeabofqfaphjqmrmhsmtrorrmvwkrollwkvullwvvokuwrwpkvwlwqlhworglujtrbabjfufajosummiottkmrkgtaaaalktnaaaavmqlaaaapnigaaaahkrpgikktvvibacvfubasuvuiowuknvoaaaaggjiaaaaijjmqovjaaaaiisoaaaaaaaaaaaavvlgcafvbveavijvwiliaaaatqvnaaaaoslgjwgsvlrwhuqlsrtutrgrvmkrtlgmloglfadoeseajprsllkpghhlvrkhuhhraaaaaaaarkmnqmoktrvmgtmrkvrtgwwvgiiwdadietearwhtkuhwhsjukppshsspknhsaaaamsutouisvpsumonprhmowushiwqudafweueahmwuhjumjlijplulsnhlhhunaaaauksiillksuhlnvhumjovskqjqtjkfabtelfawmulujvmijpjurijhouraaaapstosksslqjkhwmqhqkworiqqvprjgwvbabgfhcausthvpnspropilpruqrlwtqqtlqtsnpljminmupmkjhuistjpgmswstgbacscdaatcadnfacoeafpcaerfacqeafqbaepcabidacpcadhbacteabmfaetdafcaad
        // Still running after a night

        fillValids(G2_207, WalkerG2::new, BacktrackSolver::new);
    }

    private void fillValids(
            String bucasURL, Function<EBoard, Walker> walkerFactory, BiFunction<EBoard, Walker, Runnable> solverFactory) {
        EBoard board = loadBoard(bucasURL);
        new BoardVisualiser(board);

        Walker walker = walkerFactory.apply(board);
        Runnable solver = solverFactory.apply(board, walker);
        long runTime = -System.currentTimeMillis();
        solver.run();
        runTime += System.currentTimeMillis();
        System.out.printf("Done. Placed %d pieces in %.2f seconds\n", board.getFilledCount(), runTime/1000.0);
        try {
            Thread.sleep(1000000000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
