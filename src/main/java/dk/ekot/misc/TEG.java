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

import java.util.Locale;

/**
 *  (🍌 +🍎 + 🍍) % 305 = X år
 */
public class TEG {

    static double mathIt(double apple, double banana, double pineapple) {
        return apple/(banana+pineapple) +
               banana/(apple+pineapple) +
               pineapple/(apple+banana);
    }

    public static void main(String[] args) {
        final int MAX = 3000;
        double best = 0;
        for (int apple = 1 ; apple < MAX ; apple++) {
            for (int banana = 1 ; banana < MAX ; banana++) {
                for (int pineapple = 1 ; pineapple < MAX ; pineapple++) {
                    double candidate = mathIt(apple, banana, pineapple);
                    int age = (apple+banana+pineapple)%305;
                    if (age > 10 && age < 130 && Math.abs(4-candidate) < Math.abs(4-best)) {
                        System.out.printf(
                                Locale.ROOT, "apple=%4d, banana=%4d, pineapple=%4d, result=%1.4f, age=%3d\n",
                                apple, banana, pineapple, candidate, age);
                        best = candidate;
                    }
                }
            }
        }
    }

}
