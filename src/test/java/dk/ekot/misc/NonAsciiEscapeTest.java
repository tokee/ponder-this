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

import junit.framework.Assert;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.regex.Pattern;

public class NonAsciiEscapeTest {
    private static Log log = LogFactory.getLog(NonAsciiEscapeTest.class);


    /**
     * Unescapes all hex-escapes to UTF-8.
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public static String unEscapeHexOld(final String str) throws UnsupportedEncodingException {
        try {
            return java.net.URLDecoder.decode(str, "UTF-8");
        } catch (IllegalArgumentException e) {
            // Probably met something that contained % uses as percent instead of escape
            // All '%xx' where either of 'xx' are not hex codes, will have their '%' escaped to '%25'
            String current = str;
            while (!current.equals(current = PERCENT_PATTERN.matcher(current).replaceAll("%25$1")));
            return java.net.URLDecoder.decode(current, "utf-8");
        }
    }
    public final static Pattern PERCENT_PATTERN = Pattern.compile("%([^a-fA-F0-9]|.[^a-fA-F0-9]|.$|$)");

    public static String unEscapeHex(final String str) throws UnsupportedEncodingException {
        ByteArrayOutputStream sb = new ByteArrayOutputStream(str.length()*2);
        final byte[] utf8 = str.getBytes(UTF8_CHARSET);
        int i = 0;
        while (i < utf8.length) {
            int c = utf8[i];
            if (c == '%') {
                if (i < utf8.length-2 && isHex(utf8[i+1]) && isHex(utf8[i+2])) {
                    sb.write(Integer.parseInt("" + (char)utf8[i+1] + (char)utf8[i+2], 16));
                    i += 3;
                } else {
                    sb.write('%');
                    i++;
                }
                // https://en.wikipedia.org/wiki/UTF-8
            } else if ((0b10000000 & utf8[i]) == 0) { // ASCII
                sb.write(0xff & utf8[i++]);
            } else if ((0b11100000 & utf8[i]) == 0b11000000) { // 2 byte UTF-8
                sb.write(0xff & utf8[i++]);
                sb.write(0xff & utf8[i++]);
            } else if ((0b11110000 & utf8[i]) == 0b11100000) { // 3 byte UTF-8
                sb.write(0xff & utf8[i++]);
                sb.write(0xff & utf8[i++]);
                sb.write(0xff & utf8[i++]);
            } else if ((0b11111000 & utf8[i]) == 0b11110000) { // 4 byte UTF-8
                sb.write(0xff & utf8[i++]);
                sb.write(0xff & utf8[i++]);
                sb.write(0xff & utf8[i++]);
                sb.write(0xff & utf8[i++]);
            } else {
                throw new IllegalArgumentException(
                        "The input String '" + str + "' does not translate to supported UTF-8");
            }
        }
        return sb.toString("utf-8");
    }
    private static boolean isHex(byte b) {
        return (b >= '0' && b <= '9') || (b >= 'a' && b <= 'f') || (b >= 'A' && b <= 'F');
    }

    /**
     * Normalises URLs with or without hex-escapes to unambiguous form.
     * {code "http://example.com/foo bar.html" → "http://example.com/foo bar.html"},
     * {code "http://example.com/foo%20bar.html" → "http://example.com/foo bar.html"},
     * Handles mal-formed hex-escapes.
     * @param url
     * @return
     * @throws UnsupportedEncodingException
     */

    public static String normaliseHexEscapeUTF8(final String url) throws UnsupportedEncodingException {
        ByteArrayOutputStream sb = new ByteArrayOutputStream(url.length()*2);
        final byte[] utf8 = url.getBytes(UTF8_CHARSET);
        int i = 0;
        while (i < utf8.length) {
            int c = utf8[i];
            if (c == '%') {
                if (i < utf8.length-2 && isHex(utf8[i+1]) && isHex(utf8[i+2])) {
                    sb.write(Integer.parseInt("" + (char)utf8[i+1] + (char)utf8[i+2], 16));
                    i += 3;
                } else {
                    sb.write('%');
                    i++;
                }
                // https://en.wikipedia.org/wiki/UTF-8
            } else if ((0b10000000 & utf8[i]) == 0) { // ASCII
                sb.write(0xff & utf8[i++]);
            } else if ((0b11100000 & utf8[i]) == 0b11000000) { // 2 byte UTF-8
                sb.write(0xff & utf8[i++]);
                sb.write(0xff & utf8[i++]);
            } else if ((0b11110000 & utf8[i]) == 0b11100000) { // 3 byte UTF-8
                sb.write(0xff & utf8[i++]);
                sb.write(0xff & utf8[i++]);
                sb.write(0xff & utf8[i++]);
            } else if ((0b11111000 & utf8[i]) == 0b11110000) { // 4 byte UTF-8
                sb.write(0xff & utf8[i++]);
                sb.write(0xff & utf8[i++]);
                sb.write(0xff & utf8[i++]);
                sb.write(0xff & utf8[i++]);
            } else {
                throw new IllegalArgumentException(
                        "The input String '" + url + "' does not translate to supported UTF-8");
            }
        }
        return sb.toString("utf-8");
    }


    @Test
    public void justATest() throws UnsupportedEncodingException {
        System.out.println(java.net.URLEncoder.encode("/a, bær", "utf-8"));
    }

    @Test
    public void assignCompareTest() {
        String in = "1";
        while (!in.equals(in = in.length() <= 5 ? in + "*" : in));
        System.out.println(in);
    }

    /**
     * Escapes all non-ASCII-characters to UTF-8 represented as hex-codes. Supports up to 4-byte UTF-8.
     * flødebolle -> fl%C3%B8debolle
     */
    public static String escapeNonAscii(String str) throws UnsupportedEncodingException {
        sb.setLength(0);
        final byte[] utf8 = str.getBytes(UTF8_CHARSET);
        int i = 0;
        while (i < utf8.length) {
            int c = utf8[i];
            if (c <= 0x20 || c == '%') { // Special characters
                sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
                // https://en.wikipedia.org/wiki/UTF-8
            } else if ((0b10000000 & utf8[i]) == 0) { // ASCII
                sb.append((char) (0xff & utf8[i++]));
            } else if ((0b11100000 & utf8[i]) == 0b11000000) { // 2 byte UTF-8
                sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
                sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
            } else if ((0b11110000 & utf8[i]) == 0b11100000) { // 3 byte UTF-8
                sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
                sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
                sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
            } else if ((0b11111000 & utf8[i]) == 0b11110000) { // 4 byte UTF-8
                sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
                sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
                sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
                sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
            } else {
                throw new IllegalArgumentException(
                        "The input String '" + str + "' does not translate to supported UTF-8");
            }
        }
        return sb.toString();
    }
    private static Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private static StringBuffer sb = new StringBuffer();
    // Note: Move StringBuffer allocation inside method to make it Thread safe

    @Test
    public void testConvert() throws UnsupportedEncodingException {
        for (String t: new String[]{"æøå", "flødebolle", "Rosé", "Foo bar", "flødebolle, bær", "%"}) {
            System.out.println(t + " -> " + escapeNonAscii(t));
        }
    }
    @Test
    public void testUnescape() throws UnsupportedEncodingException {
        for (String[] test: new String[][]{
                {"%C3%A6%C3%B8%C3%A5", "æøå"},
                {"fl%C3%B8debolle", "flødebolle"},
                {"Ros%C3%A9", "Rosé"},
                {"a%2C%20b", "a, b"},
                {"foo%2A", "foo*"},
                {"foo+", "foo+"},
                {"foo%2B", "foo+"},
                {"illegal 10% wrong", "illegal 10% wrong"},
                {"illegal 5%", "illegal 5%"},
                {"illegal%%a ", "illegal%%a "},
                {"illegal%%a", "illegal%%a"},
                {"illegal%%", "illegal%%"},
                {"illegal %a%bo", "illegal %a%bo"},
                {"illegal %%21", "illegal %!"},
        }) {
            Assert.assertEquals(test[1], unEscapeHex(test[0]));
        }
    }

    @Test
    public void testSteps() throws UnsupportedEncodingException {
        for (String t: new String[]{"Red%2C%20Ros%C3%A9 14%25"}) {
            System.out.println(t + " -> " + unEscapeHex(t) + " -> " + escapeNonAscii(unEscapeHex(t)) + " -> " +
                               escapeNonAscii(unEscapeHex(t)).toLowerCase(new Locale("utf-8")));
        }

    }
}
