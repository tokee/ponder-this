/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package dk.ekot.misc;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Arrays;

/**
 *
 */
public class RomanNumerals {
    final static String[] rules = new String[]{
            "IV\u0004", "IX\u0009", "I\u0001", "V\u0005", "XL\u0028", "XC\u005A", "X\n",
            "L\u0032", "CD\u0190", "CM\u0384", "C\u0064", "D\u01F4", "M\u03E8"};
    final static String[] rules2 = new String[]{ // 0x1000 0x2000
            "\u2004IV", "\u2009IX", "\u1001I", "\u1005V", "\u2028XL", "\u205aXC", "\u100aX",
            "\u1032L", "\u2190CD", "\u2384CM", "\u1064C", "\u11f4D", "\u13e8M"};

    final static String[] rules3 = new String[]{ // 0x5000 0x6000
            "怄IV", "怉IX", "倁I", "倅V", "怨XL", "恚XC", "倊X", "倲L", "憐CD", "掄CM", "偤C", "凴D", "叨M"};

    public static void main(String[] args) {
        generate();
//        String in = args[0] + " ";
        String in = "CMMIV ";
        v1(in);
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
