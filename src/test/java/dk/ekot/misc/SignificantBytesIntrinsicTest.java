package dk.ekot.misc;

import org.junit.Test;

import java.util.Random;
import java.util.SplittableRandom;

import static org.junit.Assert.*;

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
public class SignificantBytesIntrinsicTest {

    @Test
    public void testValidityMonkey() {
        final int RUNS = 1_000_000;
        SplittableRandom r = new SplittableRandom();
        for (int i = 0 ; i < RUNS ; i++) {
            final int val = r.nextInt(Integer.MAX_VALUE);
            assertCorrect(val);
        }
    }

    @Test
    public void testSpecifics() {
        final int[] TESTS = new int[]{
                0, 1, 2, 3, 4, 5, 7, 8, 9, 15, 16, 17, 126, 127, 128, 129, 16383, 16384, 16385,
                1283401252, 104310036, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int val: TESTS) {
            assertCorrect(val);
        }
    }

    @Test
    public void testSpeed() {
        final int ITERATIONS = 10;
        final int RUNS = 10_000_000;
        SplittableRandom rSeed = new SplittableRandom(); // Faster than standard Random

        long dummy = 0;
        for (int i = 0 ; i < ITERATIONS ; i++) {
            final int seed = rSeed.nextInt();

            long sTime = -System.nanoTime();
            {
                SplittableRandom random = new SplittableRandom(seed);
                for (int r = 0; r < RUNS; r++) {
                    dummy += SignificantBytesIntrinsic.vIntSize(random.nextInt(Integer.MAX_VALUE));
                }
                sTime += System.nanoTime();
            }

            long iTime = -System.nanoTime();
            {
                SplittableRandom random = new SplittableRandom(seed);
                for (int r = 0; r < RUNS; r++) {
                    dummy += SignificantBytesIntrinsic.vIntSizeIntrinsic(random.nextInt(Integer.MAX_VALUE));
                }
                iTime += System.nanoTime();
            }

            long dTime = -System.nanoTime();
            {
                SplittableRandom random = new SplittableRandom(seed);
                for (int r = 0; r < RUNS; r++) {
                    dummy += SignificantBytesIntrinsic.dummy(random.nextInt(Integer.MAX_VALUE));
                }
                dTime += System.nanoTime();
            }

            System.out.println(
                    "Iteration " + i + ", vInt=" + sTime/1000000 + "ms, vIntIntrinsic=" + iTime/1000000 +
                    "ms, dummy=" + dTime/1000000 + "ms");
        }
        assertFalse("Dummy test to trick the JIT", 0L == dummy);
    }

    private void assertCorrect(int val) {
        assertEquals("Test value " + val + " / 0b" + Integer.toBinaryString(val) +
                     " (length " + Integer.toBinaryString(val).length() +
                     ", leading " + Integer.numberOfLeadingZeros(val) + ")",
                     SignificantBytesIntrinsic.vIntSize(val), SignificantBytesIntrinsic.vIntSizeIntrinsic(val));
    }

}