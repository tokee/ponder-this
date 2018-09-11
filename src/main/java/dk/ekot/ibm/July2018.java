/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.
 */
package dk.ekot.ibm;

import it.unimi.dsi.fastutil.Hash;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.*;

/**
 * http://www.research.ibm.com/haifa/ponderthis/challenges/July2018.html
 */
public class July2018 {
    private static Log log = LogFactory.getLog(July2018.class);

    public static final int MAX_NUMBER = 400;
    public static final int MIN_LEVEL = 5;

    public static void main(String[] args) {
        final long startNS = System.nanoTime();

//        final BitStore first = new BArray(MAX_NUMBER*3, MAX_NUMBER*MAX_NUMBER*MAX_NUMBER);
//        final BitStore second = new BArray(MAX_NUMBER*3, MAX_NUMBER*MAX_NUMBER*MAX_NUMBER);
        final BitStore first = new BSet(MAX_NUMBER*3);
        final BitStore second = new BSet(MAX_NUMBER*3);

        System.out.print("Filling");
        final int every = MAX_NUMBER/100;
        int next = every;
        for (int a = 1; a <= MAX_NUMBER ; a++) {
            if (a == next) {
                System.out.print(".");
                next += every;
            }
            for (int b = a; b <= MAX_NUMBER ; b++) {
                final int absum = a+b;
                final int abmul = a*b;
                for (int c = b; c <= MAX_NUMBER ; c++) {
                    if (first.set(absum+c, abmul*c)) {
                        second.set(absum+c, abmul*c);
                    }
                }
            }
        }

        System.out.println("\nSearching...");
        for (int a = 1; a <= MAX_NUMBER ; a++) {
            for (int b = a; b <= MAX_NUMBER ; b++) {
                continue_c:
                for (int c = b; c <= MAX_NUMBER ; c++) {
                    for (int level = 0 ; level < 100 ; level++) {
                        if (!second.get(a+b+c+level*3, (a+level)*(b+level)*(c+level))) {
                            if (level >= MIN_LEVEL) {
                                System.out.println((level) + ": [" + a + ", " + b + ", " + c + "]");
                            }
                            continue continue_c;
                        }
                    }
                }
            }
        }
        System.out.println("Spend " + (System.nanoTime()-startNS)/1000000000L + " seconds checking up to " + MAX_NUMBER +
                           " with store1.size==" + first.size() + ", store2.size==" + second.size());
    }

    private static class ShortSet implements BitStore {
        private final short[][] buckets;
        private final int[] bucketPos;
        private final int sums;

        public ShortSet(int maxSum, int maxProduct) {
            this.sums = maxSum+1;
            final long maxIndex = this.sums*(maxProduct+1);
            int maxBucket = (int) (maxIndex >> 16)+1;
            buckets = new short[maxBucket][10];
            bucketPos = new int[maxBucket];
        }

        @Override
        public boolean set(int sum, int product) {
            if (get(sum, product)) {
                return true;
            }
            final long index = ((long)sum) + product*sums;
            final int bucketIndex = (int) (index >> 16);
            if (bucketPos[bucketIndex] == 0) {
                buckets[bucketIndex] = new short[5];
            }
            buckets[bucketIndex][bucketPos[bucketIndex]++] = (short)index; // TODO: Check this keeps the raw bits
            if (bucketPos[bucketIndex] == buckets[bucketIndex].length) {
                short[] newBucket = new short[buckets[bucketIndex].length*2];
                System.arraycopy(buckets[bucketIndex], 0, newBucket, 0, buckets[bucketIndex].length);
                buckets[bucketIndex] = newBucket;
            }
        }

        @Override
        public boolean get(int sum, int product) {
            final long index = ((long)sum) + product*sums;
            final int bucketIndex = (int) (index >> 16);
            final short bucketValue = (short)index;
            short[] bucket = buckets[bucketIndex];
            for (int i = 0 ; i < bucketPos[bucketIndex] ; i++) {
                if (bucket[i] == bucketValue) {
                    return true;
                }
            }
        }

        @Override
        public int size() {
            return buckets.length;
        }
    }

    private static class BSet implements BitStore {
        private final Set<Long> set = new HashSet<>();
        private final int sums;

        public BSet(int sums) {
            this.sums = sums+1;
        }

        @Override
        public boolean set(int sum, int product) {
            final long pos = ((long)sum) + product*sums;
            return !set.add(pos);
        }

        @Override
        public boolean get(int sum, int product) {
            final long pos = ((long)sum) + product*sums;
            return set.contains(pos);
        }

        @Override
        public int size() {
            return set.size();
        }
    }

    private static class BArray implements BitStore {
        private final int sums;
        private final int products;
        private final boolean[] bitmap;

        public BArray(int sums, int products) {
            this.products = products;
            this.sums = sums+1;
            this.bitmap = new boolean[(sums+1)*(products+1)];
        }

        @Override
        public boolean set(int sum, int product) {
            final int pos = sum + product*sums;
            boolean previous = bitmap[pos];
            bitmap[pos] = true;
            return previous;
        }

        @Override
        public boolean get(int sum, int product) {
            return bitmap[sum + product*sums];
        }

        @Override
        public int size() {
            return bitmap.length/8;
        }
    }

    private interface BitStore {
        boolean set(int sum, int product);
        boolean get(int sum, int product);
        int size();
    }
}
