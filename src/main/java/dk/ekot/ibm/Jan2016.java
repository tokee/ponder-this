package dk.ekot.ibm;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * http://www.research.ibm.com/haifa/ponderthis/challenges/January2016.html
 * ...
 * So, for example, using sets of size 3, we can get to N=19 by S_1={1,3,6} and S_2={2,3,5}.
 */
@SuppressWarnings("ForLoopReplaceableByForEach")
public class Jan2016 {

    /*

    The code below works from a functional point of view, but runtime is horrible
    as it uses dumb brute force for constructing candidates and checking them.
    Runtime is roughly O(n^6) for the challenge of finding 2 6-tuples with N=56.

    Ideas for improvements:
    - Linear scaling by threading
    - Thomas Egense: Twin-primes (41 & 43) means that 42 must be calculated. Either by 21*2, 6*7 or 3*14.
    +-1: 4, 6, 12, 18, 30, 42, 60, 72, 102, 108, 138, 150, 180, 192, 198, 228, 240, 270, 282

    - We _must_ have 1 in one of the tuples as first non-prime after 1 is 4
    - We _must_ have either 2 or 3 in the other tuple

    - If the tuples are sorted, checking can be a lot faster by starting from the lower end

    - Calculate all sets of which at least one tuple must be present
      - One from each set from max down to 3 must be present in the final tuples

    Observations

    s1[3, 9, 10, 11, 13, 24] s2[1, 2, 3, 4, 5, 14] 56
    s1[3, 5, 6, 8, 13, 17] s2[1, 3, 4, 6, 7, 9] 57

    Gear 58: [2, 5, 7, 8, 9, 13], [1, 3, 4, 6, 7, 9]
     */

    public static final int[][][] TWINS = new int[][][] {
            {{  3}, {3, 1}, {2, 1}}, // Not a twin, but needed to have either 2 or 3 as starting
            {{  4}, {3, 1}, {4, 1}, {5, 1}}, // 4
            {{  6}, {5, 1}, {6, 1}, {7, 1}, {3, 2}}, // 6
            {{ 12}, {11, 1}, {12, 1}, {13, 1}, {6, 2}, {4, 3}}, // 12
            {{ 19}, {17, 1}, {18, 1}, {19, 1}, {9, 2}, {6, 3}}, // 18
            {{ 30}, {29, 1}, {30, 1}, {31, 1}, {15, 2}, {6, 5}}, // 30
            {{ 44}, {41, 1}, {42, 1}, {43, 1}, {21, 2}, {14, 3}, {7, 6}}, // 42
            {{ 61}, {59, 1}, {60, 1}, {61, 1}, {30, 2}, {15, 4}, {10, 6}}, // 60
            {{ 77}, {71, 1}, {72, 1}, {73, 1}, {36, 2}, {18, 4}, {9, 8}}, // 72
            {{103}, {101, 1}, {102, 1}, {103, 1}, {51, 2}, {17, 6}} // 102
    };


    public static boolean hasSolutionWithTwins(int tuple1, int tuple2, int required) {
        List<Set<Pair>> rules = getRuleSets(required);
        return hasSolutionWithRules(tuple1, tuple2, new LinkedHashSet<>(), new LinkedHashSet<>(), rules, 0);
    }

    public static int largestSolutionWithRules(int maxSetSize) {
        return largestSolutionWithRules(maxSetSize, maxSetSize);
    }

    public static int largestSolutionWithRules(int maxSetSize1, int maxSetSize2) {
        return largestSolutionWithRules(maxSetSize1, maxSetSize2, maxSetSize1*maxSetSize2*3);
    }

    public static int largestSolutionWithRules(int maxSetSize1, int maxSetSize2, int maxGear) {
        long startMS = System.currentTimeMillis();

        List<Set<Pair>> rules = getRuleSets(maxGear);
        AtomicInteger largestGear = new AtomicInteger(0);
        largestSolutionWithRules(maxSetSize1, maxSetSize2, new boolean[maxGear*maxGear], new boolean[maxGear*maxGear],
                                 0, 0, simplify(rules), 0, largestGear);
        System.out.println("Finished in " + (System.currentTimeMillis()-startMS)/1000 + " seconds");
        return largestGear.get();
    }

    public static List<Set<Pair>> getRuleSets(int maxGear) {
        List<Set<Pair>> rules = new ArrayList<>(maxGear);
        int totalRules = 0;
        for (int i = 0; i <= maxGear ; i++) {
            if (i < 2) {
                rules.add(Collections.emptySet());
            } else if (i == 2) { // 59 & 61 are twin primes and we guess we will reach 60 and that 59 and 61 will not be used
                rules.add(toPairs(new int[][]{{19, 3}, {29, 2}}));

/*            } else if (i == 2) { //  [[1, 1], [1, 2], [1, 3], [3, 1], [2, 1]]
                // We are allowed to make 1 rule non-mirrored, so we choose the one with largest percentage impact
                Set<Pair> threeRule = new LinkedHashSet<>();
                threeRule.add(new Pair(2, 1));
                threeRule.add(new Pair(3, 1));
                threeRule.add(new Pair(1, 1));
                rules.add(threeRule);         */
//                System.out.println(i + ": " + toString(rules.get(rules.size()-1)));
            } else {
                rules.add(getNeededPairsWithGap(i));
//                System.out.println(i + ": " + toString(rules.get(rules.size()-1)));
            }
            totalRules += rules.get(rules.size()-1).size();
        }
        System.out.println("Calculated " + totalRules + " rules for checking up to " + maxGear + " gears");
        return rules;
    }

    private static Set<Pair> toPairs(int[][] ints) {
        Set<Pair> setPairs = new LinkedHashSet<>(ints.length);
        for (int[] anInt : ints) {
            setPairs.add(new Pair(anInt[0], anInt[1]));
        }
        return setPairs;
    }

    private static int[][][] simplify(List<Set<Pair>> rules) {
        int[][][] simple = new int[rules.size()][][];
        for (int i = 0 ; i < rules.size() ; i++) {
            Set<Pair> ruleSet = rules.get(i);
            simple[i] = new int[ruleSet.size()][];
            int j = 0;
            for (Pair rule: ruleSet) {
                simple[i][j] = new int[2];
                simple[i][j][0] = rule.s1;
                simple[i][j][1] = rule.s2;
                j++;
            }
        }
        return simple;
    }

    private static boolean hasSolutionWithRules(
            int tuple1, int tuple2, Set<Integer> set1, Set<Integer> set2, List<Set<Pair>> rules, int index) {
        if (set1.size() > tuple1 || set2.size() > tuple2) { // A tuple got too big
            return false;
        }
        if (index == rules.size()) { // Reached end
            System.out.println("Found " + set1 + ", " + set2);
            return true;
        }
        if (rules.get(index).isEmpty()) { // 0-2
            if (hasSolutionWithRules(tuple1, tuple2, set1, set2, rules, index+1)) {
                return true;
            }
        }

        int test = 0;
        for (Pair rule: rules.get(index)) {
            test++;
            if (index == 3) {
                System.out.println("Checking " + test + "/" + rules.get(index).size() + " " + rule);
            } else if (index == 4) {
                System.out.println(" - Lev.2 " + test + "/" + rules.get(index).size() + " " + rule);
            } else if (index == 5) {
                System.out.println("   - L.3 " + test + "/" + rules.get(index).size() + " " + rule);
            }
            Set<Integer> sub1 = new LinkedHashSet<>(set1);
            sub1.add(rule.s1);
            Set<Integer> sub2 = new LinkedHashSet<>(set2);
            sub2.add(rule.s2);
            if (hasSolutionWithRules(tuple1, tuple2, sub1, sub2, rules, index+1)) {
                return true;
            }
        }
        return false;
    }

    private static void largestSolutionWithRules(
            final int maxSetSize1, final int maxSetSize2, final boolean[] set1, final boolean[] set2,
            int setSize1, int setSize2, final int[][][] rules, final int gear, final AtomicInteger largestGear) {
        if (setSize1 > maxSetSize1 || setSize2 > maxSetSize2) { // A tuple got too big, back up 1 gear
            if (largestGear.get() < gear-1) {
                largestGear.set(gear - 1);
            }
            return;
        }
        if (gear == rules.length) { // Reached end
            if (largestGear.get() < gear-1) {
                largestGear.set(gear-1);
                System.out.println("Max " + largestGear + " reached: " + toString(set1) + ", " + toString(set2));
            }
            return;
        }
        if (rules[gear].length == 0) { // 0-2
            largestSolutionWithRules(maxSetSize1, maxSetSize2, set1, set2, setSize1, setSize2, rules, gear+1, largestGear);
            return;
        }

        iterateRules(maxSetSize1, maxSetSize2, set1, set2, setSize1, setSize2, rules, gear, largestGear);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    private static void iterateRules(int maxSetSize1, int maxSetSize2, boolean[] set1, boolean[] set2,
                                     int setSize1, int setSize2, int[][][] rules, int gear, AtomicInteger largestGear) {
/*        if (alreadySatisfied(set1, set2, rules, gear, largestGear, true)) {
            if (largestGear.get() < gear) {
                largestGear.set(gear);
                System.out.println("Gear " + largestGear + ": " + toString(set1) + ", " + toString(set2));
            }
            iterateRules(maxSetSize1, maxSetSize2, set1, set2, setSize1, setSize2, rules, gear+1, largestGear);
            return;
        }*/
        if (largestGear.get() < gear) {
            iteratePotentialNewMax(maxSetSize1, maxSetSize2, set1, set2, setSize1, setSize2, rules, gear, largestGear);
        } else {
            iterateCannotBeNewMax(maxSetSize1, maxSetSize2, set1, set2, setSize1, setSize2, rules, gear, largestGear);
        }
    }

    private static boolean alreadySatisfied(
            boolean[] set1, boolean[] set2,
            int[][][] rules, int gear, AtomicInteger largestGear, boolean printMatching) {
        final int[][] rule1 = rules[gear];
        for (int i = 0; i < rule1.length; i++) {
            final int[] rule = rule1[i];
            if (set1[rule[0]] && set2[rule[1]]) {
                if (printMatching && gear > largestGear.get()) {
                    System.out.print("M[" + rule[0] + ", " + rule[1] + "] ");
                }
                return true;
            }
        }
        return false;
    }

    private static void iteratePotentialNewMax(
            int maxSetSize1, int maxSetSize2, boolean[] set1, boolean[] set2,
            int setSize1, int setSize2, int[][][] rules, int gear, AtomicInteger largestGear) {
        final int[][] rule1 = rules[gear];
        for (int i = 0; i < rule1.length; i++) {
            final int[] rule = rule1[i];
            final int s1 = rule[0];
            final int s2 = rule[1];
            final boolean wasSet1 = set1[s1];
            final boolean wasSet2 = set2[s2];
            if (((setSize1 == maxSetSize1 && !wasSet1) || (setSize2 == maxSetSize2 && !wasSet2))) { // Will exceed
                if (largestGear.get() < gear-1) { // need the last as it can change in the loop
                    largestGear.set(gear - 1);
                    System.out.println("M[" + rule[0] + ", " + rule[1] + "] Gear "
                                       + largestGear + ": " + toString(set1) + ", " + toString(set2));
                }
                continue;
            }
            set1[s1] = true;
            set2[s2] = true;
            largestSolutionWithRules(
                    maxSetSize1, maxSetSize2, set1, set2,
                    wasSet1 ? setSize1 : setSize1 + 1, wasSet2 ? setSize2 : setSize2 + 1,
                    rules, gear + 1, largestGear);
            // Reset to input
            set1[s1] = wasSet1;
            set2[s2] = wasSet2;
        }
    }

    private static void iterateCannotBeNewMax(
            int maxSetSize1, int maxSetSize2, boolean[] set1, boolean[] set2,
            int setSize1, int setSize2, int[][][] rules, int gear, AtomicInteger largestGear) {
        final int[][] rule1 = rules[gear];
        for (int i = 0; i < rule1.length; i++) {
            final int[] rule = rule1[i];
            final int s1 = rule[0];
            final int s2 = rule[1];
            final boolean wasSet1 = set1[s1];
            final boolean wasSet2 = set2[s2];
            if ((setSize1 == maxSetSize1 && !wasSet1 || setSize2 == maxSetSize2 && !wasSet2)) {
                continue;
            }
            set1[s1] = true;
            set2[s2] = true;
            largestSolutionWithRules(
                    maxSetSize1, maxSetSize2, set1, set2,
                    wasSet1 ? setSize1 : setSize1 + 1, wasSet2 ? setSize2 : setSize2 + 1,
                    rules, gear + 1, largestGear);
            // Reset to input
            set1[s1] = wasSet1;
            set2[s2] = wasSet2;
        }
    }


    private static void largestSolutionWithRules(
            final int maxSetSize1, final int maxSetSize2, final boolean[] set1, final boolean[] set2,
            int setSize1, int setSize2, final List<Set<Pair>> rules, final int gear, final AtomicInteger largestGear) {
        if (setSize1 > maxSetSize1 || setSize2 > maxSetSize2) { // A tuple got too big, back up 1 gear
            if (largestGear.get() < gear-1) {
                largestGear.set(gear-1);
                System.out.println("Previous Gear " + largestGear + ": " + toString(set1) + ", " + toString(set2));
            }
            return;
        }
        if (gear == rules.size()) { // Reached end
            if (largestGear.get() < gear-1) {
                largestGear.set(gear-1);
                System.out.println("Max " + largestGear + " reached: " + toString(set1) + ", " + toString(set2));
            }
            return;
        }
        if (rules.get(gear).isEmpty()) { // 0-2
            largestSolutionWithRules(maxSetSize1, maxSetSize2, set1, set2, setSize1, setSize2, rules, gear+1, largestGear);
            return;
        }

        int test = 0;
        for (Pair rule: rules.get(gear)) {
            test++;
/*            if (gear == 3) {
                System.out.println("Checking " + test + "/" + rules.get(gear).size() + " " + rule);
            } else if (gear == 4) {
                System.out.println(" - 4     " + test + "/" + rules.get(gear).size() + " " + rule);
            } else if (gear == 5) {
                System.out.println("  - 5    " + test + "/" + rules.get(gear).size() + " " + rule);
            } else if (gear == 6) {
                System.out.println("   - 6   " + test + "/" + rules.get(gear).size() + " " + rule);
            } else if (gear == 7) {
                System.out.println("    - 7  " + test + "/" + rules.get(gear).size() + " " + rule);
            } else if (gear == 8) {
                System.out.println("     - 8 " + test + "/" + rules.get(gear).size() + " " + rule);
            }*/
            boolean wasSet1 = set1[rule.s1];
            boolean wasSet2 = set2[rule.s2];
            if ((!wasSet1 && setSize1 == maxSetSize1 || !wasSet2 && setSize2 == maxSetSize2)
                && largestGear.get() < gear) {
                largestGear.set(gear);
                System.out.println("Gear " + largestGear + ": " + toString(set1) + ", " + toString(set2));
                continue;
            }
            set1[rule.s1] = true;
            set2[rule.s2] = true;
            largestSolutionWithRules(
                    maxSetSize1, maxSetSize2, set1, set2,
                    wasSet1 ? setSize1 : setSize1+1, wasSet2 ? setSize2 : setSize2+1,
                    rules, gear+1, largestGear);
            // Reset to input
            set1[rule.s1] = wasSet1;
            set2[rule.s2] = wasSet2;
        }
    }

    // Forces 1 twin pair from each TWINS-row up till the row for max to be part of the tuples
    public static int findMaxTwins(int tuple1, int tuple2, int max) {
        int end = 0;
        for (int[][] twins: TWINS) {
            if (twins[0][0] <= max) {
                end++;
            } else {
                break;
            }
        }
        System.out.println("max=" + max + ", " + end);
        return end;
    }

    // The pairs of which at least 1 must be present to accept the gear
    // This means either the gear itself, one less or 2 less
    public static Set<Pair> getNeededPairsWithGap(int gear) {
        Set<Pair> pairs = new LinkedHashSet<>();
//        for (int i = gear-1 ; i <= gear+1 ; i++) {
//        for (int i = gear+1 ; i >= 1 && i >= gear-1 ; i--) {
//        for (int i = gear ; i >= 1 && i >= gear-2 ; i--) {
            pairs.addAll(getAllPairs(gear));
            pairs.addAll(getAllPairs(gear+1));
            pairs.addAll(getAllPairs(gear-1));
//        }
        return pairs;
    }

    // All possible pairs that calculates the exact gear
    public static Set<Pair> getAllPairs(int gear) {
        // Yeah, yeah, Sieve of Eratosthenes and all that would be nice
        Set<Pair> pairs = new LinkedHashSet<>();
        // Reverse to prioritize large divisors
        for (int i = ((int) Math.sqrt(gear))+1; i >= 1 ; i--) {
//        for (int i = 1 ; i <= Math.sqrt(gear) ; i++) {
            if (gear % i == 0) {
                pairs.add(new Pair(gear/i, i));
                // TODO: For gear 3 we can use one side only: [1, 1] [1, 2] {1, 3]
                // The rest of the pairs must
                pairs.add(new Pair(i, gear/i));
            }
        }
        return pairs;
    }

    public static int findMaxNaive(int tuple1, int tuple2, int max) {
        return findMaxNaive(tuple1, tuple2, max, tuple1, tuple2);
    }

    public static int findMaxFixed(int tupleSize1, int tupleSize2, int max, int fixed1, int fixed2) {
        long startMS = System.currentTimeMillis();
        int[] s1 = new int[tupleSize1];
        int[] s2 = new int[tupleSize2];
        s1[0] = fixed1;
        int result = down1(s1, s2, tupleSize1-2, 1, max, tupleSize1, tupleSize2);
        System.out.println("Finished in " + (System.currentTimeMillis()-startMS)/1000 + " seconds");
        return result;
    }

    public static int findMaxNaive(int tuple1, int tuple2, int max, int s1min, int s2min) {
        long startMS = System.currentTimeMillis();
        int[] s1 = new int[tuple1];
        int[] s2 = new int[tuple2];
        s1[tuple1-1] = max/2+1;
        s2[tuple2-1] = max/2+1;
        int result = down1(s1, s2, tuple1-1, 1, max, s1min, s2min);
        System.out.println("Finished in " + (System.currentTimeMillis()-startMS)/1000 + " seconds");
        return result;
    }

    private static int down1(int[] s1, int[] s2, int index, int best, int max, int s1min, int s2min) {
        if (index == -1) {
            // Reset s2
            for (int j = s2.length-1; j >= 0; j--) {
                s2[j] = max/2 + 1;
            }
            return down2(s1, s2, s2.length-1, best, s2min);
        }
//        if (index == s1.length-1 && s1[index] < s1min) {
//            return best;
//        }
        for (int i = s1[index]-1; i >= 1; i--) {
            s1[index] = i;
            for (int j = index-1; j >= 0; j--) {
                s1[j] = s1[index];
            }
            best = down1(s1, s2, index-1, best, max, s1min, s2min);
        }
        return best;
    }

    private static int down2(int[] s1, int[] s2, int index, int best, int min) {
        if (index == -1) {
            int candidate = getMaxBitmap(s1, s2);
            if (candidate > best) {
                System.out.println("s1[" + toString(s1) + "] s2[" + toString(s2) + "] " + candidate);
                return candidate;
            }
            return best;
        }
//        if (index == s2.length-1 && s2[index] < min) {
//            return best;
//        }
        for (int i = s2[index]-1 ; i >= 1 ; i--) {
            s2[index] = i;
            for (int j = index-1 ; j >= 0 ; j--) {
                s2[j] = s2[index];
            }
            best = down2(s1, s2, index-1, best, min);
        }
        return best;
    }

    public static int getMaxNaive(int[] s1, int[] s2) {
        int last = 0;
        outer:
        while (true) {
            for (int i1 = 0 ; i1 < s1.length ; i1++) {
                for (int i2 = 0 ; i2 < s2.length ; i2++) {
                    final int gear = s1[i1]*s2[i2];
//                    System.out.println("Last=" + last + ", s1=" + s1[i1] + ", s2=" + s2[i2] + ", gear=" + gear);
                    if (gear == last+1 || gear == last+2 || gear == last+3) {
                        last = gear;
                        continue outer;
                    }
                }
            }
            break;
        }
        return last+1;
    }

    private static boolean[] cache = new boolean[10000]; // This should neither be static nor fixed in size
    public static int getMaxBitmap(int[] s1, int[] s2) {
//        System.out.println("Checking s1[" + toString(s1) + "] s2[" + toString(s2) + "]");
        Arrays.fill(cache, false);
        // Fill cache
        for (int i1 = 0 ; i1 < s1.length ; i1++) {
            for (int i2 = 0; i2 < s2.length; i2++) {
                cache[s1[i1] * s2[i2]] = true;
            }
        }

        // Check cache
        for (int i = 1 ; i < cache.length-2 ; i++) {
            if (!cache[i] && !cache[i+1] && !cache[i+2]) {
                return i;
            }
        }
        return 0;
    }

    public static String toString(int[] tuple) {
        StringBuilder sb = new StringBuilder();
        for (int t: tuple) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(Integer.toString(t));
        }
        return sb.toString();
    }

    public static String toString(int[][] d2) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int[] tuple: d2) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append("[");
            sb.append(toString(tuple));
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(Collection<Pair> pairs) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Object o: pairs) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(o);
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(boolean[] bitmap) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0 ; i < bitmap.length ; i++) {
            if (bitmap[i]) {
                if (sb.length() > 1) {
                    sb.append(", ");
                }
                sb.append(Integer.toString(i));
            }
        }
        sb.append("]");
        return sb.toString();
    }


    public static class Pair { // Order is significant
        public final int s1;
        public final int s2;

        private Pair(int s1, int s2) {
            this.s1 = s1;
            this.s2 = s2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Pair pair = (Pair) o;

            return s1 == pair.s1 && s2 == pair.s2;
        }

        @Override
        public int hashCode() {
            int result = s1;
            result = 31 * result + s2;
            return result;
        }

        @Override
        public String toString() {
            return "[" +s1 + ", " + s2 + "]";
        }
    }
}
