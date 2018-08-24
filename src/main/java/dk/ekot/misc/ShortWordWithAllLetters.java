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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * http://stevensnedker.com/ord.htm
 * https://dsn.dk/retskrivning/om-retskrivningsordbogen/ro-elektronisk-og-som-bog
 * Direct: https://dsn.dk/retskrivning/om-retskrivningsordbogen/RO2012.opslagsord.med.homnr.og.ordklasse.zip
 * Unzip and
 * cat RO2012.opslagsord.med.homnr.og.ordklasse.txt | cut -d';' -f1 | sed 's/^[0-9][.] //' | grep -v "^-" | tr '[:upper:]' '[:lower:]' > RO2012.cleaned.txt
 * TODO: Add Danish cities and person names
 */
@SuppressWarnings("ForLoopReplaceableByForEach")
public class ShortWordWithAllLetters {
    private static Log log = LogFactory.getLog(ShortWordWithAllLetters.class);
    public static final String VALID_CHARS = "abcdefghijklmnopqrstuvwxyzæøå";
    public static final Set<String> ILLEGAL_WORDS = new HashSet<>(Arrays.asList(
            "a", "b", "c", "d", "e", "f", "g", "h", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u",
            "v", "w", "x", "y", "z", "æ", "ov", "fx", "vy", "kuf"));
    
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
        final List<String> words = new ArrayList<>(loadUniqueLetterwords());
        words.sort((o1, o2) -> {
            if (o1.length() > o2.length()) {
                return -1;
            }
            if (o1.length() < o2.length()) {
                return 1;
            }
            return o1.compareTo(o2);
        });
        System.out.println("Got " + words.size() + " words");
        Set<String> candidates = new HashSet<>();
        System.out.println(
                "|--------------------------------------------------------------------------------------------------|");
        fillCandidates(words, toWordcodes(words), 0, 0, 0L,"", candidates);
    }

    private static List<Long> toWordcodes(List<String> words) {
        List<Long> wordCodes = new ArrayList<>(words.size());
        for (int i = 0 ; i < words.size() ; i++) {
            wordCodes.add(getWordCode(words.get(i)));
        }
        return wordCodes;
    }

    private static void fillCandidates(
            List<String> words, List<Long> wordCodes, int index, int depth, long wordCode, String expandedPrefix,
            Set<String> candidates) {
        int every = words.size()/100;
        int next = every;
        for (int i = index ; i < words.size(); i++) {
            if (depth == 0) {
                System.out.println("\n- " + words.get(i));
            } else if (depth == 1 && next == i) {
                System.out.print(".");
                next += every;
            }
            final long candidateCode = wordCodes.get(i);
            if ((wordCode & candidateCode) != 0) {
                continue;
            }
            final long concatCode = wordCode | candidateCode;

            String expanded = expandedPrefix + (expandedPrefix.isEmpty() ? "" : "_") + words.get(i);
            if (concatCode == WANTED_CODE) {
                candidates.add(expanded);
                System.out.println(expanded);
                continue;
            }
            fillCandidates(words, wordCodes, index+1, depth+1, concatCode, expanded, candidates);
        }
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

        BufferedReader reader = new BufferedReader(new FileReader("/home/te/projects/ponder-this/RO2012.cleaned.txt"));
        String word;
        while ((word = reader.readLine()) != null && !word.isEmpty()) {
            word = word.toLowerCase(DA);
            if (isUniqueLetters(word) && !ILLEGAL_WORDS.contains(word)) {
                unique.add(word);
            }
        }
        return unique;
    }

    private static boolean isUniqueLetters(final String word) {
        final boolean[] bitmap = new boolean[CODEPOINT_BITS.length];
        final char[] chars = word.toCharArray();
        for (int i = 0 ; i < chars.length ; i++) {
            char c = chars[i];
            if (c > bitmap.length) {
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
