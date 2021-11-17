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

import org.apache.commons.lang.math.LongRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 *
 */
public class RandomPattern {
    private static final Logger log = LoggerFactory.getLogger(RandomPattern.class);

    public static void main(String[] args) {
        /*
        0 BLUES=16.48
        1 CLASSICAL=9.97
        2 RAP=16.97
        3 COUNTRY=11.97
        4 POP=16.23
        0 ALTERNATIVE=16.48
        5 ROCK=15.64
        3 CLASSIC_ROCK=11.97
         */
//        findDivisor();

        //findRandomInt();
        findRandomDouble();
    }

    private static void findRandomInt() {
        double[] raw = new double[]{16.48, 9.97, 16.97, 11.97, 16.23, 16.48, 15.64, 11.97};
        int[] wanted = new int[]{0, 1, 2, 3, 4, 0, 5, 3};

        double[] delta = new double[]{6.51, 0, 7, 2, 6.26, 6.51, 5.67, 2}; // From 0.97
        int[] wanted2 = new int[]{651, 0, 7, 2, 626, 651, 576, 2};
        int[] wanted3 = new int[]{101, 0, 7, 2, 76, 101, 26, 2}; // > 10 -> +550
        int[] wanted4 = new int[]{86, 0, 7, 2, 61, 86, 11, 2}; // > 10 -> +565
        int[] wanted5 = new int[]{83, 0, 7, 2, 58, 83, 8, 2}; // > 10 -> +568
        // 3015435
        int highest = 0;
        int ceil = Arrays.stream(wanted).max().getAsInt()+1;
        outer:
        for (long seed = 0 ; seed < Long.MAX_VALUE ; seed++) {
//            if (((i >> 14) & 1) == 1) {
//                System.out.print(".");
//            }
            Random r = new Random(seed);
            int l = 0;
            for (int w: wanted) {
                if (r.nextInt(ceil) != w) {
                    if (l > highest) {
                        System.out.println(l + " seed=" + seed);
                        highest = l;
                    }
                    continue outer;
                }
                ++l;
            }
            System.out.println("\nMatching seed: " + seed);
            break;
        }
    }

    private static void findRandomDouble() {
        final double jitter = 0.1;

        double[] rawE = new double[]{16.48, 9.97, 16.97, 11.97, 16.23, 16.48, 15.64, 11.97}; // enum 0.5: 50545511251
        double[] delta = new double[]{6.51, 0, 7, 2, 6.26, 6.51, 5.67, 2}; // From 9.97   Jitter 0.5 -> 35301108

        double[] rawS = new double[]{16.48, 16.48, 9.97, 11.97, 11.97, 16.23, 16.97, 15.64}; // toString-sorted
        double[] rawD = new double[]{15.64, 16.97, 16.48, 16.23, 9.97, 11.97, 11.97, 16.48}; // unsorted

        double[] wanted = rawD;
        AtomicInteger highest = new AtomicInteger(0);
        double min = 10; //(int)Arrays.stream(wanted).min().getAsDouble();
        System.out.println("min=" + min);
        double[] zeroed = Arrays.stream(wanted).map(value -> value-min).toArray();
        double max = Arrays.stream(zeroed).max().getAsDouble();
        double ceil = Math.floor(max+1);

        //double ceil = Arrays.stream(wanted).max().getAsDouble()+1;
        //double ceil = 18; //Math.floor(Arrays.stream(wanted).max().getAsDouble()+1);
        System.out.println("ceil = " + ceil);

        OptionalLong workingSeed = LongStream.range(0, 10000L*Integer.MAX_VALUE).parallel()
                .filter(seed -> isMatch(jitter, zeroed, highest, ceil, seed))
                .findAny();

        System.out.println("\nMatching seed: " + workingSeed);
    }

    private static boolean isMatch(double jitter, double[] wanted, AtomicInteger highest, final double ceil, long seed) {
        boolean match = true;
        final Random r = new Random(seed);
        int l = 0;
        for (int i = 0, wantedLength = wanted.length; i < wantedLength; i++) {
            double w = wanted[i];
            double guess = r.nextDouble() * ceil;
            if (guess > w + jitter || guess < w - jitter) {
                if (l > highest.get()) {
                    highest.set(l);
                    System.out.println(l + " seed=" + seed);
                }
                match = false;
                break;
            }
            ++l;
        }
        return match;
    }

    private static void findDivisor() {
        double[] raw = new double[]{16.48, 9.97, 16.97, 11.97, 16.23, 16.48, 15.64, 11.97};

        double minDist = Double.MAX_VALUE;
        for (double divisor = 0.1 ; divisor < 5 ; divisor += 0.001) {
            double maxMin = 0;
            for (double r: raw) {
                maxMin = Math.max(maxMin, Math.abs((r/divisor) - Math.round(r/divisor)));
            }
            if (maxMin < minDist) {
                minDist = maxMin;
                System.out.println("Divisor " + divisor + " has minDist=" + minDist);
            }
        }
    }

}
