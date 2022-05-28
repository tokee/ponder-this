package dk.ekot.eternii;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 *  Licensed under the Apache License", " Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing", " software
 *  distributed under the License is distributed on an "AS IS" BASIS", "
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND", " either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
public class EPiecesTest extends TestCase {
    String[] E2_PIECES = new String[]{"aabd", "aabe", "aacd", "aadc", "abgb", "abhc", "abjb", "abjf", "abmd", "aboe", "abpc", "abte", "abtf", "abve", "achb", "acid", "ackf", "acnf", "acoc", "acpc", "acqe", "acrb", "acrf", "acsb", "acvb", "adgc", "adgd", "adhd", "adid", "adof", "adpc", "adsd", "adtc", "adte", "aduf", "adwe", "aeib", "aelf", "aemf", "aenc", "aend", "aepb", "aepc", "aepd", "aeqb", "aese", "aete", "aeue", "afgf", "afhb", "afhc", "afjb", "afoe", "afqe", "afqf", "aftd", "afub", "afuf", "afvc", "afwd", "ggji", "ggko", "ghhl", "gigt", "giiw", "gikk", "gimh", "gisj", "giwt", "gllo", "glor", "gmki", "gmpq", "gmsp", "gmtl", "gnkp", "gnnp", "golu", "gosl", "gouv", "gpni", "gqii", "gqmq", "grin", "grjk", "grtr", "grus", "gsgv", "gsjw", "gsqu", "gtmr", "gtnp", "gtnq", "gtqv", "gtrk", "gvvl", "gwqn", "gwst", "gwvj", "gwwv", "hhrp", "hhru", "hhun", "hhwj", "hiqp", "hjlt", "hjnt", "hjqp", "hjum", "hkpr", "hkrp", "hlsn", "hlsu", "hmkq", "hmor", "hmrm", "hmwu", "hnlv", "hour", "hptw", "hqkw", "hsju", "hskn", "hssp", "htrw", "htvp", "hukj", "hunv", "huql", "hust", "hvjs", "hvrk", "hwku", "hwmq", "hwql", "hwus", "iiso", "ijjl", "ijjm", "ijjr", "ijnv", "ijpj", "ijur", "ijvv", "iklq", "ilir", "iliw", "illk", "ilpr", "injm", "inqw", "iomm", "iomn", "iowu", "iqoo", "iqor", "iqwo", "isou", "istj", "itvv", "iujs", "iuks", "iwpm", "iwqu", "jklq", "jkqt", "jmll", "jnmp", "jnnv", "joqt", "josu", "jovm", "jppp", "jprs", "jqov", "jron", "jskq", "jtru", "jttp", "juou", "jvmu", "jvom", "kknt", "klwo", "kmnr", "kmtt", "knvo", "kokv", "koln", "koun", "kpll", "kpps", "kqmo", "krvm", "krvw", "krwp", "ksmw", "ksnt", "ksss", "ktnl", "kuvt", "kuwo", "kvrn", "kvrt", "kvul", "kvwv", "llwo", "lmnw", "lmtp", "lomn", "loup", "lplu", "lqtt", "lrls", "lrqw", "lrwv", "lsnp", "luqr", "lvmq", "lwmu", "lwvv", "lwvw", "mmrw", "mmso", "mmup", "monp", "morr", "mqnt", "msow", "msut", "mtrs", "mtrv", "nnns", "nouq", "nqoq", "nqos", "nqrp", "nrqu", "nspw", "nsvp", "ntov", "ntqv", "oppr", "opst", "oqws", "ovuw", "ppvw", "pqrq", "prqv", "psuv", "qqwt", "qrtr", "rtus", "suvu", "swuw", "twvw"};
    String[] E2_PIECES_MOTIFS_ORDER_JEF = new String[]{"aaie", "aaiw", "aaqw", "aawq", "aebe", "aefi", "aegm", "aeje", "aeki", "aekq", "aekw", "aeom", "aete", "aeuq", "aeuw", "aevi", "aibe", "aibm", "aice", "aidi", "aidm", "aihi", "aikq", "aiow", "aipq", "aise", "ambw", "amce", "amdi", "amfe", "amfm", "amhm", "amji", "amjm", "ampi", "ampq", "amrw", "amsq", "aqcq", "aqfe", "aqkq", "aqlm", "aqni", "aqnm", "aqpi", "aqsi", "aqti", "aqum", "aqvw", "awbe", "awbq", "awcm", "awhq", "awhw", "awjm", "awkq", "awpw", "awre", "awtw", "awvw", "bbgf", "bbkd", "bblo", "bckt", "bcsu", "bdcf", "bdlf", "bdvt", "bffr", "bfsh", "bfsu", "bgho", "bhrt", "bhvh", "bhvr", "bjtn", "bkgo", "bljs", "bllu", "blsn", "bltu", "bnfn", "bnhn", "bnjd", "bnlh", "bnrp", "bnso", "bnto", "bofu", "bonh", "botj", "bpdg", "bpdu", "bpjt", "brpk", "brsr", "bskp", "bssv", "bufh", "bugl", "bukh", "ccvf", "cfrt", "cfuf", "cggr", "cgjh", "cgul", "chgg", "chhl", "cjdj", "cjfu", "cjkg", "cjnp", "cjsh", "cjul", "cjvt", "ckkn", "clfo", "clgr", "cljr", "clsl", "clus", "cnhg", "cnno", "cnpo", "cnvf", "cods", "coot", "coov", "coug", "couv", "crjv", "crot", "csdf", "csjr", "csod", "ctgh", "ctjd", "ctuf", "cudn", "cuko", "cvfr", "cvvt", "ddgv", "ddnv", "ddov", "dfkp", "dhrs", "dhvt", "djnv", "djop", "djpt", "dkdv", "dkkk", "dknt", "dlgf", "dlhn", "dogg", "dovu", "dpjl", "dppr", "drht", "dsoj", "dssv", "dtlf", "dtps", "dtvj", "duok", "dusv", "duus", "dvhh", "fgpj", "fgpr", "fgso", "fhfo", "fhok", "fjht", "fjun", "fjvr", "fkfn", "fkpv", "flrp", "fngj", "fnku", "fpol", "fpro", "frgn", "frvu", "fskn", "fuhr", "fvlg", "fvvh", "gglk", "gglv", "ghpp", "ghss", "gjgk", "gknv", "glsj", "gngt", "gnrs", "gour", "groj", "grsr", "grss", "gspu", "gtjp", "gtuk", "gtup", "gvnv", "gvrv", "hkuv", "hnjt", "hnvu", "holv", "hotk", "hrrs", "hsht", "hulk", "huuk", "hvll", "hvop", "hvvr", "jkoo", "jltv", "jpor", "jppn", "jprl", "jrtr", "jsjt", "jskt", "jtpr", "jupp", "jusp", "kksr", "kktl", "klnr", "knpl", "kovr", "kpln", "kppn", "kptt", "krut", "kuts", "lnso", "lnsr", "loun", "lpsn", "lsnu", "lsrs", "ltor", "lttt", "lupt", "nopo", "nroo", "tuuu"};
    String[] E2_PIECES_MOTIFS_ORDER_JBLACKWOOD = new String[]{"aabf", "aabr", "aajr", "aarj", "abcb", "abgb", "abgn", "abhf", "abif", "abin", "abkj", "ablr", "abpj", "abvf", "afdn", "afeb", "afif", "afln", "afpb", "afpj", "afpr", "afqf", "afsb", "aftj", "aftr", "afuf", "ajef", "ajhj", "ajkb", "ajmb", "ajmn", "ajon", "ajpj", "ajsr", "ajtn", "ajub", "ajvb", "ancn", "anef", "anen", "angb", "anhf", "anir", "ankb", "ankj", "anqb", "anqn", "anvj", "anwr", "arcj", "arcr", "arhn", "arif", "arij", "arkr", "arpj", "arqn", "arsr", "arur", "arwf", "ccgs", "ccoh", "cddh", "cdhm", "cele", "cess", "chdq", "chqv", "chud", "cics", "ciev", "cilm", "cimo", "cite", "citp", "ckkd", "clid", "clos", "clpe", "clup", "cmgo", "cmim", "cmqu", "cmst", "cpts", "cslk", "csoo", "cssw", "csug", "cswi", "ctop", "cttp", "cucv", "cueq", "cugw", "cvvd", "cwet", "cwui", "cwvg", "cwwv", "ddgl", "ddop", "ddos", "ddwh", "dego", "deii", "deso", "dhlt", "dhqp", "dikg", "dkqe", "dkwe", "dlip", "dltw", "dmdu", "dmew", "dmwv", "doit", "dovq", "dpdq", "dpms", "dqem", "dsgg", "dsms", "dsws", "dtoh", "duqk", "dutk", "dutp", "dvkt", "dvle", "dwho", "dwlq", "dwvv", "dwvw", "eewi", "eguo", "ehet", "ehhs", "ehms", "ehut", "ehvg", "eigh", "eigo", "eklo", "ekwl", "elho", "emep", "emim", "empt", "eowk", "epkg", "epks", "eqsw", "eqtm", "ethq", "etil", "evpm", "evti", "ewhs", "ewst", "ewuh", "ggls", "ggms", "ghuq", "ghvl", "giip", "gimq", "gkkw", "gkqo", "glst", "gmht", "gpgs", "gpmu", "gppp", "gqhq", "gqku", "gqlk", "gqms", "gsui", "gtik", "gtlp", "gttv", "gtvs", "gukv", "gusq", "gvhl", "gvlq", "gvvs", "hlls", "hllu", "hlts", "hmkl", "hmml", "hoqw", "hotv", "hovo", "hppm", "hpui", "hqmk", "hqsu", "hqto", "hssu", "htpl", "hvqw", "hvti", "hwlu", "hwqs", "iiol", "ikqu", "iluq", "imul", "imvl", "imwk", "ioot", "ioqv", "iout", "iovm", "iqum", "ivpk", "ivvs", "iwkp", "iwvw", "kkmp", "kkmq", "kkqt", "klml", "klwq", "komp", "kopm", "kqtv", "kuot", "kuup", "kvmo", "kwoq", "kwqu", "llmw", "llqp", "lomv", "lswp", "ltmo", "lwou", "mtov", "mvwo", "mwpo", "oppu", "ousq", "ouuu", "ovwv", "ppvw", "ptuv", "puqv", "pwtu", "quqv", "qwuw", "tttu"};

    final String TEDGES = "acdaaepcabteacsbacpcacocadtcaendaboeabgbacrbaencabveacvbadpcaabddsdapgmstmrgsutmplouomnlttkmusrtowmshtrwrvmtnwlmuwswvgwwllogbachdgcamtlgrgrttqvgosnqntkskkntrwpkmmrwrmhmmpjnlsnpsphswnspopprcabpcqealhwqruhhvtkunlktkpllnkpgprhkrkmnhvrkjshvnnnshhunsknhppskbaepeteawvwthnlvkvrnkvwvlwvvppvwhhrpmkqhrphkhtvpnqgtvntqnpgnsuvpeaeueqbawsoqlgosrusgwokuvntovoknrrmoqunrhlsuuqrlgqmqttlqgwstvwlweadwbhcaourhoujusuvukjhuttpjkvrtmqlvngwqsqugrpnqmonplnkosvpnlgvvdadgckfarvwkjnnvvhunhlghplulrlsllkilwhqkuqlhnqoqnouqkqmopqrqvprqdaepfhcawushnkouuhwkgimhuksismwkijjmqovjllwoorglujtrmlljrqwlrtrqeadtcrfasmtrorhmwvlrmujvsthuwhptjumhvgouwvjggsgvtopslugowovuriqodadifqeatjoqhhwjlsnhjprshjqppppjmiwpomnijovmggkopniggrinvmkrqhwmdadheseaoiiswquintmqrkgtqtjkplmtwoklnjrovvijkokviqwoiigqklqiwmuldabmelfaijjluhsjmwuhgsjwjgismkigkgikrjkgistjkqjswtqqgigtqooiuisobaeifufajsiussksujosjrijilpririlijurklqjthjljnthqwingiiwommisommeafofwdailiwkvulomjvinjmpgtniwtguiowqphijijptvviijnviggjmpqgmmupfaemdcaaidacufadjbafjbabtfabtdafofadhbafjfabvcafnfacgfafqfafubafeaab";

    public void testMapping() {
        EPieces pieces = EPieces.getEternii();
        for (String pieceStr: E2_PIECES) {
            assertNotSame(-1, pieces.getPieceFromString(pieceStr));
        }
    }

    public void testRecall() {
        // From https://e2.bucas.name/init.js


        final String oedges = "acdaaepcabteacsbacpcacocadtcaendaboeabgbacrbaencabveacvbadpcaabddsdapgmstmrgsutmplouomnlttkmusrtowmshtrwrvmtnwlmuwswvgwwllogbachdgcamtlgrgrttqvgosnqntkskkntrwpkmmrwrmhmmpjnlsnpsphswnspopprcabpcqealhwqruhhvtkunlktkpllnkpgprhkrkmnhvrkjshvnnnshhunsknhppskbaepeteawvwthnlvkvrnkvwvlwvvppvwhhrpmkqhrphkhtvpnqgtvntqnpgnsuvpeaeueqbawsoqlgosrusgwokuvntovoknrrmoqunrhlsuuqrlgqmqttlqgwstvwlweadwbhcaourhoujusuvukjhuttpjkvrtmqlvngwqsqugrpnqmonplnkosvpnlgvvdadgckfarvwkjnnvvhunhlghplulrlsllkilwhqkuqlhnqoqnouqkqmopqrqvprqdaepfhcawushnkouuhwkgimhuksismwkijjmqovjllwoorglujtrmlljrqwlrtrqeadtcrfasmtrorhmwvlrmujvsthuwhptjumhvgouwvjggsgvtopslugowovuriqodadifqeatjoqhhwjlsnhjprshjqppppjmiwpomnijovmggkopniggrinvmkrqhwmdadheseaoiiswquintmqrkgtqtjkplmtwoklnjrovvijkokviqwoiigqklqiwmuldabmelfaijjluhsjmwuhgsjwjgismkigkgikrjkgistjkqjswtqqgigtqooiuisobaeifufajsiussksujosjrijilpririlijurklqjthjljnthqwingiiwommisommeafofwdailiwkvulomjvinjmpgtniwtguiowqphijijptvviijnviggjmpqgmmupfaemdcaaidacufadjbafjbabtfabtdafofadhbafjfabvcafnfacgfafqfafubafeaab";

        //final String edges = "acdaaepcabteacsbacpcacocadtcaendaboeabgbacrbaencabveacvbadpcaabddsdapgmstmrgsutmplouomnlttkmusrtowmshtrwrvmtnwlmuwswvgwwllogbachdgcamtlgrgrttqvgosnqntkskkntrwpkmmrwrmhmmpjnlsnpsphswnspopprcabpcqealhwqruhhvtkunlktkpllnkpgprhkrkmnhvrkjshvnnnshhunsknhppskbaepeteawvwthnlvkvrnkvwvlwvvppvwhhrpmkqhrphkhtvpnqgtvntqnpgnsuvpeaeueqbawsoqlgosrusgwokuvntovoknrrmoqunrhlsuuqrlgqmqttlqgwstvwlweadwbhcaourhoujusuvukjhuttpjkvrtmqlvngwqsqugrpnqmonplnkosvpnlgvvdadgckfarvwkjnnvvhunhlghplulrlsllkilwhqkuqlhnqoqnouqkqmopqrqvprqdaepfhcawushnkouuhwkgimhuksismwkijjmqovjllwoorglujtrmlljrqwlrtrqeadtcrfasmtrorhmwvlrmujvsthuwhptjumhvgouwvjggsgvtopslugowovuriqodadifqeatjoqhhwjlsnhjprshjqppppjmiwpomnijovmggkopniggrinvmkrqhwmdadheseaoiiswquintmqrkgtqtjkplmtwoklnjrovvijkokviqwoiigqklqiwmuldabmelfaijjluhsjmwuhgsjwjgismkigkgikrjkgistjkqjswtqqgigtqooiuisobaeifufajsiussksujosjrijilpririlijurklqjthjljnthqwingiiwommisommeafofwdailiwkvulomjvinjmpgtniwtguiowqphijijptvviijnviggjmpqgmmupfaemdcaaidacufadjbafjbabtfabtdafofadhbafjfabvcafnfacgfafqfafubafeaab";
        // Clues are in JEF-order
        final String clueEdgesJeff = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaargouaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaartrjaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaavddoaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaajdsoaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaafsknaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        //final String edges = "rgourtrjvddojdsofskn";
        //final String edges = "ruog";
        EPieces pieces = EPieces.getEternii();
        List<String> e2 = Arrays.asList(E2_PIECES_MOTIFS_ORDER_JEF);
        for (int i = 0 ; i < 256 ; i++) {
            String pieceStr = clueEdgesJeff.substring(i*4, i*4+4);
            if ("aaaa".equals(pieceStr)) {
                continue;
            }
            System.out.println("x=" + i%16 + " y=" + i/16 + " i=" + i + ": clues=" + pieceStr + " originals=" + oedges.substring(i*4, i*4+4));
            for (int r = 0 ; r < 4 ; r++) {
                if (e2.contains(pieceStr)) {
                    System.out.println(" rot=" + r + ", E2-Piece: " + e2.indexOf(pieceStr) + " (" + e2.get(e2.indexOf(pieceStr)) + ")");
                }
                pieceStr = pieceStr.charAt(3) + pieceStr.substring(0, 3);
            }
            continue;
/*            for (int r = 0 ; r < 4 ; r++) {
                assertNotSame("Piece '" + pieceStr + "' should be recallable"", "
                              -1", " pieces.getPieceFromString(pieceStr));
                pieceStr = pieceStr.substring(1) + pieceStr.charAt(0);
            }*/
        }
    }
}