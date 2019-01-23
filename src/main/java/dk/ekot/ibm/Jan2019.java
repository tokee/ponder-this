package dk.ekot.ibm;

import dk.ekot.misc.Bitmap;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

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

/*
   java -cp ./target/ponder-this-0.1-SNAPSHOT.jar dk.ekot.ibm.Jan2019 2000000 4 4 1

earlyEliminationSet
  4,4: maxElements=     5M: [1, 2608, 87088, 2334256] [528, 12768, 187488, 1697808] 225 seconds ***
  4,4: maxElements=    10M: [1, 2608, 87088, 2334256] [528, 12768, 187488, 1697808] 778 seconds ***

earlyEliminationMix
  2,2: maxElements=     41: [1, 33] [3, 48]
  2,3: maxElements=     98: [1, 97] [3, 24, 99]
  3,3: maxElements=    578: [1, 166, 481] [3, 195, 675]
  3,4: maxElements=  10658: [1, 1366, 3361] [3, 483, 2115, 17955]
  3,5: maxElements= 112338: [1, 2913, 93633] [3, 1848, 9408, 31683, 131043] 2 seconds
  4,5: maxElements=     1M: (912601): [1, 13393, 179713, 586433] [28223, 71288, 304703, 1238768] 2638 seconds

 */

// Note: Is it all n^2 and not all prime-pair-sums?
public class Jan2019 {
    public static void main(String[] args) {
        new Jan2019().run(args);
    }

    long startNS = System.nanoTime();

    private static final String USAGE =
            "Jan2019 [maxElement [minALength [minBLength [maxResults [minValidDeltaCardinality]]]]]";
    void run(String[] args) {
        if (args.length == 1 && "".equals(args[0])) {
            System.out.println(USAGE);
            return;
        }
        final int maxElement = args.length > 0 ? Integer.parseInt(args[0]) : 2_000_000;
        final int minALength = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        final int minBLength = args.length > 2 ? Integer.parseInt(args[2]) : 4;
        final int maxResults = args.length > 3 ? Integer.parseInt(args[3]) : 1;
        final int minValidDeltaCardinality = args.length > 4 ? Integer.parseInt(args[4]) : minBLength*2;

        // 1 [1, 241, 0] [15, 48, 120, 288, 783, 3480] 9 seconds
        // [1, 481, 0] [3, 48, 195, 360, 675, 1368, 3363, 14160] 10 seconds
        // [1, 721, 0] [8, 63, 120, 575, 960, 1680, 3248, 7743, 32040] 11 seconds
        // [1, 1345, 0] [24, 99, 255, 1155, 1680, 2499, 6399, 11880, 27555, 112224] 16 seconds
        // [1, 1441, 0] [3, 80, 323, 675, 960, 1368, 2915, 4488, 7395, 13688, 31683, 128880] 17 seconds
        // [1, 2881, 0] [35, 255, 483, 840, 1088, 2303, 3843, 5040, 6723, 12995, 19320, 30975, 56168, 128163, 516960] 28 seconds
        // [1, 3361, 0] [3, 120, 360, 483, 1680, 2115, 3363, 5475, 9408, 12768, 17955, 26568, 42435, 76728, 174723, 703920] 32 seconds

        //fixedA2(maxElement, minBLength);
        //fixedA3(maxElement, minBLength);

        //earlyEliminationB(maxElement, minALength, minBLength, maxResults); // Dog slow
        //earlyEliminationAFirst(maxElement, minALength, minBLength, maxResults); // Fair
        //String results = earlyEliminationMix(maxElement, minALength, minBLength, maxResults); // Somewhat fast
        String results = earlyEliminationSet(maxElement, minALength, minBLength, maxResults, minValidDeltaCardinality);
        System.out.println("*************************");
        System.out.println(results);
        System.out.println("Total time: " + time());
    }

    // *****************************************************************************************************************

    String earlyEliminationSet(
            int maxElement, int minALength, int minBLength, int maxResults, int minValidDeltaCardinality) {
        System.out.println("Early elimination set maxElement=" + maxElement + ", min-A-size=" + minALength +
                           ", min-B-size=" + minBLength + ", minValidDeltaCardinality=" + minValidDeltaCardinality);
        StringBuilder result = new StringBuilder();
        final IS validDeltas = getValidDeltas(maxElement, minBLength, minValidDeltaCardinality);

        final List<Iterator<Integer>> isi = new ArrayList<>(minALength);
        isi.add(new IntSequence(1, maxElement+1));
        for (int i = 1 ; i < minALength ; i++) {
            isi.add(null);;
        }
        final int[] as = new int[minALength];
        final IS[] validBs = new IS[minALength];
        final IS[] validAs = new IS[minALength];

        earlyEliminationSet(validDeltas, maxElement, minALength, minBLength, isi, as, validAs, validBs, 0,
                            new AtomicInteger(maxResults), new AtomicInteger(1), new AtomicInteger(1),
                            result);
        return result.toString();
    }

    void earlyEliminationSet(
            IS validDeltas, int maxElement, int minALength, int minBLength,
            List<Iterator<Integer>> isi, int[] as, IS[] validAs, IS[] validBs, final int level,
            AtomicInteger resultsLeft, AtomicInteger printedA, AtomicInteger printedB, StringBuilder result) {
        if (level == minALength) {
            String res = toString(as) + " " + validBs[level-1] + " " + time() + " ***";
            System.out.println(res);
            result.append(res).append("\n");
            resultsLeft.decrementAndGet();
            return;
        }

        while (isi.get(level).hasNext()) {
            as[level] = isi.get(level).next();
            if (level == 0) {
                System.out.print(as[level] + " ");
                if ((as[level] & 31) == 0) {
                    System.out.println("- " + time());
                }
            }

            validBs[level] = getSquareNumbers(-as[level], maxElement, level == 0 ? null : validBs[level-1]);
            final int cardinality = validBs[level].size();
            if (cardinality >= minBLength) {
                if (level > printedA.get()) {
                    System.out.println(toString(as) + " " + validBs[level] + " " + time());
                    printedA.set(level);
                    printedB.set(1);
                } else if (level == printedA.get() && (cardinality > printedB.get())) {
                    System.out.println(toString(as) + " " + validBs[level] + " " + time());
                    printedB.set(cardinality);
                }
                if (level < validAs.length-1) {
                    if (level ==0) {
                        validAs[level + 1] = shift(validDeltas, as[level]);
                    } else {
                        validAs[level + 1] = and(validAs[level], shift(validDeltas, as[level]));
                    }
                    isi.set(level+1, validAs[level+1].iterator());
                }

                earlyEliminationSet(validDeltas, maxElement, minALength, minBLength, isi, as,
                                    validAs, validBs, level + 1,
                                    resultsLeft, printedA, printedB, result);

                if (resultsLeft.get() <= 0) {
                    break;
                }
            }
        }
        as[level] = 0;
    }

    class IS extends LinkedHashSet<Integer>{
        public IS(int initialCapacity) {
            super(initialCapacity);
        }

        public IS() {
        }
    };
    class IntSequence implements Iterator<Integer> {
        private int current;
        private final int end;
        public IntSequence(int start, int end) { // End is non-inclusive
            current = start;
            this.end = end;
        }

        @Override
        public boolean hasNext() {
            return current < end;
        }

        @Override
        public Integer next() {
            return current++;
        }
    }

    private IS shift(IS source, int offset) {
        IS result = new IS(source.size());
        for (Integer i: source) {
            int val = i+offset;
            if (val > 0) {
                result.add(val);
            }
        }
        return result;
    }

    private IS and(IS set1, IS set2) {
        if (set1.size() > set2.size()) {
            IS temp = set1;
            set1 = set2;
            set2 = temp;
        }
        IS result = new IS(set1.size());
        for (Integer i: set1) {
            if (set2.contains(i)) {
                result.add(i);
            }
        }
        return result;
    }

    private IS getSquareNumbers(int offset, int max) {
        return getSquareNumbers(offset, max, null);
    }
    private Map<Integer, IS> sqCache = new HashMap<>();
    private IS getSquareNumbers(int offset, int max, final IS andSet) {
        IS set = new IS();
        int start = offset < 0 ? (int) Math.sqrt(offset) : 2;
        int end = (int) (Math.sqrt(max) - offset);
        for (int i = start ; i < end+1; i++) {
            int val = i*i+offset;
            if (val < 1) {
                continue;
            }
            if (val > max) {
                break;
            }
            if (andSet == null || andSet.contains(val)) {
                set.add(val);
            }
        }
        return set;
    }
    private int countSquareNumbers(int offset, int max, final IS andSet) {
        int count = 0;
        int start = offset < 0 ? (int) Math.sqrt(offset) : 2;
        int end = (int) (Math.sqrt(max) - offset);
        StreamSupport.stream(Spliterators.spliterator(new IntSequence(start, end+1), (end+1-start), Spliterator.CONCURRENT))
        .
        for (int i = start ; i < end+1; i++) {
            int val = i*i+offset;
            if (val < 1) {
                continue;
            }
            if (val > max) {
                break;
            }
            if (andSet == null || andSet.contains(val)) {
                count++;
            }
            if (i == start+100) break;
        }
        return count;
    }


    IS getValidDeltas(int max, int minCardinality, int minValidDeltaCardinality) {
        System.out.print("Calculating valid deltas... ");
        final long deltaStart = System.nanoTime();
        int maxCardinality = -1;
        int maxCardinalityDelta = -1;
        IS validDeltas = new IS();
        IS validBase = getSquareNumbers(0, max, null);
        for (int delta = 1 ; delta < max-minCardinality ; delta++) {
            //IS both = getSquareNumbers(-delta, max, validBase);
            int cardinality = countSquareNumbers(-delta, max, validBase);
            if (cardinality >= minValidDeltaCardinality) {
                validDeltas.add(delta);
            }
            maxCardinality = maxCardinality < cardinality ? cardinality : maxCardinality;
        }
        System.out.println(String.format(
                "Calculated %d/%d valid deltas in %d seconds with " +
                "minCardinality=%d, maxCardinality=%d (first: delta=%d)",
                validDeltas.size(), max, (System.nanoTime()-deltaStart)/1000000/1000,
                minCardinality, maxCardinality, maxCardinalityDelta));

        return validDeltas;
    }


    private IS bitmapToSet(Bitmap bits) {
        IS set = new IS();
        int index = 0;
        while (index < bits.size()) {
            index = bits.thisOrNext(index);
            if (index < bits.size()) {
                set.add(index);
            }
        }
        return set;
    }

    String earlyEliminationMix(int maxElement, int minALength, int minBLength, int maxResults) {
        System.out.println("Early elimination maxElement=" + maxElement + ", min-A-size=" + minALength +
                           ", min-B-size=" + minBLength);
        StringBuilder result = new StringBuilder();
        final Bitmap validProducts = generateValidValues(maxElement * 2);
        Bitmap validDeltas = getValidDeltas(validProducts, maxElement, minBLength);

        final int[] as = new int[minALength];
        final Bitmap[] candidateBs = new Bitmap[minALength];
        final Bitmap[] validBs = new Bitmap[minALength];
        final Bitmap[] validAs = new Bitmap[minALength];
        for (int i = 0 ; i < minALength ; i++) {
            candidateBs[i] = new Bitmap(validProducts.size());
            validBs[i] = new Bitmap(validProducts.size());
            validAs[i] = new Bitmap(validProducts.size());
            if (i == 0) {
                validAs[i].invert(); // All first are valid, except 0
            }
        }

        earlyEliminationMix(validProducts, validDeltas, maxElement, minALength, minBLength, as, candidateBs, validAs, validBs, 0,
                            new AtomicInteger(maxResults), new AtomicInteger(1), new AtomicInteger(1),
                            result);
        return result.toString();
    }

    void earlyEliminationMix(
            Bitmap validProducts, Bitmap validDeltas, int maxElement, int minALength, int minBLength,
            int[] as, Bitmap[] candidateBs, Bitmap[] validAs, Bitmap[] validBs, final int level,
            AtomicInteger resultsLeft, AtomicInteger printedA, AtomicInteger printedB, StringBuilder result) {
        if (level == minALength) {
            String res = toString(as) + " " + toString(validBs[level-1].getIntegers()) + " " + time() + " ***";
            System.out.println(res);
            result.append(res).append("\n");
            resultsLeft.decrementAndGet();
            return;
        }
        final int previousIndex = level == 0 ? 0 : as[level-1];
        as[level] = validAs[level].thisOrNext(previousIndex+1);
        while (as[level] <= maxElement) {
            if (level == 0) {
                System.out.print(as[level] + " ");
                if ((as[level] & 31) == 0) {
                    System.out.println("- " + time());
                }
            }

            if (level == 0) {
                validProducts.shift(-as[level], validBs[level]);
            } else {
                validProducts.shift(-as[level], candidateBs[level]);
                Bitmap.and(validBs[level-1], candidateBs[level], validBs[level], true);
            }
            final int cardinality =
                    validBs[level].cardinalityStopAt(minBLength < (printedB.get()+1) ? printedB.get()+1 : minBLength);
            if (cardinality >= minBLength) {
                if (level > printedA.get()) {
                    System.out.print(toString(as) + " " + toString(validBs[level].getIntegers()));
                    System.out.println(" " + time());
                    printedA.set(level);
                    printedB.set(1);
                } else if (level == printedA.get() && (cardinality > printedB.get())) {
                    System.out.print(toString(as) + " " + toString(validBs[level].getIntegers()));
                    System.out.println(" " + time());
                    printedB.set(validBs[level].cardinality());
                }
                if (level < validAs.length-1) {
                    validDeltas.shift(as[level], validAs[level + 1]);
                    if (level > 0) {
                        Bitmap.and(validAs[level], validAs[level + 1], validAs[level + 1], false);
                    }
                }

                earlyEliminationMix(validProducts, validDeltas, maxElement, minALength, minBLength, as, candidateBs,
                                    validAs, validBs, level + 1,
                                    resultsLeft, printedA, printedB, result);

                if (resultsLeft.get() <= 0) {
                    break;
                }
            }

            as[level] = validAs[level].thisOrNext(as[level]+1);
        }
        as[level] = 0;
    }

    // *****************************************************************************************************************

    void earlyEliminationAFirst(int maxElement, int minALength, int minBLength, int maxResults) {
        System.out.println("Early eliminationAFirst maxElement=" + maxElement + ", min-A-size=" + minALength +
                           ", min-B-size=" + minBLength);
        final Bitmap validProducts = generateValidValues(maxElement * 2);
        Bitmap validDeltas = getValidDeltas(validProducts, maxElement, minBLength);

        final int[] as = new int[minALength];
        final Bitmap[] candidateBs = new Bitmap[minALength];
        final Bitmap[] validBs = new Bitmap[minALength];
        final Bitmap[] validAs = new Bitmap[minALength];
        for (int i = 0 ; i < minALength ; i++) {
            candidateBs[i] = new Bitmap(validProducts.size());
            validBs[i] = new Bitmap(validProducts.size());
            validAs[i] = new Bitmap(validProducts.size());
            if (i == 0) {
                validAs[i].invert(); // All level 0 are valid, except 0
            }
        }

        earlyEliminationAFirst(validProducts, validDeltas, maxElement, minALength, minBLength, as, candidateBs, validAs, validBs, 0,
                               new AtomicInteger(maxResults));
    }

    void earlyEliminationAFirst(
            Bitmap validProducts, Bitmap validDeltas, int maxElement, int minALength, int minBLength,
            int[] as, Bitmap[] candidateBs, Bitmap[] validAs, Bitmap[] validBs, final int level,
            AtomicInteger resultsLeft) {
        if (level == minALength) {

            for (int i = 0 ; i < level ; i++) {
                validProducts.shift(-as[i], candidateBs[i]);
                if (i == 0) {
                    candidateBs[i].copy(validBs[i]);
                } else {
                    Bitmap.and(validBs[i - 1], candidateBs[i], validBs[i], true);
                }
            }
            if (validBs[level-1].cardinality() < minBLength) {
                return;
            }

            System.out.print(toString(as) + " " + toString(validBs[level-1].getIntegers()));
            System.out.println(" " + time());
            resultsLeft.decrementAndGet();
            return;
        }
        final int previousIndex = level == 0 ? 0 : as[level-1];
        as[level] = validAs[level].thisOrNext(previousIndex+1);
        while (as[level] <= maxElement) {
            if (level == 0) {
                System.out.print(as[level] + " ");
                if ((as[level] & 31) == 0) {
                    System.out.println();
                }
            }
            if (level < validAs.length-1) {
                validDeltas.shift(as[level], validAs[level + 1]);
                Bitmap.and(validAs[level], validAs[level + 1], validAs[level + 1], true);
            }
            earlyEliminationAFirst(validProducts, validDeltas, maxElement, minALength, minBLength, as, candidateBs,
                                   validAs, validBs, level + 1,
                                   resultsLeft);
            if (resultsLeft.get() == 0) {
                return;
            }
            as[level] = validAs[level].thisOrNext(as[level]+1);
        }
        as[level] = 0;
    }

    // *****************************************************************************************************************

    String earlyEliminationB(int maxElement, int minALength, int minBLength, int maxResults) {
        System.out.println("Early eliminationB maxElement=" + maxElement + ", min-A-size=" + minALength +
                           ", min-B-size=" + minBLength);
        StringBuilder result = new StringBuilder();
        final Bitmap validProducts = generateValidValues(maxElement * 2);

        final int[] as = new int[minALength];
        final Bitmap[] candidateBs = new Bitmap[minALength];
        final Bitmap[] validBs = new Bitmap[minALength];
        for (int i = 0 ; i < minALength ; i++) {
            candidateBs[i] = new Bitmap(validProducts.size());
            validBs[i] = new Bitmap(validProducts.size());
        }

        earlyEliminationB(validProducts, maxElement, minALength, minBLength, as, candidateBs, validBs, 0,
                          new AtomicInteger(maxResults), new AtomicInteger(1), new AtomicInteger(1),
                          result);
        return result.toString();
    }

    void earlyEliminationB(
            Bitmap validProducts, int maxElement, int minALength, int minBLength,
            int[] as, Bitmap[] candidateBs, Bitmap[] validBs, final int level,
            AtomicInteger resultsLeft, AtomicInteger printedA, AtomicInteger printedB, StringBuilder result) {
        if (level == minALength) {
            String res = toString(as) + " " + toString(validBs[level-1].getIntegers()) + " " + time();
            result.append(res).append("\n");
            System.out.println(res);
            resultsLeft.decrementAndGet();
            return;
        }
        int startIndex = level == 0 ? 1 : as[level-1]+1;
        for (as[level] = startIndex ; as[level] <= maxElement ; as[level]++) {
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
                Bitmap.and(validBs[level-1], candidateBs[level], validBs[level], true);
            }

            if (validBs[level].cardinality() >= minBLength) {
                if (level > printedA.get()) {
                    System.out.print(toString(as) + " " + toString(validBs[level].getIntegers()));
                    System.out.println(" " + time());
                    printedA.set(level);
                    printedB.set(1);
                } else if (level == printedA.get() && validBs[level].cardinality() > printedB.get()) {
                    System.out.print(toString(as) + " " + toString(validBs[level].getIntegers()));
                    System.out.println(" " + time());
                    printedB.set(validBs[level].cardinality());
                }
                earlyEliminationB(validProducts, maxElement, minALength, minBLength, as, candidateBs,
                                  validBs, level + 1,
                                  resultsLeft, printedA, printedB, result);
                if (resultsLeft.get() <= 0) {
                    break;
                }
            }
        }
        as[level] = 0;
    }

    // *****************************************************************************************************************


    // Calculate deltas from 1 that has >= minBLength valids

    Bitmap getValidDeltas(Bitmap validProducts, int maxElement, int minBLength) {
        System.out.print("Calculating valid deltas... ");
        final Bitmap validDeltas = new Bitmap(maxElement+1, true);
        final Bitmap reuse = new Bitmap(validProducts.size());

        for (int delta = 1 ; delta < maxElement ; delta++) {
            validProducts.shift(-delta, reuse);
            Bitmap.and(validProducts, reuse, reuse, true);
            if (reuse.cardinality() >= minBLength) {
                validDeltas.set(delta);
            }
        }
        System.out.println("Calculated valid deltas: " + validDeltas.cardinality() + "/" + maxElement);
        return validDeltas;
    }

    void fixedA2(int maxElement, final int minBLength) {
        final int minALength = 2;

        final Bitmap validProducts = generateValidValues(maxElement * 2);

        final long[] as = new long[minALength];
        final Bitmap validB1s = new Bitmap(validProducts.size());
        final Bitmap candidateB2s = new Bitmap(validProducts.size());
        final Bitmap validB2s = new Bitmap(validProducts.size());

        int results = 0;
        for (as[0] = 1 ; as[0] <= maxElement ; as[0]++) {
            validProducts.shift((int) -as[0], validB1s);

            for (as[1] = as[0]+1; as[1] <= maxElement; as[1]++) {
                validProducts.shift((int) -as[1], candidateB2s);
                Bitmap.and(validB1s, candidateB2s, validB2s, true);
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

    void fixedA3(int maxElement, final int minBLength) {
        final int minALength = 3;

        final Bitmap validProducts = generateValidValues(maxElement * 2);

        final long[] as = new long[minALength];
        final Bitmap validB1s = new Bitmap(validProducts.size());
        final Bitmap candidateB2s = new Bitmap(validProducts.size());
        final Bitmap validB2s = new Bitmap(validProducts.size());
        final Bitmap candidateB3s = new Bitmap(validProducts.size());
        final Bitmap validB3s = new Bitmap(validProducts.size());

        int results = 0;
        for (as[0] = 1 ; as[0] <= maxElement ; as[0]++) {
            validProducts.shift((int) -as[0], validB1s);

            for (as[1] = as[0]+1; as[1] <= maxElement; as[1]++) {
                validProducts.shift((int) -as[1], candidateB2s);
                Bitmap.and(validB1s, candidateB2s, validB2s, true);
                if (validB2s.cardinality() < minBLength) {
                    continue;
                }

                for (as[2] = as[1]+1; as[2] <= maxElement; as[2]++) {
                    validProducts.shift((int) -as[2], candidateB3s);
                    Bitmap.and(validB2s, candidateB3s, validB3s, true);
                    if (validB3s.cardinality() < minBLength) {
                        continue;
                    }

                    System.out.println(toString(as) + " " + toString(validB3s.getIntegers()));
                    results++;
                    if (results >= 10) {
                        return;
                    }
                }
            }
        }
    }

    // Simple brute
    void findGroups2(boolean[] validProducts) {
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
    void findGroups3(boolean[] validProducts) {
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

    String toString(Bitmap b) {
        return toString(b.getIntegers());
    }
    String toString(Bitmap as, Bitmap bs) {
        return toString(as.getBacking()) + " " + toString(bs.getBacking());
    }
    String toString(int[] ints) {
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

    String toString(long[] longs) {
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

    boolean check(int[] validProducts, int[] A, int[] B) {
        for (int ai = 0 ; ai < A.length ; ai++) {
            for (int bi = 0 ; bi < B.length ; bi++) {
                if (Arrays.binarySearch(validProducts, A[ai] + B[bi]) < 0) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean check(boolean[] validProducts, int[] A, int[] B) {
        for (int ai = 0 ; ai < A.length ; ai++) {
            for (int bi = 0 ; bi < B.length ; bi++) {
                if (!validProducts[A[ai] + B[bi]]) {
                    return false;
                }
            }
        }
        return true;
    }

    Bitmap toBitmap(int[] ints) {
        Bitmap b = new Bitmap(ints[ints.length-1]+1);
        Arrays.stream(ints).forEach(b::set);
        return b;
    }

    boolean[] toBool(int[] ints) {
        boolean[] result = new boolean[ints[ints.length-1]+1];
        for (int i: ints) {
            result[i] = true;
        }
        return result;
    }

    // Why only these?
    Bitmap generateValidValues(final int maxValid) {
        System.out.print("Generating valid square numbers... ");
        final Bitmap bResult = new Bitmap(maxValid+1, true);
        final int top = (int) (Math.sqrt(maxValid) + 1);
        for (int i = 2; i < top ; i++) {
            bResult.set(i*i);
        }
        System.out.println("Valid square numbers up to maxValid: " + bResult.cardinality());
        return bResult;
    }

    Bitmap generateValidValuesSumPrimePairs(final int maxValid) {
        System.out.print("Generating valid prime pair sums... ");
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

    class GrowableInts {
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
    static int val2(int n) {
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
    static int modPow(int base, int exponent, int m) {
        BigInteger bigB = BigInteger.valueOf(base);
        BigInteger bigE = BigInteger.valueOf(exponent);
        BigInteger bigM = BigInteger.valueOf(m);
        BigInteger bigR = bigB.modPow(bigE, bigM);
        return bigR.intValue();
    }

    // Basic implementation.
    static boolean isStrongProbablePrime(int n, int base) {
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

    public String time() {
        return (System.nanoTime()-startNS)/1000000/1000 + " seconds";
    }
}
