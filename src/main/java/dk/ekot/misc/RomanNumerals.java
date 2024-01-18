/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package dk.ekot.misc;

import com.google.common.primitives.Chars;

import java.awt.*;
import java.util.Arrays;

/**
 *
 */
@SuppressWarnings({"StatementWithEmptyBody", "NonAsciiCharacters"})
public class RomanNumerals {
    final static String[] rules = new String[]{
            "IV\u0004", "IX\u0009", "I\u0001", "V\u0005", "XL\u0028", "XC\u005A", "X\n",
            "L\u0032", "CD\u0190", "CM\u0384", "C\u0064", "D\u01F4", "M\u03E8"};
    final static String[] rules2 = new String[]{ // 0x1000 0x2000
            "\u2004IV", "\u2009IX", "\u1001I", "\u1005V", "\u2028XL", "\u205aXC", "\u100aX",
            "\u1032L", "\u2190CD", "\u2384CM", "\u1064C", "\u11f4D", "\u13e8M"};


    final static String[] rules3 = new String[]{ // 0x5000 0x6000
            "怄IV", "怉IX", "倁I", "倅V", "怨XL", "恚XC", "倊X", "倲L", "憐CD", "掄CM", "偤C", "凴D", "叨M"};

    final static String[] rules4a = new String[]{
            "\u0001I", "\u0005V", "\nX", "\u0032L", "\u0064C", "\u01f4D", "\u03e8M"};

    final static String rules4c = "、搅氊㰲ᡤ\u1DF4䏨";

    public static void main(String[] args) {
//        useArray("CMMIV");

//        generate4a();
//        String in = args[0] + " ";
        for (String test: new String[]{
                "CMMIV 1904", "M 1000", "IV 4", "XIV 14", "XVIII 18", "DCCCXC 890", "XXI 21", "MMC 2100", "CM 900",
                "XXIV 24", "XL 40", "L 50", "XC 90"
        }) {
            String rom = test.split(" ")[0];
            int dec = Integer.parseInt(test.split(" ")[1]);
            if (dec != romanToDecimal(rom)) {
                System.err.println("Error: " + rom + ": " + romanToDecimal(rom) + " (expected " + dec + ")");
            } else {
                System.out.println("OK: " + rom + ": " + romanToDecimal(rom));
            }
        }

//        String romans = "IVXLCDM";
//        for (char c: romans.toCharArray()) {
//            String message = c + " -> (c-0x42) " + (c-0x42) + " -> (c%'') " + (c%'B') + ": " + Integer.toBinaryString(c);
//            if (c-0x42 == (c&0b1000010)) {
//                System.out.println(message);
//            } else {
//                System.err.println(message);
//            }
//        }
//        dumpRomans();
    }

    public static void dumpRomans() {
        char min = Character.MAX_VALUE;
        for (char co: "IVXLCDM".toCharArray()) {
            char c = (char) (co - 67);
            System.out.println(co + ": " + align(Integer.toBinaryString(c), 5));
            if (min > c) {
                min = c;
            }
        }
        System.out.println("Min: " + min);
    }

    public static String align(String v, int digits) {
        while (v.length() < digits) {
            v = "0" + v;
        }
        return v;
    }

    private static void v3(String in) {
        int result = 0;
        while (in.length() > 1) {
            for (String r: rules) {
                String s = r.substring(0, r.length()-1);
                if (in.startsWith(s)) {
                    result += r.charAt(r.length()-1);
                    in = in.substring(s.length());
                }
            }
        }
        System.out.println(result & 0x4FFF);
    }

    private static void v1(String in) {
        int result = 0;
        while (in.length() > 1) {
            for (String r: rules) {
                String s = r.substring(0, r.length()-1);
                if (in.startsWith(s)) {
                    result += r.charAt(r.length()-1);
                    in = in.substring(s.length());
                }
            }
        }
        System.out.println(result);
    }

    public static int romanToDecimalOriginal(String rom) {
        char[] map = "?£ȳ????@??qЧ????????D?I".toCharArray();
        char[] s = (rom+"E").toCharArray();
        int dec = 0;
        for (int i = 0 ; i < s.length-1 ;) {
            dec += (map[s[i]-0x42] < map[s[i+1]-0x42] ? -1 : 1) * (map[s[i++]-0x42]-'?');
        }
        return dec;
    }

    public static int romanToDecimalLong(String rom) {
        char[] map = "$£ȳ?dec@//qЧ?switch?D:I".toCharArray();
        char[] s = (rom+"E").toCharArray();
        int dec = 0;
        for (int i = 0 ; i < s.length-1 ;) {
            dec += (map[s[i]-0x42] < map[s[i+1]-0x42] ? -1 : 1) * (map[s[i++]-0x42]-'?');
        }
        return dec;
    }

    public static int romanToDecimalSane(String rom) {
        char[] map = "$£ȳ?dec@//qЧ?switch?D:I".toCharArray();
        char[] r = (rom+"E").toCharArray();
        int $, dec = 0;
        for (int i = 0 ; i < rom.length() ;) {
            dec += ($=map[r[i++]-0x42]) < map[r[i]%'B'] ? '?'-$ : $-'?';
        }
        return dec;
    }


    /* Current favourite */
    // ꓽ ߺ Ɂ ᒿ З ᐸ ᚲ Ꙩ ˆ


    public static int romanToDecimal(String rom) {
        char[] map = "$£ȳ?dec@//qЧ?switch?D:I".toCharArray(),
                r = (rom+"E").toCharArray();
        int Ɂ, Ꙩ, ꓽ;
        for (Ꙩ=ꓽ=0; Ꙩ < rom.length() ; ꓽ+= (Ɂ=map[r[Ꙩ++]-0x42]) < map[r[Ꙩ]%'B'] ? '?'-Ɂ : Ɂ-'?');
        return ꓽ;
    }


    public static int romanToDecimalC(String rom) {
        char[] map = "$£ȳ?dec@//qЧ?switch?D:I".toCharArray();
        char[] s = (rom+"E").toCharArray();
        int $, ᒿ, З;
        for (ᒿ = З = 0 ; ᒿ < s.length-1 ;) {
            З += (($=map[s[ᒿ++]-0x42]) < map[s[ᒿ]-'B'] ? -1 : 1) * ($-'?');
        }
        return З;
    }

    public static int romanToDecimalB(String rom) {
        char[] map = "$£ȳ?dec@//qЧ?switch?D:I".toCharArray();
        char[] s = (rom+"E").toCharArray();
        int dec = 0;
        for (int i = 0 ; i < s.length-1 ;) {
            dec += (map[s[i++]-0x42]-'?') * (map[s[i--]-66] > map[s[i++]-'B'] ? -1 : 1);
        }
        return dec;
    }

    public static void useArray(String in) {
        // Store in array from 'C'-'C' to 'X'-'C'
        // Each entry contains priority and value
        char[] map = getArrayTweaked();
        System.out.println(new String(map));
//        System.out.println("Array-length: " + map.length);
        System.out.println("In: " + in);
//        char[] map = "£ȳ????@??qЧ????????D?I\n".toCharArray();
        char[] s = (in+"S").toCharArray();
        int result = 0;
        for (int i = 0 ; i < s.length-1 ; i++) {
            result += (map[s[i]-INDEX_BASE] < map[s[i+1]-INDEX_BASE] ? -1 : 1) * (map[s[i]-INDEX_BASE]-TWEAK_BASE);
        }
        System.out.println(result);
    }
    static final char TWEAK_BASE = '?';
    static final char INDEX_BASE = 'B';
    private static char[] getArrayTweaked() {
        char[] map = new char['X'-INDEX_BASE+1];
        Arrays.fill(map, TWEAK_BASE);
        map['I'-INDEX_BASE] += 1;
        map['V'-INDEX_BASE] += 5;
        map['X'-INDEX_BASE] += 10;
        map['L'-INDEX_BASE] += 50;
        map['C'-INDEX_BASE] += 100;
        map['D'-INDEX_BASE] += 500;
        map['M'-INDEX_BASE] += 1000;
        return map;
    }

    private static char[] getArrayFlip() {
        char[] map = new char['Z'-'C'+1];
        Arrays.fill(map, '_');
        final int offset = 5;
        map['I'-'C'] = 1 | (1 << offset);
        map['V'-'C'] = 2 | (5 << offset);
        map['X'-'C'] = 3 | (10 << offset);
        map['L'-'C'] = 4 | (50 << offset);
        map['C'-'C'] = 5 | (100 << offset);
        map['D'-'C'] = 6 | (500 << offset);
        map['M'-'C'] = 7 | (1000 << offset);
        map['S'-'C'] = 0 | (0); // S = Stop
        return map;
    }

    public static void useArrayWorks1(String in) {
        // Store in array from 'C'-'C' to 'X'-'C'
        // Each entry contains priority and value
        char[] map = getArray();
        System.out.println("Array-length: " + map.length);
        System.out.println("In: " + in);

        int result = 0;
        for (int i = 0 ; i < in.length()-1 ; i++) {
            System.out.println("Checking: " + in.charAt(i));
            if (map[in.charAt(i)-'C']>>10 < map[in.charAt(i+1)-'C']>>10) {
                result -= map[in.charAt(i)-'C'] & 0b1111111111;
            } else {
                result += map[in.charAt(i)-'C'] & 0b1111111111;
            }
        }
        System.out.println(result);
    }

    private static char[] getArray() {
        char[] map = new char['Z'-'C'+1];
        Arrays.fill(map, '_');
        map['I'-'C'] = (1 << 10) | 1;
        map['V'-'C'] = (2 << 10) | 5;
        map['X'-'C'] = (3 << 10) | 10;
        map['L'-'C'] = (4 << 10) | 50;
        map['C'-'C'] = (5 << 10) | 100;
        map['D'-'C'] = (6 << 10) | 500;
        map['M'-'C'] = (7 << 10) | 1000;
        map['Z'-'C'] = (0 << 10) | 0;
        return map;
    }

    public static void generate46() {
        int rules[] = new int[]{ // roman, exp, 1/5
                0b00_01001_0,
                0b00_10110_1,
                0b01_11000_0,
                0b01_01100_1,
                0b10_00011_0,
                0b10_00100_1,
                0b11_01101_0
        };
        for (int rule : rules) {
            char roman = (char) ((rule >> 1) | 0b1000000);
            int value = (int) (((rule & 0b1) == 0 ? 1 : 5) * Math.pow(10, ((rule & 0b11000000)>>6)));
            System.out.println(roman + " " + value + ": " + (char) rule);
        }
    }
    public static void generate4g() {
        int rules[] = new int[]{ // padding, roman, exp, 1/5
                0b1_01001_00_0,
                0b1_10110_00_1,
                0b1_11000_01_0,
                0b1_01100_01_1,
                0b1_00011_10_0,
                0b1_00100_10_1,
                0b1_01101_11_0
        };
        for (int rule : rules) {
            char roman = (char) ((rule >> 3) | 0b1000000);
            int value = (int) (((rule & 0b1) == 0 ? 1 : 5) * Math.pow(10, ((rule & 0b110)>>1)));
            System.out.println(roman + " " + value + ": " + (char) rule);
        }
    }
    String pack4g = "ňƱǂţĜĥŮ"; //  1-bit padding, roman-64, 10^x, 1=0 / 5=1

    public static void generate4a() {
        int rules[] = new int[]{ // padding, roman, 1/5, exp
                0b1_01001_0_00,
                0b1_10110_1_00,
                0b1_11000_0_01,
                0b1_01100_1_01,
                0b1_00011_0_10,
                0b1_00100_1_10,
                0b1_01101_0_11
        };
        for (int rule : rules) {
            char roman = (char) ((rule >> 3) | 0b1000000);
            int value = (int) ((rule & 4|1) * Math.pow(10, rule & 0b11));
            //System.out.print((char)rule);
            System.out.println(roman + " " + value + ": " + (char) rule);
        }
    }
    static String pack = "ňƴǁťĚĦū"; //  1-bit padding, roman-64, 1=0 / 5=1, 10^x
    private static void vPack(String in) {
        System.out.println("In: " + in);
        int result = 0;
        for (int i = 0 ; i < in.length() ; i++) {
            for (char rule : pack.toCharArray()) {
                char roman = (char) ((rule >> 3) + 32);
                int value = (int) ((rule & 4|1) * Math.pow(10, rule & 0b11));
                if (in.charAt(i) == roman) {
                    if (in.charAt(i+1) != roman && in.charAt(i+1) != ' ') {
                        System.out.println("-" + roman + " (" + value + ")");
                        result -= value;
                    } else {
                        System.out.println("+" + roman + " (" + value + ")");
                        result += value;
                    }
                }
            }
        }
        System.out.println(result);
    }



    public static void generate4c() {
        // 5^x ->
        // 1: 1*10^0 b000
        // 5: 5*10^0 b100
        // 10: 1*10^1 b001
        // 50: 5*10^1 b101
        // 100: 1*10^2 b010
        // 500: 5*10^2 b110
        // 1000: 1*10^3 b011

        int rules[] = new int[]{ // roman, 1/5, exp
                0b01001_000, // 1: I - 64
                0b10110_100, // 5: V
                0b11000_001, // 10: X
                0b01100_101, // 50:
                0b00011_010, // 100
                0b00100_110, // 500
                0b01101_011 // 1000
        };
        for (int rule : rules) {
            char roman = (char) ((rule >> 3) | 0b1000000);
            int value = (int) (((rule & 0b100) == 0 ? 1 : 5) * Math.pow(10, rule & 0b11));
            System.out.println(roman + " " + value + ": " + (char) rule);
        }
    }
    public static void generate4b() {
        // 5^x ->
        // 1: 5^0  1*10^0 b000
        // 5: 5^1 5*10^0 b100
        // 10: 5^1*2 1*10^1 b001
        // 50: 5^2*2 5*10^1 b101
        // 100: 5^2*4 1*10^2 b010
        // 500: 5^2*20 5*10^2 b110
        // 1000: 5^2*40 3+6 bits 1*10^3 b011

        // 1: 0b01001 ^ 00000 01000
        // 5: 0b10110 ^ 00000
        int rules[] = new int[] {
                0b000_01001, // 1: I - 64
                0b100_10110, // 5: V
                0b001_11000, // 10: X
                0b101_01100, // 50:
                0b010_00011, // 100
                0b110_00100, // 500
                0b011_01101 // 1000
        };
        for (int rule: rules) {
            char roman = (char) ((rule & 0b11111) | 0b1000000);
            int value = (int) (((rule & 10000000) == 0 ? 1 : 5) * Math.pow(10, (rule & 0b1100000) >> 5));
            System.out.println(roman + " " + value + ": " + (char)rule);
        }
/*
        for (String rule: rules4a) {
            int value = rule.charAt(0); // 1 5 10 50 100 500 1000
            char roman = rule.charAt(1);
            char romanMod = (char) ((roman-64));
            int ocode = (value << 5 | romanMod);
            char ucode = (char)(ocode);
//            System.out.println(ocode + " " + (ucode^1984));
            System.out.println(a(value, 10) + " " + a(romanMod, 5));// + " " + ucode);*/
            /*System.out.println(
                               " value" + a(value, 10)
                               + " roman" + a(roman, 7)
                               + ", romanMod" + a(romanMod, 5)
                               + ", " + roman + ": " + ucode + " " + a(ucode, 16) + " V:" + (int)value);*/
//        }
  //      System.out.println();
    }

    public static String a(int val, int digits) {
        return "(" + align(Integer.toBinaryString(val), digits) + ")";
    }

    public static void generate() {
        System.out.print("new String[]{");
        for (String rule: rules) {
            String roman = rule.substring(0, rule.length()-1);
            char value = rule.charAt(rule.length()-1);
            int ucode = (roman.length() == 1 ? 0x1000 : 0x2000) + value;
            //System.out.println(roman + " -> \\u" + Integer.toHexString(ucode) + " -> " + (char)ucode);
            //System.out.print("\"\\u" + Integer.toHexString(ucode) + roman + "\", ");
            System.out.print("\"" + (char)ucode + roman + "\", ");
            if (!isPrintableChar((char) ucode))  {
                System.out.println("!");
            }
        }
        System.out.println("};");
    }

    public static boolean isPrintableChar( char c ) {
        Font font = new Font("Monospaced", Font.PLAIN, 12);
        return font.canDisplay(c);
        /*
        Character.UnicodeBlock block = Character.UnicodeBlock.of( c );
        return (!Character.isISOControl(c)) &&
               c != KeyEvent.CHAR_UNDEFINED &&
               block != null &&
               block != Character.UnicodeBlock.SPECIALS;
               */
    }
}
