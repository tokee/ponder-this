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
package dk.ekot.misc;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * http://stevensnedker.com/ord.htm
 * https://dsn.dk/retskrivning/om-retskrivningsordbogen/ro-elektronisk-og-som-bog
 * Direct: https://dsn.dk/retskrivning/om-retskrivningsordbogen/RO2012.opslagsord.med.homnr.og.ordklasse.zip
 * Unzip and
 * cat RO2012.opslagsord.med.homnr.og.ordklasse.txt | cut -d';' -f1 | sed 's/^[0-9][.] //' | grep -v "^-" | tr '[:upper:]' '[:lower:]' > RO2012.cleaned.txt
 *
 * wget https://www.stednavneudvalget.ku.dk/autoriserede_stednavne/AUTORISEREDE_STEDNAVNE_v.2013_05_23.csv
 * iconv -f iso-8859-1 -t UTF-8 < AUTORISEREDE_STEDNAVNE_v.2013_05_23.csv | cut -d';' -f1 | grep -v " " | sort | uniq > stednavne.txt
 *
 * https://ast.dk/born-familie/navne/navnelister/godkendte-fornavne
 * wget 'https://ast.dk/_namesdb/export/names?format=xls&gendermask=1' -O pigenavne.xsl
 * wget 'https://ast.dk/_namesdb/export/names?format=xls&gendermask=2' -O drengenavne.xsl
 * sudo libreoffice --headless --convert-to csv pigenavne.xsl --outdir ./
 * sudo libreoffice --headless --convert-to csv drengenavne.xsl --outdir ./
 * iconv -f iso-8859-1 -t UTF-8 < pigenavne.csv > pigenavne_utf8.txt
 * iconv -f iso-8859-1 -t UTF-8 < drengenavne.csv > drengenavne_utf8.txt
 *
 * Limited:
 * cat RO2012.opslagsord.med.homnr.og.ordklasse.txt | grep -E ";.*(sb|adj|præfiks)" | cut -d';' -f1 | sed -e 's/^[0-9][.] //' -e 's/-$//' | grep -v "^-" | tr '[:upper:]' '[:lower:]' > RO2012.navneord.txt
 */

// daghøjskole_quiz_vært_bmx_fyn_på_wc
// våbenhjælp_quiz_tysk_fax_god_mør_wc -> god mør tysk våbenhjælp wc quiz fax
// håndvask_boxer_fløjt_pygmæ_quiz_wc -> pygmæ_boxer_fløjt_quiz_wc_håndvask

// quizshows_nykåret_bælg_mjød_fax_pvc
// patchwork_bjæf_quiz_lyn_møg_sex_våd
// kæmpehøj_growl_quiz_snyd_båt_fax_cv -> kæmpehøj_growl_quiz_snyd_båt_cvfax
// quizshowvært_pcfax_kåbe_lyng_mjød -> lyngmjød_quizshowvært_pckåbe_fax
// quizshowvært_pcfax_kåbe_løn_myg_dj -> djmygkåbe_quizshowvært_lønpcfax
// quizshowvært_pcfax_kåbe_myg_nøl_dj
// høstmåned_cvfax_growl_pjæk_quiz_by -> høstmåned_bygrowl_pjækcvfax_quiz

// knæler_pvcfax_quizdj_møgby_show_tå
// dragshow_jævnmål_quiz_bøf_tyk_ex_pc

/* våbenhjælp_modtryk_quiz_fax_gøs_wc
våbenhjælp_trykfod_quiz_møg_sax_wc
mordvåben_højtysk_pægl_quiz_fax_wc
bådværft_hjamsk_oxygen_quiz_pøl_wc
bådværft_hjamsk_epoxy_løgn_quiz_wc
båndhøvl_skjorte_pygmæ_quiz_fax_wc
fjervægt_håndkøb_olymp_quiz_sax_wc
håndvask_boxer_fløjt_pygmæ_quiz_wc
håndværk_zygote_squaw_fløj_mix_pcb
nødhjælp_skovmår_byget_quiz_fax_wc
xylograf_håndkøb_pjevs_quiz_mæt_wc
blåtryk_jævnhed_opsmøg_quiz_fax_wc
bygværk_hjemlån_opstød_quiz_fax_wc
*/
@SuppressWarnings("ForLoopReplaceableByForEach")
public class ShortWordWithAllLetters {
    private static final int MAX_REDUCE_DEPTH = 3;
    private static Log log = LogFactory.getLog(ShortWordWithAllLetters.class);

    public static final String[] SOURCES = new String[]{
            //"RO2012.cleaned.txt", "stednavne.txt", "pigenavne_utf8.txt", "drengenavne_utf8.txt",
            "RO2012.navneord.txt"};

    public static final String VALID_CHARS = "abcdefghijklmnopqrstuvwxyzæøå";
    public static final Set<String> ILLEGAL_WORDS = new HashSet<>(Arrays.asList(
            ("a,b,c,d,e,f,g,h,j,k,l,m,n,o,p,q,r,t,u,v,w,x,y,z,æ," +
             "ov,fx,vy,kuf,zweck,pø," +
             "stup,sufi,sura,syld,sågu,søgn,tren,varp,wadi,ætan,air,ais,alk,amu,aps,ard," +
             "arg,aud,bah,bel,beo,bni,but,bæh,cad,cam,ces,chi,cis,dab,dao,erg,fer,ah,ba," +
             "bs,cg,db,dg,ea,ep,fa,fe,fi,gb,gu,hd, hf,hr,iq,kb,kj,kv,kw,mf,mi,mt,ni,ol," +
             "pa,tb,uf,kwh,nyk,såh,åsø,kås,påbøl,wen,new,qu,az,paz,lex,påby,ståby,påø," +
             "wenxi,zevin,xhyla,qixuan,ziw,zofja,quy,ydø,pax,pog,kjove,youqi,yvon,zuwa,gy," +
             "zema,ytje,howa,luqa,qalb,vasebækgård,kåg,nyx,pom,øhop,vrå,dany,nga,yok,wan," +
             "wah,val,joly,kwam,may,que,qi,hoax,yon,nya,bex,manje,quyen,wenja,anqi,femja," +
             "mpv,paræz,quynh,fjandø,grydnæs,fragå,fay,årh,væn,xing,laf,løvhang,jay,vakd," +
             "nlp,balqis,ishwan,asløg,bjørg,iqrah,iqra,crawl,rødå,ruqia,råhøj,bølå,drap,hap" +
             "pal,åbøl,xi,hl,ph,wrap,ådal,zi,la,ziv,hg,jah,ziv,qing,sng,zu,yngva,åvang,nåby," +
             "ishwaq,qudsia,sælvig,valdhøj,iqbal,iglsø,vælig,alshøj,lisz,ahl,høl,hopsø,mixo,omø," +
             "kovad,wagon,yvona,kady,ngoy,ynwa,yang,skovbølgård,yuqing,qiong,judy,vajd,fæby," +
             "mæby,pcb,my,båke,gytje," +
             // Only disabled temporarily to find better sentence candidates
             "wc,brynjeklædt,højbrynet,højspændt,dybgrøn,bælgmørk,gråklædt,hjælpsom,hårdkogt," +
             "lysegrøn,go,klejn,bleg,nej,bælgmørke,håndgjort,mørkeblå,opskræmt,jysk,lom,ør," +
             "ly,mør,løj,blå,hygsom,håbløs,grå,mødj,få,blødkogt,grådkvalt,"
            ).split(" *, *")));
    public static final Set<String> CUSTOM_WORDS = new HashSet<>(Arrays.asList(
            ("iglesø,croquis,s,en,er,quizshow,quizshow,quizshowvært,e,pcen,pcs,cvs,djmix,cvfax," +
             "pvcfax,pvcmås,våbenhjælpsquiz,møgby,møgbydj,jeg,quizshowværten,hævnlyster," +
             "quizbowle,mødj,quizdj,boghjemlån,pchjemlån,mjødbowle,ex,exgæst,exdj,eksdj," +
             "exquizshowvært,exquizshowværtklan,exquizshowværtpc,,exquizshowværtklanpc," +
             "møgbydjfan,exquizshowværtcamp,faq,iq,quartz,squash,laviq,højiq,megaiq,topiq"
            ).split(" *, *")));


    public static final long[] CODEPOINT_BITS;
    public static final long WANTED_CODE;
    static {
        char max = 0;
        for (char c: VALID_CHARS.toCharArray()) {
            max = (char) Math.max(max, c);
        }
        CODEPOINT_BITS = new long[max+1];

        long wantedCode = 0L;
        long bit = 1L;
        for (char c: VALID_CHARS.toCharArray()) {
            CODEPOINT_BITS[c] = bit;
            wantedCode |= bit;
            bit = bit << 1;
        }
        WANTED_CODE = wantedCode;
    }

    public static void main(String[] args) throws IOException {
        version1(7);

        // mælkebøvs
        // højspændt_kålorm
        //seeded("mælkebøvs", 89);
    }

    private static void seeded(String seed, int maxWords) throws IOException {
        final CodeWord seedCodeWord = new CodeWord(seed);

        List<CodeWord> codeWords = new ArrayList<>(loadCodeWords());
        sort(codeWords);
        codeWords = reduce(codeWords, 0, seedCodeWord);

        Set<String> candidates = new HashSet<>();
        fillCandidates(codeWords, 0, 1, MAX_REDUCE_DEPTH, maxWords, seedCodeWord.code, seedCodeWord.word, candidates);
    }

    private static void printAll() throws IOException {
        int count = 0;
        List<CodeWord> codeWords = new ArrayList<>(loadCodeWords());
        sort(codeWords);
        for (CodeWord codeWord: codeWords) {
            System.out.print(", " + codeWord.word);
            if (count++ % 20 == 0) {
                System.out.println();
            }
        }
    }


    private static void version1(int maxWords) throws IOException {
        final List<CodeWord> codeWords = new ArrayList<>(loadCodeWords());
        sort(codeWords);

        System.out.println("Got " + codeWords.size() + " words");
        Set<String> candidates = new HashSet<>();
        System.out.println(
                "|--------------------------------------------------------------------------------------------------|");
        fillCandidates(codeWords, 0, 0, MAX_REDUCE_DEPTH, maxWords, 0L, "", candidates);
    }

    private static void sort(List<CodeWord> codeWords) {
        codeWords.sort((o1, o2) -> {
            if (o1.word.length() > o2.word.length()) {
                return -1;
            }
            if (o1.word.length() < o2.word.length()) {
                return 1;
            }
            return o1.word.compareTo(o2.word);
        });
    }

    private static void version1_2(int maxWords) throws IOException {
        final List<CodeWord> codeWords = new ArrayList<>(loadCodeWords());
        sort(codeWords);

        System.out.println("Got " + codeWords.size() + " words");
        Set<String> candidates = new HashSet<>();
        fillCandidates1_2(codeWords, maxWords, candidates);
    }


    private static Set<CodeWord> loadCodeWords() throws IOException {
        final Set<String> words = loadUniqueLetterwords();
        final Set<CodeWord> codeWords = new HashSet<>(words.size());
        for (String word: words) {
            codeWords.add(new CodeWord(word));
        }
        return codeWords;
    }

    private static List<Long> toWordCodes(List<String> words) {
        List<Long> wordCodes = new ArrayList<>(words.size());
        for (int i = 0 ; i < words.size() ; i++) {
            wordCodes.add(getWordCode(words.get(i)));
        }
        return wordCodes;
    }

    private static class CodeWord {
        private final String word;
        private final long code;

        public CodeWord(String word) {
            this.word = word;
            this.code = getWordCode(word);
        }

        @Override
        public int hashCode() {
            return word.hashCode();
        }
    }

    private static void fillCandidates(
            List<CodeWord> codeWords, int index, int depth, int maxReduceDepth, int maxWords, long wordCode, String prefix,
            Set<String> candidates) {
        int every = codeWords.size()/100+999999;
        int next = every;
        for (int i = index ; i < codeWords.size(); i++) {
/*            if (depth == 0) {
                System.out.println("\n- " + codeWords.get(i).word);
            } else if (depth == 1 && next == i) {
                System.out.print(".");
                next += every;
            }*/
            final CodeWord codeWord = codeWords.get(i);
            // TODO: Add terminate early on word length vs. maxWords
            final long candidateCode = codeWord.code;
            if ((wordCode & candidateCode) != 0) {
                continue;
            }
            final long concatCode = wordCode | candidateCode;

            String expanded = prefix + (prefix.isEmpty() ? "" : "_") + codeWord.word;
            if (concatCode == WANTED_CODE) {
                candidates.add(expanded);
                System.out.println(expanded);
                continue;
            }
            if (depth >= maxWords-1) {
                continue;
            }
            if (depth <= maxReduceDepth) {
                fillCandidates(reduce(codeWords, i+1, codeWord), 0, depth+1, maxReduceDepth, maxWords, concatCode, expanded, candidates);
            } else {
                fillCandidates(codeWords, i+1, depth+1, maxReduceDepth, maxWords, concatCode, expanded, candidates);
            }
        }
    }

    private static void fillCandidates1_2(
            List<CodeWord> codeWords, int maxWords, Set<String> candidates) {
        for (int i = 0 ; i < codeWords.size(); i++) {
            final CodeWord codeWord = codeWords.get(i);
            if (codeWord.word.length() < VALID_CHARS.length()/maxWords) {
                System.out.println(String.format(
                        "Reached word length %d for '%s' with maxWords = %d and max sentence length = %d",
                        codeWord.word.length(), codeWord.word, maxWords, VALID_CHARS.length()));
                return;
            }
            final List<CodeWord> reduced = reduce(codeWords, i, codeWord);

            if (reduced.isEmpty()) {
                continue;
            }
            fillCandidates(codeWords, i+1, 1, MAX_REDUCE_DEPTH, maxWords, codeWord.code, codeWord.word, candidates);
        }
    }

    private static List<CodeWord> reduce(List<CodeWord> codeWords, int start, CodeWord codeWord) {
        //System.out.println("\n- " + codeWord.word);
        final List<CodeWord> reduced = new ArrayList<>(codeWords.size());
        for (int j = start; j < codeWords.size() ; j++) {
            if ((codeWord.code & codeWords.get(j).code) == 0) {
                reduced.add(codeWords.get(j));
            }
        }
        return reduced;
    }

    private static long getWordCode(String word) {
        final char[] chars = word.toCharArray();
        long wc = 0;
        for (int i = 0 ; i < chars.length ; i++) {
            wc |= CODEPOINT_BITS[chars[i]];
        }
        return wc;
    }

    private static Set<String> loadUniqueLetterwords() throws IOException {
        final Locale DA = new Locale("da_DK");
        final Set<String> unique = new HashSet<>();
// "/home/te/projects/ponder-this/RO2012.cleaned.txt"
        for (String source: SOURCES) {
            BufferedReader reader = new BufferedReader(new FileReader(source));
            String word;
            while ((word = reader.readLine()) != null && !word.isEmpty()) {
                word = word.toLowerCase(DA).replace("-", "");
                if (isUniqueLetters(word) && !ILLEGAL_WORDS.contains(word)) {
                    //if (isUniqueLetters(word)) {
                    unique.add(word);
                }
            }
            reader.close();
        }
        unique.addAll(CUSTOM_WORDS);
        return unique;
    }

    private static boolean isUniqueLetters(final String word) {
        final boolean[] bitmap = new boolean[CODEPOINT_BITS.length];
        final char[] chars = word.toCharArray();
        for (int i = 0 ; i < chars.length ; i++) {
            char c = chars[i];
            if (c >= bitmap.length) {
                return false;
            }
            if (CODEPOINT_BITS[c] == 0 || bitmap[c]) {
                return false;
            }
            bitmap[c] = true;
        }
        return true;
    }


}
