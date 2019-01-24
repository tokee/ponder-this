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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//  https://mathsgear.co.uk/products/truncatable-prime-pencil
public class CuttablePrime {
    public static final int CERTAINITY = 15;

    public static void main(String[] args) {
        for (int radix = 10 ; radix <= 10 ; radix++) {
            //System.out.println("Counting cuttable primes for radix " + radix);
            if (radix == 18) { // Takes ½ hour
                //System.out.println("43: af93e41a586he75a7hhaab7he12fg79992ga7741b3d (10024849 total truncable primes)");
                System.out.println("Total count for radix 18: 14979714 (cached calculation), sample of longest: 43: af93e41a586he75a7hhaab7he12fg79992ga7741b3d");
            } else {
                cuttable(radix);
            }
        }
    }

    private static void cuttable(int radix) {
        long startTime = System.nanoTime();
//        final char[] bases = basePrimes(radix).toCharArray();
        final char[] bases = basePositive(radix).toCharArray();
        List<Integer> buckets = new ArrayList<>();
        AtomicInteger longest = new AtomicInteger(0);
        StringBuilder last = new StringBuilder();
        extend(bases, radix, "", longest, buckets, 1, last);
//        System.out.println("*************");
        int total = 0;
        for (int i = 1 ; i < buckets.size() ; i++) {
//            System.out.println("Length " + i + ": " + buckets.get(i));
            total += buckets.get(i);
        }
        System.out.println("Total count for radix " + radix + ": " + total + ", sample of longest: " + last +
                           ". Spend time: " + (System.nanoTime()-startTime)/1000000 + " ms");
    }


    private static void extend(char[] bases, int radix, String previous, AtomicInteger longest, List<Integer> buckets,
                               final long maxChecksAccumulated, StringBuilder last) {
        int localChecksLeft = bases.length;
        for (char base: bases) {
            String candidate = base + previous;
            if (new BigInteger(candidate, radix).isProbablePrime(CERTAINITY)) {
                while (buckets.size() < candidate.length()+1) {
                    buckets.add(0);
                }
                buckets.set(candidate.length(), buckets.get(candidate.length())+1);
                if (candidate.length() >= longest.get()) {
                    longest.set(candidate.length());
                    String message = longest.get() + ": " + candidate;
                    last.setLength(0);
                    last.append(message);
                    System.out.println(
                            "Radix " + radix + ", length " + message + " (" +
                            buckets.stream().mapToInt(Integer::intValue).sum() + " total truncable primes so far)");
                }
                extend(bases, radix, candidate, longest, buckets, maxChecksAccumulated*localChecksLeft, last);
            }
            localChecksLeft--;
        }
    }

    private static String basePrimes(int radix) {
        String bases = "";
        for (int i = 0 ; i < radix ; i++) {
            if (BigInteger.valueOf(i).isProbablePrime(15)) {
                bases += Integer.toString(i, radix);
            }
        }
//        System.out.println("Base primes (radix " + radix + "): " + bases);
        return bases;
    }

    private static String basePositive(int radix) {
        String bases = "";
        for (int i = 1 ; i < radix ; i++) {
            bases += Integer.toString(i, radix);
        }
//        System.out.println("Base positives (radix " + radix + "): " + bases);
        return bases;
    }

    /*

Total count for radix 2: 0, sample of longest:
Total count for radix 3: 3, sample of longest: 3: 212
Total count for radix 4: 16, sample of longest: 6: 333323
Total count for radix 5: 15, sample of longest: 6: 222232
Total count for radix 6: 454, sample of longest: 17: 14141511414451435
Total count for radix 7: 22, sample of longest: 7: 6642623
Total count for radix 8: 446, sample of longest: 15: 313636165537775
Total count for radix 9: 108, sample of longest: 10: 2462868287
Total count for radix 10: 4260, sample of longest: 24: 357686312646216567629137
Total count for radix 11: 75, sample of longest: 9: a68822827
Total count for radix 12: 170053, sample of longest: 32: 471a34a164259ba16b324ab8a32b7817
Total count for radix 13: 100, sample of longest: 8: cc4c8c65
Total count for radix 14: 34393, sample of longest: 26: c6143392ccbb3d11ac22cc5543
Total count for radix 15: 9357, sample of longest: 22: 6c6c2ce2ceeea4826e642b
Total count for radix 16: 27982, sample of longest: 25: dbc7fba24fe6aec462abf63b3
Total count for radix 17: 362, sample of longest: 11: 6c66cc4cc83
Total count for radix 18: 14979714 (cached calculation), sample of longest: 43: af93e41a586he75a7hhaab7he12fg79992ga7741b3d
Total count for radix 19: 685, sample of longest: 14: cieg86gcea2c6h
Total count for radix 20: 3062899, sample of longest: 37: fc777g3cg1fidi9i31ie5ffb379c7a3f6efid (2014834 total truncable primes)
Total count for radix 21: 59131, sample of longest: 27: g8agg2gca8cak4k68gea4g2k22h (41447 total truncable primes)
Total count for radix 22: 1599447, sample of longest: 37: ffhalc8jfb9jka2ah9fab4i9l5i9l3gf8d5l5 (445142 total truncable primes)
Total count for radix 23: 1372, sample of longest: 17: immgm6c6imci66a4h (927 total truncable primes)


     */
}
