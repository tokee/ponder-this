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

import java.util.regex.Pattern;

/**
 *
 */
public class LetterSearch {
    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder(32768);
        Pattern OK = Pattern.compile("[\\p{L}0-9]+");
        for (char c = 0 ; c < 32768 ; c++) {
            if (OK.matcher(Character.toString(c)).matches()) {
                sb.append(Character.toString(c));
            }
        }

        for (int i = 0 ; i < sb.length() ; i++) {
            System.out.print(sb.charAt(i));
            if (i % 100 == 0) {
                System.out.println();
            }
        }
    }
}
