package dk.ekot.misc;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

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
public class CollationTest {

    @Test
    public void testDanish() {
        assertItems(Arrays.asList(
                "abe", "aben", "aben løb", "abende", "æblegrød", "ørentvist", "Å"
        ), new Locale("da-dk"));
    }

    @Test
    public void testDanishBibliographic() {
        assertChars("#$%&*+<=>@¢£¥§©¬®°±·×÷€", new Locale("da-dk")); // TODO: Uendelig
    }

    @Test
    public void testBasicCollation() {
        final Locale DA = new Locale("da-dk");
        assertChars("0123456789abcdefghijklmnopqrstuvwxyzæøå", DA);
    }

    private void assertChars(String chars, Locale locale) {
        List<String> items = new ArrayList<>(chars.length());
        for (char c: chars.toCharArray()) {
            items.add(Character.toString(c));
        }
        assertItems(items, locale);
    }

    private void assertItems(List<String> items, Locale locale) {
        List<String> reordered = new ArrayList<>(items);
        Collections.shuffle(reordered);
        List<String> sorted = Collation.sort(reordered, locale);
        for (int i = 0 ; i < items.size() ; i++) {
            assertEquals("Expected same item at #" + i, items.get(i), sorted.get(i));
        }
    }
}