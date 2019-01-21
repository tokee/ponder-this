/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.
 */
package dk.ekot.ibm;

import dk.ekot.misc.Bitmap;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * http://www.research.ibm.com/haifa/ponderthis/challenges/January2019.html
 *
 * Alice and Bob are playing the following game:
 * they start from a number N and each one of them in his or her turn (Alice starts) divides N by any divisor that is
 * either a prime or a product of several distinct prime numbers. The winner is the one who gets to one - thus leaving
 * the other player with no legal move.
 *
 * To define the initial N, Alice chooses a number a from a set A, and a number b from a set B.
 * The game is played with N=a+b. Charlie knows that Alice will start, and he wants to let Bob win.
 * He does that by fixing the sets A and B.
 * He can do that, for example, by choosing A=[3,99] and B=[1,22]. (Why?)
 * Your challenge, this month, is to help Charlie find sets A and B with at least four different numbers each,
 * that will allow Bob to win.
 *
 * Bonus '*' for solutions with more than 4 elements in the set B.
 *
 * Analysis: a+b is equal to the product of prime-pairs, Bob wins as he can either divide by a prime to get to 1 or
 * mimic what Alice did to arrive to a new product consisting of prime-pairs.
 *
 * Solution: 1) Generate a list of guaranteed-win products and 2) Create sets A and B so that a+b is in the list.
 *
 */
public class Jan2019 {
    private static Log log = LogFactory.getLog(Jan2019.class);

    public static void main(String[] args) {
        new Jan2019().run();
    }

    private void run() {
        final int maxElement = 100_000;
        final int minALength = 4;
        final int minBLength = 4;
        final int maxResults = 5;
        //fixedA2(maxElement, minBLength);
        long startNS = System.nanoTime();
        earlyElimination(maxElement, minALength, minBLength, maxResults);
    }

    private void earlyElimination(int maxElement, int minALength, int minBLength, int maxResults) {
        System.out.println("Early eliminationwith maxElement=" + maxElement + ", min-A-size=" + minALength +
                           ", min-B-size=" + minBLength);
        final Bitmap validProducts = generateValidProducts(maxElement*2);
        Bitmap validDeltas = getValidDeltas(validProducts, maxElement, minBLength);

        final int[] as = new int[minALength];
        final Bitmap[] candidateBs = new Bitmap[minALength];
        final Bitmap[] validBs = new Bitmap[minALength];
        for (int i = 0 ; i < minALength ; i++) {
            candidateBs[i] = new Bitmap(validProducts.size());
            validBs[i] = new Bitmap(validProducts.size());
        }

        earlyElimination(validProducts, validDeltas, maxElement, minALength, minBLength, as, candidateBs, validBs, 0,
                         new AtomicInteger(maxResults), new AtomicInteger(1), new AtomicInteger(1),
                         System.nanoTime());
    }

    private void earlyElimination(
            Bitmap validProducts, Bitmap validDeltas, int maxElement, int minALength, int minBLength,
            int[] as, Bitmap[] candidateBs, Bitmap[] validBs, final int level,
            AtomicInteger resultsLeft, AtomicInteger printedA, AtomicInteger printedB, long startTime) {
        if (level == minALength) {
            System.out.print(toString(as) + " " + toString(validBs[level-1].getIntegers()));
            System.out.println(" " + (System.nanoTime() - startTime)/1000000/1000 + " seconds");
            resultsLeft.decrementAndGet();
            return;
        }
        final int previousIndex = level == 0 ? 0 : as[level-1];
        int delta = level == 0 ? 1 : validDeltas.thisOrNext(1);
        as[level] = previousIndex+delta;
        while (as[level] <= maxElement) {
            if (level == 0) {
                System.out.print(as[level] + " ");
                if ((as[level] & 31) == 0) {
                    System.out.println();
                }
            }

            validProducts.shift(-as[level], candidateBs[level]);
            if (level == 0) {
                candidateBs[level].copy(validBs[level]);
            } else {
                Bitmap.and(validBs[level-1], candidateBs[level], validBs[level]);
            }

            if (validBs[level].cardinality() >= minBLength) {
                if (level > printedA.get()) {
                    System.out.print(toString(as) + " " + toString(validBs[level].getIntegers()));
                    System.out.println(" " + (System.nanoTime() - startTime) / 1000000 / 1000 + " seconds");
                    printedA.set(level);
                    printedB.set(1);
                } else if (level == printedA.get() && validBs[level].cardinality() > printedB.get()) {
                    System.out.print(toString(as) + " " + toString(validBs[level].getIntegers()));
                    System.out.println(" " + (System.nanoTime() - startTime) / 1000000 / 1000 + " seconds");
                    printedB.set(validBs[level].cardinality());
                }
                earlyElimination(validProducts, validDeltas, maxElement, minALength, minBLength, as, candidateBs, validBs, level + 1,
                                 resultsLeft, printedA, printedB, startTime);
                if (resultsLeft.get() <= 0) {
                    break;
                }
            }

            delta = level == 0 ? delta+1 : validDeltas.thisOrNext(delta+1);
            if (delta == Integer.MAX_VALUE) {
                break;
            }
            as[level] = previousIndex+delta;
        }
        as[level] = 0;
    }
    // Calculate deltas from 1 that has >= minBLength valids

    private Bitmap getValidDeltas(Bitmap validProducts, int maxElement, int minBLength) {
        final Bitmap validDeltas = new Bitmap(maxElement+1);
        final Bitmap reuse = new Bitmap(validProducts.size());

        for (int delta = 1 ; delta < maxElement ; delta++) {
            validProducts.shift(-delta, reuse);
            Bitmap.and(validProducts, reuse, reuse);
            if (reuse.cardinality() >= minBLength) {
                validDeltas.set(delta);
            }
        }
        System.out.println("Calculated valid deltas: " + validDeltas.cardinality() + "/" + maxElement);
        return validDeltas;
    }


    private void fixedA2(int maxElement, int minBLength) {
        final int minALength = 2;

        final Bitmap validProducts = generateValidProducts(maxElement*2);

        final long[] as = new long[minALength];
        final Bitmap validB1s = new Bitmap(validProducts.size());
        final Bitmap candidateB2s = new Bitmap(validProducts.size());
        final Bitmap validB2s = new Bitmap(validProducts.size());

        int results = 0;
        for (as[0] = 1 ; as[0] <= maxElement ; as[0]++) {
            validProducts.shift((int) -as[0], validB1s);

            for (as[1] = as[0]+1; as[1] <= maxElement; as[1]++) {
                validProducts.shift((int) -as[1], candidateB2s);
                Bitmap.and(validB1s, candidateB2s, validB2s);
                if (validB2s.cardinality() < minBLength) {
                    continue;
                }


                System.out.println(toString(as) + " " + toString(validB2s.getIntegers()));
                results++;
                if (results >= 10) {
                    return;
                }
            }
        }
    }

    // Simple brute
    private void findGroups2(boolean[] validProducts) {
        final int max = 200;
        int[] A = new int[2];
        int[] B = new int[2];
        
        for (A[0] = 1 ; A[0] <= max ; A[0]++) {
            for (A[1] = A[0]+1 ; A[1] <= max ; A[1]++) {
                for (B[0] = 1; B[0] <= max; B[0]++) {
                    for (B[1] = B[0]+1 ; B[1] <= max; B[1]++) {
                        if (check(validProducts, A, B)) {
                            System.out.println(toString(A) +  " " + toString(B));
                        }
                    }
                }
            }
        }
        
    }

    // Simple brute
    private void findGroups3(boolean[] validProducts) {
        final int max = 200;
        int[] A = new int[3];
        int[] B = new int[3];

        for (A[0] = 1 ; A[0] <= max ; A[0]++) {
            System.out.print("\n/");
            for (A[1] = A[0]+1 ; A[1] <= max ; A[1]++) {
                System.out.print(".");
                for (A[2] = A[1]+1 ; A[2] <= max ; A[2]++) {
                    for (B[0] = 1; B[0] <= max; B[0]++) {
                        for (B[1] = B[0]+1 ; B[1] <= max; B[1]++) {
                            for (B[2] = B[1]+1 ; B[2] <= max; B[2]++) {
                                if (check(validProducts, A, B)) {
                                    System.out.println(toString(A) +  " " + toString(B));
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private String toString(Bitmap b) {
        return toString(b.getIntegers());
    }
    private String toString(Bitmap as, Bitmap bs) {
        return toString(as.getBacking()) + " " + toString(bs.getBacking());
    }
    private String toString(int[] ints) {
        StringBuilder sb = new StringBuilder(ints.length*4);
        sb.append("[");
        for (int i: ints) {
            if (sb.length() != 1) {
                sb.append(", ");
            }
            sb.append(Integer.toString(i));
        }
        sb.append("]");
        return sb.toString();
    }

    private String toString(long[] longs) {
        StringBuilder sb = new StringBuilder(longs.length*4);
        sb.append("[");
        for (long l: longs) {
            if (sb.length() != 1) {
                sb.append(", ");
            }
            sb.append(Long.toString(l));
        }
        sb.append("]");
        return sb.toString();
    }

    private boolean check(int[] validProducts, int[] A, int[] B) {
        for (int ai = 0 ; ai < A.length ; ai++) {
            for (int bi = 0 ; bi < B.length ; bi++) {
                if (Arrays.binarySearch(validProducts, A[ai] + B[bi]) < 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean check(boolean[] validProducts, int[] A, int[] B) {
        for (int ai = 0 ; ai < A.length ; ai++) {
            for (int bi = 0 ; bi < B.length ; bi++) {
                if (!validProducts[A[ai] + B[bi]]) {
                    return false;
                }
            }
        }
        return true;
    }

    private Bitmap toBitmap(int[] ints) {
        Bitmap b = new Bitmap(ints[ints.length-1]+1);
        Arrays.stream(ints).forEach(b::set);
        return b;
    }

    private boolean[] toBool(int[] ints) {
        boolean[] result = new boolean[ints[ints.length-1]+1];
        for (int i: ints) {
            result[i] = true;
        }
        return result;
    }

    private Bitmap generateValidProducts(final int maxValid) {
        int primePairs = 0;
        while (Math.pow(4, primePairs) < maxValid) { // 4 is smallest possible valid
            primePairs++;
        }

        // All prime-pairs up to at most maxPrime^2
        final Bitmap bResult = new Bitmap(maxValid+1);
        final GrowableInts result = new GrowableInts(); // For efficient iteration
        result.add(2*2); bResult.set(2*2);
        for (int i = 3 ; i*i <= maxValid ; i+=2) {
            if (isPrime(i)) {
                int pow = i*i;
                result.add(pow); bResult.set(pow);
            }
        }

        for (int pp = 1; pp < primePairs ; pp++) {
            final int prevPos = result.size();
            for (int startPos = 0 ; startPos < prevPos ; startPos++) {
                for (int multiplierPos = startPos ; multiplierPos < prevPos ; multiplierPos++) {
                    final long newValid = 1L*result.get(startPos)*result.get(multiplierPos);
                    if (newValid <= maxValid && !bResult.get((int) newValid)) {
                        result.add((int) newValid); bResult.set((int) newValid);
                    }
                }
            }
        }
        System.out.println("Valid prime pair sums up to maxValid: " + bResult.cardinality());
        return bResult;
    }

    private class GrowableInts {
        int[] ints = new int[100];
        int pos = 0;
        public void add(int v) {
            if (pos == ints.length) {
                int[] newInts = new int[ints.length*2];
                System.arraycopy(ints, 0, newInts, 0, ints.length);
                ints = newInts;
            }
            ints[pos++] = v;
        }
        public int get(int index) {
            return ints[index];
        }
        public int size() {
            return pos;
        }
        public int[] getInts() {
            int[] result = new int[pos];
            System.arraycopy(ints, 0, result, 0, pos);
            return result;
        }
    } 
    

    // https://stackoverflow.com/questions/2385909/what-would-be-the-fastest-method-to-test-for-primality-in-java
    private static int val2(int n) {
        int m = 0;
        if ((n&0xffff) == 0) {
            n >>= 16;
            m += 16;
        }
        if ((n&0xff) == 0) {
            n >>= 8;
            m += 8;
        }
        if ((n&0xf) == 0) {
            n >>= 4;
            m += 4;
        }
        if ((n&0x3) == 0) {
            n >>= 2;
            m += 2;
        }
        if (n > 1) {
            m++;
        }
        return m;
    }

    // For convenience, handle modular exponentiation via BigInteger.
    private static int modPow(int base, int exponent, int m) {
        BigInteger bigB = BigInteger.valueOf(base);
        BigInteger bigE = BigInteger.valueOf(exponent);
        BigInteger bigM = BigInteger.valueOf(m);
        BigInteger bigR = bigB.modPow(bigE, bigM);
        return bigR.intValue();
    }

    // Basic implementation.
    private static boolean isStrongProbablePrime(int n, int base) {
        int s = val2(n-1);
        int d = modPow(base, n>>s, n);
        if (d == 1) {
            return true;
        }
        for (int i=1; i < s; i++) {
            if (d+1 == n) {
                return true;
            }
            d = d*d % n;
        }
        return d+1 == n;
    }

    public static boolean isPrime(int n) {
        if ((n&1) == 0) {
            return n == 2;
        }
        if (n < 9) {
            return n > 1;
        }

        return isStrongProbablePrime(n, 2) && isStrongProbablePrime(n, 7) && isStrongProbablePrime(n, 61);
    }
}
