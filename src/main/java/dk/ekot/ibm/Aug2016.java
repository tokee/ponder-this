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
import java.util.concurrent.atomic.AtomicLong;

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
/*
    No (x1+y1+z1)*0.9 == (x2+y2)*0.9 == x3*0.9 == any_single_value
    No (x1+y1+z1) == (x2+y2) == x3 == any_single_value/0.9
 */
public class Aug2016 {
    private static Log log = LogFactory.getLog(Aug2016.class);
    public static final int GOOD = 10;
    public static final int BAD = 9;

    public static void main(String[] args) {

//        checkValidate();

//        permutateRun(3, 7, 174, true, true);
//        permutateRun(3, 10, 174, true, true);


        final int BAGS = 10;
        final int MAX = 174;
        //permutateRun(Math.min(BAGS, 3), BAGS, 100, true, true);
        for (int bags = 1 ; bags <= BAGS ; bags++) {
            permutateRun(Math.min(bags, 3), bags, MAX, true, true);
        }
    }

    private static void checkValidate() {
        int[] bags = new int[]{1, 2, 3};
        boolean[] existing = new boolean[bags.length*3*Math.max(GOOD, BAD)];
        for (int checks = 1 ; checks <= 3 ; checks++) {
            if (!validates(bags, bags.length, existing, checks)) {
                // 1*9+2*9+3*10 == 1*10+2*10+3*9
                System.out.println("All OK: " + Arrays.toString(bags) + " did not validate");
                return;
            }
        }
        System.out.println("Problem: " + Arrays.toString(bags) + " validated but should not");
    }

    private static void permutateRun(
            int atMostBadBags, int bagCount, int atMostCoinsPerBag, boolean print, boolean forward) {
        final int[] best = new int[bagCount];
        Arrays.fill(best, atMostCoinsPerBag);
        final int[] bags = new int[bagCount];
        Arrays.fill(bags, atMostCoinsPerBag);
        final boolean[] existing = new boolean[bagCount*atMostCoinsPerBag*Math.max(GOOD, BAD)];
        AtomicLong solutionCount = new AtomicLong(0);
        long startTime = System.nanoTime();
        permutateAndCheck(bags, best, existing, 0, atMostBadBags, print, forward, solutionCount, atMostCoinsPerBag);
        System.out.println(String.format(
                "atMostBadBags=%d, bagCount=%d, atMostCoinsPerBag=%d, time=%.2fms, solution=%s",
                atMostBadBags, bagCount, atMostCoinsPerBag, (System.nanoTime()-startTime)/1000000.0,
                Arrays.toString(best)));
    }

    private static boolean permutateAndCheck(
            int[] bags, int[] best, boolean[] existing, int index, int atMostBadBags, boolean print, boolean forward,
            AtomicLong solutionCount, int atMostCoinsPerBag) {
        if (index > 0) {
            Arrays.fill(existing, false);
            for (int badBags = 1 ; badBags <= atMostBadBags ; badBags++) { // a+b+c == d+e == f
//                Arrays.fill(existing, false);
                if (!validates(bags, index, existing, badBags)) {
                    return false;
                }
            }
            if (index == bags.length) {
                return true;
//            Arrays.fill(existing, false);
//            return validates(bags, bags.length, existing, atMostBadBags);
            }
        }

        if (forward) {
            final int startCoins = index == 0 ? 1 : bags[index - 1] + 1;
            final int bestIndex = best.length-1;
            final int bestLeft = best.length-index+1;
            for (int coins = startCoins ; coins < best[bestIndex]-bestLeft ; coins++) {
                if (index == 0) {
                    System.out.print(coins + ": ");
                }
                bags[index] = coins;
                if (permutateAndCheck(bags, best, existing, index + 1, atMostBadBags, print, true, solutionCount,
                                      atMostCoinsPerBag)) {
                    if (index > 0) {
                        return true;
                    }
                    if (print || solutionCount.getAndAdd(1) % 100 == 0) {
                        System.out.println(Arrays.toString(bags));
                    }
                    System.arraycopy(bags, 0, best, 0, bags.length);
                }
            }
        } else {
            final int startCoins = index == 0 ? bags[0] : bags[index - 1] - 1;
            final int endCoins = best.length - index;
            for (int coins = startCoins; coins >= endCoins; coins--) {
                if (index == 0) {
                    System.out.print(coins + ": ");
                }
                bags[index] = coins;
                if (permutateAndCheck(bags, best, existing, index + 1, atMostBadBags, print, false, solutionCount,
                                      atMostCoinsPerBag)) {
                    if (index > 0) {
                        return true;
                    }
                    if (print || solutionCount.getAndAdd(1) % 100 == 0) {
                        System.out.println(Arrays.toString(bags));
                    }
                    System.arraycopy(bags, 0, best, 0, bags.length);
                }
            }
        }
        return false;
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
        for (int bag = nextFreeBad ; bag < bagCount-(missingBad-1) ; bag++) {
            final int diff = bags[bag]*BAD - bags[bag]*GOOD;
            sum += diff;
            if (missingBad == 1) {
                if (existing[sum]) {
                    return false;
                }
                existing[sum] = true;
            }
            if (!validates(bags, bagCount, existing, bag+1, missingBad-1, sum)) {
                return false;
            }
            sum -= diff;
        }
        return true;
    }
}

/*

1: [1]
atMostBadBags=1, bagCount=1, atMostCoinsPerBag=174, time=0.62ms, solution=[1]
1: [1, 2]
atMostBadBags=2, bagCount=2, atMostCoinsPerBag=174, time=0.14ms, solution=[1, 2]
1: [1, 2, 4]
atMostBadBags=3, bagCount=3, atMostCoinsPerBag=174, time=0.32ms, solution=[1, 2, 4]
1: [1, 2, 4, 8]
2:
atMostBadBags=3, bagCount=4, atMostCoinsPerBag=174, time=0.84ms, solution=[1, 2, 4, 8]
1: [1, 2, 4, 8, 15]
2: 3: 4: 5: 6: 7: 8:
atMostBadBags=3, bagCount=5, atMostCoinsPerBag=174, time=4.21ms, solution=[1, 2, 4, 8, 15]
1: [1, 2, 4, 8, 15, 28]
2: 3: [3, 6, 12, 20, 24, 25]
4: 5: 6: 7: 8: 9: 10: 11: 12: 13: 14: 15: 16: 17:
atMostBadBags=3, bagCount=6, atMostCoinsPerBag=174, time=12.20ms, solution=[3, 6, 12, 20, 24, 25]
1: [1, 2, 4, 8, 15, 28, 52]
2: [2, 3, 12, 24, 41, 45, 49]
3: [3, 6, 12, 22, 41, 42, 43]
4: 5: 6: 7: 8: 9: 10: 11: 12: 13: 14: 15: 16: 17: 18: 19: 20: 21: 22: 23: 24: 25: 26: 27: 28: 29: 30: 31: 32: 33: 34:
atMostBadBags=3, bagCount=7, atMostCoinsPerBag=174, time=506.75ms, solution=[3, 6, 12, 22, 41, 42, 43]
1: [1, 2, 4, 8, 15, 28, 52, 96]
2: [2, 3, 4, 8, 16, 29, 52, 88]
3: [3, 4, 6, 8, 39, 59, 72, 85]
4: [4, 5, 40, 55, 58, 70, 76, 82]
5: [5, 6, 12, 24, 39, 72, 74, 76]
6: [6, 12, 23, 40, 68, 70, 71, 72]
7: 8: 9: 10: 11: 12: 13: 14: 15: 16: 17: 18: 19: 20: 21: 22: 23: 24: 25: 26: 27: 28: 29: 30: 31: 32: 33: 34: 35: 36: 37: 38: 39: 40: 41: 42: 43: 44: 45: 46: 47: 48: 49: 50: 51: 52: 53: 54: 55: 56: 57: 58: 59: 60: 61: 62:
atMostBadBags=3, bagCount=8, atMostCoinsPerBag=174, time=39683.78ms, solution=[6, 12, 23, 40, 68, 70, 71, 72]
1: [1, 2, 4, 8, 15, 28, 52, 96, 165]
2: [2, 3, 4, 8, 16, 29, 52, 88, 147]
3: [3, 4, 6, 8, 20, 44, 82, 113, 144]
4: [4, 5, 8, 16, 56, 78, 109, 111, 140]
5: [5, 6, 10, 26, 38, 52, 66, 132, 135]
6: [6, 7, 12, 24, 39, 66, 124, 126, 128]
7: [7, 9, 18, 36, 48, 60, 116, 120, 124]
8: [8, 16, 32, 36, 64, 77, 118, 119, 120]
9: 10: 11: 12: 13: 14: 15: 16: 17: 18: 19: 20: 21: 22: 23: 24: 25: 26: 27: 28: 29: 30: 31: 32: 33: 34: 35: 36: 37: 38: 39: 40: 41: 42: 43: 44: 45: 46: 47: 48: 49: 50: 51: 52: 53: 54: 55: 56: 57: 58: 59: 60: 61: 62: 63: 64: 65: 66: 67: 68: 69: 70: 71: 72: 73: 74: 75: 76: 77: 78: 79: 80: 81: 82: 83: 84: 85: 86: 87: 88: 89: 90: 91: 92: 93: 94: 95: 96: 97: 98: 99: 100: 101: 102: 103: 104: 105: 106: 107: 108: 109:
atMostBadBags=3, bagCount=9, atMostCoinsPerBag=174, time=12826982.70ms, solution=[8, 16, 32, 36, 64, 77, 118, 119, 120]
1:

 */
