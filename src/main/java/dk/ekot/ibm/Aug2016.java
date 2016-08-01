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
        final int BAGS = 10;
        permutateRun(Math.min(BAGS, 3), BAGS, 174, false);
/*        for (int bags = 1 ; bags <= 10 ; bags++) {
            permutateRun(Math.min(bags, 3), bags, 174);
        }*/
    }

    private static void permutateRun(int atMostBadBags, int bagCount, int atMostCoinsPerBag, boolean stopWhenFound) {
        final int[] best = new int[bagCount];
        Arrays.fill(best, atMostCoinsPerBag);
        final int[] bags = new int[bagCount];
        final boolean[] inUse = new boolean[atMostCoinsPerBag];
        final boolean[] existing = new boolean[bagCount*atMostCoinsPerBag*Math.max(GOOD, BAD)];
        long startTime = System.nanoTime();
        boolean solved = permutateAndCheck(
                bags, best, inUse, existing, 0, 0, atMostCoinsPerBag, atMostBadBags, stopWhenFound);
        System.out.println(String.format(
                "atMostBadBags=%d, bagCount=%d, atMostCoinsPerBag=%d, time=%.2fms, solution=%s",
                atMostBadBags, bagCount, atMostCoinsPerBag, (System.nanoTime()-startTime)/1000000.0,
                Arrays.toString(best)));
    }

    private static boolean permutateAndCheck(
            int[] bags, int[] best, boolean[] inUse, boolean[] existing, int index, int lastCoins,
            double atMostCoinsPerBag, int atMostBadBags, boolean stopWhenFound) {
        // TODO: Always check to break bad solutions before the array is full
        if (index > 0) {
            Arrays.fill(existing, false);
            if (!validates(bags, index-1, existing, atMostBadBags)) {
                return false;
            }
            if (index == bags.length) {
                return true;
//            Arrays.fill(existing, false);
//            return validates(bags, bags.length, existing, atMostBadBags);
            }
        }

        boolean checked = false;
        final int bestIndex = best.length-1;
        final int bestLeft = best.length-index+1;
        for (int coins = lastCoins+1 ; coins < best[bestIndex]-bestLeft ; coins++) {
            if (!inUse[coins]) {
                inUse[coins] = true;
                checked = true;
                bags[index] = coins;
                if (permutateAndCheck(bags, best, inUse, existing, index+1, coins, atMostCoinsPerBag, atMostBadBags,
                                      stopWhenFound)) {
                    if (stopWhenFound) {
                        return true;
                    } else {
                        if (bags[bags.length-1] < best[best.length-1]) {
                            System.out.println(Arrays.toString(bags));
                            System.arraycopy(bags, 0, best, 0, bags.length);
                        }
                        continue;
                    }
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
        if (validates(SAMPLE, SAMPLE.length, existing, 2)) {
            System.out.println("It works!");
        } else {
            System.out.println("Fails!");
        }
    }

    private static boolean validates(int[] bags, int bagCount, boolean[] existing, int atMostBadBags) {
        int idealSum = 0;
        for (int val: bags) {
            idealSum += val*GOOD;
        }
        return validates(bags, bagCount, existing, 0, atMostBadBags, idealSum);
    }

    private static boolean validates(int[] bags, int bagCount,
                                     boolean[] existing, int nextFreeBad, int missingBad, int sum) {
        if (nextFreeBad == bagCount || missingBad == 0) {
            return true;
        }
        for (int bag = nextFreeBad ; bag < bagCount ; bag++) {
            final int diff = bags[bag]*BAD - bags[bag]*GOOD;
            sum += diff;
            if (existing[sum]) {
                return false;
            }
            existing[sum] = true;
            if (!validates(bags, bagCount, existing, bag+1, missingBad-1, sum)) {
                return false;
            }
            sum -= diff;
        }
        return true;
    }
}

/*

  atMostBadBags=1, bagCount=1, atMostCoinsPerBag=5574, time=1.35ms, solution=[1]
atMostBadBags=2, bagCount=2, atMostCoinsPerBag=5574, time=1.16ms, solution=[1, 2]
atMostBadBags=3, bagCount=3, atMostCoinsPerBag=5574, time=1.34ms, solution=[1, 2, 3]
atMostBadBags=3, bagCount=4, atMostCoinsPerBag=5574, time=47.68ms, solution=[1, 2, 4, 5]
atMostBadBags=3, bagCount=5, atMostCoinsPerBag=5574, time=248.90ms, solution=[1, 2, 4, 8, 9]
atMostBadBags=3, bagCount=6, atMostCoinsPerBag=5574, time=776.86ms, solution=[1, 2, 4, 8, 15, 16]
atMostBadBags=3, bagCount=7, atMostCoinsPerBag=5574, time=2073.66ms, solution=[1, 2, 4, 8, 15, 28, 29]
atMostBadBags=3, bagCount=8, atMostCoinsPerBag=5574, time=4769.95ms, solution=[1, 2, 4, 8, 15, 28, 52, 53]
atMostBadBags=3, bagCount=9, atMostCoinsPerBag=5574, time=10776.46ms, solution=[1, 2, 4, 8, 15, 28, 52, 96, 97]
atMostBadBags=3, bagCount=10, atMostCoinsPerBag=5574, time=21165.85ms, solution=[1, 2, 4, 8, 15, 28, 52, 96, 165, 166]
atMostBadBags=3, bagCount=11, atMostCoinsPerBag=5574, time=39665.24ms, solution=[1, 2, 4, 8, 15, 28, 52, 96, 165, 278, 279]
atMostBadBags=3, bagCount=12, atMostCoinsPerBag=5574, time=71297.57ms, solution=[1, 2, 4, 8, 15, 28, 52, 96, 165, 278, 460, 461]
atMostBadBags=3, bagCount=13, atMostCoinsPerBag=5574, time=109865.99ms, solution=[1, 2, 4, 8, 15, 28, 52, 96, 165, 278, 460, 663, 664]
atMostBadBags=3, bagCount=14, atMostCoinsPerBag=5574, time=170580.48ms, solution=[1, 2, 4, 8, 15, 28, 52, 96, 165, 278, 460, 663, 980, 981]
atMostBadBags=3, bagCount=15, atMostCoinsPerBag=5574, time=242107.54ms, solution=[1, 2, 4, 8, 15, 28, 52, 96, 165, 278, 460, 663, 980, 1332, 1333]
atMostBadBags=3, bagCount=16, atMostCoinsPerBag=5574, time=340697.04ms, solution=[1, 2, 4, 8, 15, 28, 52, 96, 165, 278, 460, 663, 980, 1332, 1864, 1865]
atMostBadBags=3, bagCount=17, atMostCoinsPerBag=5574, time=467159.20ms, solution=[1, 2, 4, 8, 15, 28, 52, 96, 165, 278, 460, 663, 980, 1332, 1864, 2609, 2610]

10: [1, 2, 30, 56, 68, 91, 106, 110, 114, 115]

 */
