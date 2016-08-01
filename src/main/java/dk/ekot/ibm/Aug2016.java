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
package dk.ekot.ibm;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;

/**
 * https://www.research.ibm.com/haifa/ponderthis/challenges/August2016.html
 *   Ponder This Challenge:
 * A king receives 10 bags of N golden coins each. Each coin should weigh exactly 10 grams, but some bags contain (only)
 * counterfeit coins that weigh exactly 9 grams each.
 * If N>=1024 then one can identify the counterfeit bags using a single measurement with an accurate weighing scale.
 * (How?)
 * Our challenge this month is to find a way to identify the counterfeit bags, when we know that there are at most
 * three such bags out of the ten, and when N=174.
 * Bonus '*' for solving it with even smaller N.
 */
public class Aug2016 {
    private static Log log = LogFactory.getLog(Aug2016.class);
    public static final int GOOD = 10;
    public static final int BAD = 9;

    public static void main(String[] args) {
        //sampleRun();
        for (int bags = 1 ; bags <= 10 ; bags++) {
            permutateRun(Math.min(bags, 3), bags, 174);
        }
    }

    private static void permutateRun(int atMostBadBags, int bagCount, int atMostCoinsPerBag) {
        final int[] bags = new int[bagCount];
        final boolean[] inUse = new boolean[atMostCoinsPerBag];
        final boolean[] existing = new boolean[bagCount*atMostCoinsPerBag*Math.max(GOOD, BAD)];
        long startTime = System.nanoTime();
        boolean solved = permutateAndCheck(bags, inUse, existing, 0, 0, atMostCoinsPerBag, atMostBadBags);
        System.out.println(String.format(
                "atMostBadBags=%d, bagCount=%d, atMostCoinsPerBag=%d, time=%.2fms, solution=%s",
                atMostBadBags, bagCount, atMostCoinsPerBag, (System.nanoTime()-startTime)/1000000.0,
                solved ? Arrays.toString(bags) : "N/A"));
    }

    private static boolean permutateAndCheck(
            int[] bags, boolean[] inUse, boolean[] existing, int index, int lastCoins,
            double atMostCoinsPerBag, int atMostBadBags) {
        if (index == bags.length) {
            Arrays.fill(existing, false);
            return validates(bags, existing, atMostBadBags);
        }
        boolean checked = false;
        for (int coins = lastCoins+1 ; coins < atMostCoinsPerBag ; coins++) {
            if (!inUse[coins]) {
                inUse[coins] = true;
                checked = true;
                bags[index] = coins;
                if (permutateAndCheck(bags, inUse, existing, index+1, coins, atMostCoinsPerBag, atMostBadBags)) {
                    return true;
                }
                inUse[coins] = false;
            }
        }
        return false; // What if not checked?
    }

    private static void sampleRun() {
        final int[] SAMPLE = new int[]{1, 2, 4};
        final int atMostCoinsPerBag = 4;
        //int[] SAMPLE = new int[]{1, 174, 3, 4, 5, 6};
        boolean[] existing = new boolean[SAMPLE.length*atMostCoinsPerBag*Math.max(GOOD, BAD)];
        if (validates(SAMPLE, existing, 2)) {
            System.out.println("It works!");
        } else {
            System.out.println("Fails!");
        }
    }

    private static boolean validates(int[] bags, boolean[] existing, int atMostBadBags) {
        int idealSum = 0;
        for (int val: bags) {
            idealSum += val*GOOD;
        }
        return validates(bags, existing, 0, atMostBadBags, idealSum);
    }

    private static boolean validates(int[] bags, boolean[] existing, int nextFreeBad, int missingBad, int sum) {
        if (nextFreeBad == bags.length || missingBad == 0) {
            return true;
        }
        for (int bag = nextFreeBad ; bag < bags.length ; bag++) {
            final int diff = bags[bag]*BAD - bags[bag]*GOOD;
            sum += diff;
            if (existing[sum]) {
                return false;
            }
            existing[sum] = true;
            if (!validates(bags, existing, bag+1, missingBad-1, sum)) {
                return false;
            }
            sum -= diff;
        }
        return true;
    }
}
