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
import java.util.concurrent.atomic.AtomicInteger;
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

    Level 3: Keep a stack of values that cannot be used anywhere
    [1]: [1]
    [1, 2]: [1, 2, 3]
    [1, 3]: [1, 3, 4]
    [1, 2, 4]: [1, 2, 3, 4, 5, 6, 7]
    [1, 2, 5]: [1, 2, 4, 5, 6, 7, 8]
    When the next number is to be tried, iterate through non-taken values and test those
 */
public class Aug2016 {
    private static Log log = LogFactory.getLog(Aug2016.class);
    public static final int GOOD = 10;
    public static final int BAD = 9;

    public static void main(String[] args) {

//        checkValidate1();
//        checkValidate2();
//        checkValidate3();

        //permutateRun(3, 7, 174, false, true);
        //permutateRun(3, 8, 6, 174, true, true);
        //permutateRun(3, 10, 1, 174, true, true);

  //      onlyValid(6, 100);


        for (int bags = 1 ; bags < 10 ; bags++) {
            onlyValid(bags, 174);
        }

        /*
        final int BAGS = 10;
        final int MAX = 174;
        //permutateRun(Math.min(BAGS, 3), BAGS, 100, true, true);
        for (int bags = 1 ; bags <= BAGS ; bags++) {
            permutateRun(Math.min(bags, 3), bags, 1, MAX, true, true);
        }
        */
    }

    public static void onlyValid(int bagCount, int atMostCoinsPerBag) {
        int[] bags = new int[bagCount+1];
        bags[0] = 2; // Starting value-1
        int[] best = new int[bagCount+1];
        boolean[][] used = new boolean[bagCount+1][(int) Math.pow(atMostCoinsPerBag, 3)];
        AtomicInteger atMost = new AtomicInteger(atMostCoinsPerBag);
        long startTime = System.nanoTime();
        boolean ok = onlyValid(1, bags, best, used, atMost, new AtomicInteger(used[0].length));
        System.out.println(String.format("bags=%d, atMost=%d, time=%.2fms, pass=%b, result=%s",
                                         bagCount, atMostCoinsPerBag, (System.nanoTime()-startTime)/1000000.0, ok,
                                         ok ? toString(best, 1, best.length) : "N/A"));
    }

    private static boolean onlyValid(int bag, int[] bags, int[] best, boolean[][] usedStack, AtomicInteger atMost,
                                     AtomicInteger highestCache) {
        if (bag == usedStack.length) {
            System.out.println(toString(bags, 1, bags.length));
            System.arraycopy(bags, 0, best, 0, bags.length);
            atMost.set(Math.min(atMost.get(), bags[bags.length-1]));
            atMost.decrementAndGet();
            highestCache.set(atMost.get()*atMost.get()*atMost.get()); // Math.pow uses floating point and is slower
            return true;
        }

        final boolean[] previousCache = usedStack[bag-1];
        boolean[] cache = usedStack[bag];
        boolean someFound = false;
/*        if (bag == 3 && toString(bags, 1, bags.length).startsWith("[3, 6, ")) { // [3, 6, 12, 20, 24, 25]
            System.out.println("Nearly there: " + toString(bags, 1, bag));
        }*/
        for (int coins = bags[bag-1]+1 ; coins <= atMost.get() ; coins++) {
            if (previousCache[coins]) {
                continue;
            }
            bags[bag] = coins;
            System.arraycopy(usedStack[bag-1], 0, cache, 0, highestCache.get());
            if (!markAndCheckAllPermutations(bags, bag, cache, atMost.get())) {
                continue;
            }
            if (onlyValid(bag+1, bags, best, usedStack, atMost, highestCache)) {
                someFound = true;
            }
        }
        return someFound;
    }

    private static boolean markAndCheckAllPermutations(int[] bags, int bagIndex, boolean[] cache, int maxBagSize) {
        final int coins = bags[bagIndex];
        cache[coins] = true;
        for (int i2 = 1 ; i2 < bagIndex ; i2++) { // Iterate previous bags to populate cache
            final int c2 = bags[i2];
            final int sum2 = coins+c2;
            if (cache[sum2]) { // 2 bad bags: a+b=c
                return false;
            }
            cache[sum2] = true;

            for (int i3 = 1 ; i3 < i2 ; i3++) { // 3 bad bags: a*b*c=d
                final int c3 = bags[i3];
                final int sum3a = c3+coins+c2;
                if (cache[sum3a]) { // 2 bad bags: a+c=d
                    return false;
                }
                cache[sum3a] = true;
            }
        }
        return true; // No collisions
    }

    private static String toString(int[] values, int start, int end) {
        StringBuilder sb = new StringBuilder(100);
        sb.append("[");
        for (int i = start ; i < end ; i++) {
            if (i != start) {
                sb.append(", ");
            }
            sb.append(values[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private static void checkValidate1() {
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
    private static void checkValidate2() {
        int[] bags = new int[]{174, 173, 172, 170, 117, 88, 48, 24, 12, 6};
        boolean[] existing = new boolean[bags.length*174*Math.max(GOOD, BAD)];
        if (!validates(bags, bags.length, existing, 3)) {
            System.out.println("Problem: " + Arrays.toString(bags) + " did not validate, but it should");
            return;
        }
        System.out.println("All OK: " + Arrays.toString(bags) + " did validate");
    }

    private static void checkValidate3() {
        int[] bags = new int[]{3, 6, 11, 12, 13};
        boolean[] existing = new boolean[bags.length*20*Math.max(GOOD, BAD)];
        if (!validates(bags, bags.length, existing, 3)) {
            System.out.println("Problem: " + Arrays.toString(bags) + " did not validate, but it should");
            return;
        }
        System.out.println("All OK: " + Arrays.toString(bags) + " did validate");
    }

    private static void permutateRun(int atMostBadBags, int bagCount, int atLeastCoinsPerBag, int atMostCoinsPerBag,
                                     boolean print, boolean forward) {
        final int[] best = new int[bagCount];
        Arrays.fill(best, atMostCoinsPerBag);
        final int[] bags = new int[bagCount];
        Arrays.fill(bags, atMostCoinsPerBag);
        final boolean[] existing = new boolean[bagCount*atMostCoinsPerBag*Math.max(GOOD, BAD)];
        AtomicLong solutionCount = new AtomicLong(0);

        long startTime = System.nanoTime();
        if (forward) {
            permutateAndCheckForward(
                    bags, best, existing, 0, atMostBadBags, print, solutionCount, atLeastCoinsPerBag);
        } else {
            permutateAndCheckBackwards(
                    bags, best, existing, 0, atMostBadBags, print, solutionCount, atLeastCoinsPerBag);
        }
        System.out.println(String.format(
                "\natMostBadBags=%d, bagCount=%d, atMostCoinsPerBag=%d, time=%.2fms, solution=%s",
                atMostBadBags, bagCount, atMostCoinsPerBag, (System.nanoTime()-startTime)/1000000.0,
                Arrays.toString(best)));
    }

    private static boolean permutateAndCheckForward(
            int[] bags, int[] best, boolean[] existing, int index, int atMostBadBags, boolean print,
            AtomicLong solutionCount, int atLeastCoinsPerBag) {
        if (index > 0) {
            if (!validates(bags, index, existing, atMostBadBags)) {
                return false;
            }
            if (index == bags.length) {
                return true;
            }
        }

        final int startCoins = index == 0 ? atLeastCoinsPerBag : bags[index - 1] + 1;
        final int lastIndex = best.length-1;
        final int bestLeft = best.length-index+1;
        for (int coins = startCoins ; coins < best[lastIndex]-bestLeft ; coins++) {
            if (index == 0) {
                System.out.print(coins + ": ");
            }
            bags[index] = coins;
            if (permutateAndCheckForward(bags, best, existing, index+1, atMostBadBags, print, solutionCount,
                                  atLeastCoinsPerBag)) {
                solutionCount.addAndGet(1);
                if (index == lastIndex && print) {
                    System.out.println(Arrays.toString(bags));
                }
                System.arraycopy(bags, 0, best, 0, bags.length);
            }
        }
        return false;
    }


    private static boolean permutateAndCheckBackwards(
            int[] bags, int[] best, boolean[] existing, int index, int atMostBadBags, boolean print,
            AtomicLong solutionCount, int atLeastCoinsPerBag) {
        if (index > 0) {
            if (!validates(bags, index, existing, atMostBadBags)) {
                return false;
            }
            if (index == bags.length) {
                return true;
            }
        }

        final int startCoins = index == 0 ? bags[0] : bags[index - 1] - 1;
        final int endCoins = Math.max(atLeastCoinsPerBag, best.length - index);
        for (int coins = startCoins; coins >= endCoins; coins--) {
            if (index == 0) {
                System.out.print(coins + ": ");
            }
            bags[index] = coins;
            if (permutateAndCheckBackwards(bags, best, existing, index + 1, atMostBadBags, print, solutionCount,
                                           atLeastCoinsPerBag)) {
                if (index > 0) {
                    return true;
                }
                if (print || solutionCount.getAndAdd(1) % 100 == 0) {
                    System.out.println(Arrays.toString(bags));
                }
                System.arraycopy(bags, 0, best, 0, bags.length);
            }
        }
        return false;
    }
    private static boolean validates(int[] bags, int bagCount, boolean[] existing, int atMostBadBags) {
        //return validatesV1(bags, bagCount, existing, atMostBadBags);
        return validatesV2(bags, bagCount, existing);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    private static boolean validatesV2(final int[] bags, int bagCount, boolean[] existing) {
        Arrays.fill(existing, false);
        for (int i1 = 0 ; i1 <  bagCount ; i1++) {
            final int i1v = bags[i1];
            if (existing[i1v]) {
//                System.out.println("Problem: Existing value for i1v=" + i1v);
                return false;
            }
            existing[i1v] = true;
            for (int i2 = i1+1 ; i2 <  bagCount ; i2++) {
                final int i2v = bags[i2];
                if (existing[i1v + i2v]) {
                    System.out.println("Problem: Existing value for i1v=" + i1v + " + i2v=" + i2v
                                       + " => " + (i1v + i2v));
                    return false;
                }
                existing[i1v + i2v] = true;
                for (int i3 = i2 + 1; i3 < bagCount; i3++) {
                    final int i3v = bags[i3];
                    if (existing[i1v + i2v + i3v]) {
//                        System.out.println("Problem: Existing value for i1v=" + i1v + " + i2v=" + i2v+ " + i3v=" + i3v
//                                           + " => " + (i1v + i2v + i3v));
                        return false;
                    }
                    existing[i1v + i2v + i3v] = true;
                }
            }
        }
        return true;
    }

    private static boolean validatesV1(int[] bags, int bagCount, boolean[] existing, int atMostBadBags) {
        if (bagCount < 2) {
            return true;
        }
        int idealSum = 0;
        for (int i = 0; i < bagCount; i++) {
            int val = bags[i];
            idealSum += val * GOOD;
        }
        Arrays.fill(existing, false);
        for (int badBags = 1 ; badBags <= atMostBadBags ; badBags++) { // a+b+c == d+e == f
//                Arrays.fill(existing, false);
            if (!validatesV1(bags, bagCount, existing, 0, badBags, idealSum)) {
                return false;
            }
        }
        return true;
    }

    private static boolean validatesV1(
            int[] bags, int bagCount, boolean[] existing, int nextFreeBad, int missingBad, int sum) {
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
            if (!validatesV1(bags, bagCount, existing, bag + 1, missingBad - 1, sum)) {
                return false;
            }
            sum -= diff;
        }
        return true;
    }
}

/*

atMostBadBags=1, bagCount=1, atMostCoinsPerBag=174, time=0.62ms, solution=[1]
atMostBadBags=2, bagCount=2, atMostCoinsPerBag=174, time=0.14ms, solution=[1, 2]
atMostBadBags=3, bagCount=3, atMostCoinsPerBag=174, time=0.32ms, solution=[1, 2, 4]
atMostBadBags=3, bagCount=4, atMostCoinsPerBag=174, time=0.84ms, solution=[1, 2, 4, 8]
atMostBadBags=3, bagCount=5, atMostCoinsPerBag=174, time=4.21ms, solution=[1, 2, 4, 8, 15]
atMostBadBags=3, bagCount=6, atMostCoinsPerBag=174, time=12.20ms, solution=[3, 6, 12, 20, 24, 25]
atMostBadBags=3, bagCount=7, atMostCoinsPerBag=174, time=506.75ms, solution=[3, 6, 12, 22, 41, 42, 43]

atMostBadBags=3, bagCount=8, atMostCoinsPerBag=174, time=219946.00ms, solution=[72, 71, 70, 68, 40, 23, 12, 6] // permutateRun(3, 8, 174, true, false); validate1
atMostBadBags=3, bagCount=8, atMostCoinsPerBag=174, time=60396.82ms, solution=[6, 12, 23, 40, 68, 70, 71, 72] //  permutateRun(3, 8, 174, true, true); validate1
atMostBadBags=3, bagCount=8, atMostCoinsPerBag=174, time=70554.31ms, solution=[6, 12, 23, 40, 68, 70, 71, 72] // permutateRun(3, 8, 174, true, true); validate2
Just about forever: permutateRun(3, 8, 174, true, false); // validate2

atMostBadBags=3, bagCount=9, atMostCoinsPerBag=174, time=12826982.70ms, solution=[8, 16, 32, 36, 64, 77, 118, 119, 120]
9 bags: [1, 2, 30, 56, 68, 91, 106, 110, 114] (not finished)

 */
