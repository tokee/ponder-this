package dk.ekot.ibm;

import java.util.Arrays;

/**
 * http://www.research.ibm.com/haifa/ponderthis/challenges/January2016.html
 * ...
 * So, for example, using sets of size 3, we can get to N=19 by S_1={1,3,6} and S_2={2,3,5}.
 */
public class Jan2016 {

    /*

    The code below works from a functional point of view, but runtime is horrible
    as it uses dumb brute force for constructing candidates and checking them.
    Runtime is roughly O(n^6) for the challenge of finding 2 6-tuples with N=56.

    Ideas for improvements:
    - Linear scaling by threading
    - Thomas Egense: Pair-primes (41 & 43) means that 42 must be calculated. Either by 21*2, 6*7 or 3*14.
    - If the tuples are sorted, checking can be a lot faster by starting from the lower end

    Observations

    s1[3, 9, 10, 11, 13, 24] s2[1, 2, 3, 4, 5, 14] 56
    s1[3, 5, 6, 8, 13, 17] s2[1, 3, 4, 6, 7, 9] 57
     */

    public static int findMaxNaive(int tuple1, int tuple2, int max) {
        return findMaxNaive(tuple1, tuple2, max, tuple1, tuple2);
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

    private static boolean[] cache = null; // In case of multi threading, make the cache (and the methods) non-static
    public static int getMaxBitmap(int[] s1, int[] s2) {
        if (cache == null || cache.length < s1[s1.length-1] * s2[s2.length-2]) {
            cache = new boolean[s1[s1.length-1] * s2[s2.length-1] + 3]; // + 3 to ensure false at end
        } else {
            Arrays.fill(cache, false);
        }
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
}
