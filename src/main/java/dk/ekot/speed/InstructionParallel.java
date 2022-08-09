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
package dk.ekot.speed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inspired by http://igoro.com/archive/gallery-of-processor-cache-effects/
 */
public class InstructionParallel {
    private static final Logger log = LoggerFactory.getLogger(InstructionParallel.class);


    public static void main(String[] args) {
        int STEPS = 500_000_000;
        int RUNS = 4;

        System.out.println("****** ints");
        for (int i = 0 ; i < RUNS ; i++) {
            //compare1(STEPS);
            compare2(STEPS);
            compare2double(STEPS);
        }

        System.out.println("****** longs");
        for (int i = 0 ; i < RUNS ; i++) {
            //compare1I(STEPS);
            compare2I(STEPS);
            compare2doubleI(STEPS);
        }
    }

    private static void compare1(int steps) {
        final long startTime = System.currentTimeMillis();

        final int[] buffer = new int[1024];
        for (int i = 0 ; i < steps ; i++) {
            buffer[0]++;
            buffer[0]++;
        }
        System.out.println("Finished 1 in " + (System.currentTimeMillis()-startTime) + " ms with sanity " + buffer[0]);
    }

    private static void compare2(int steps) {
        final long startTime = System.currentTimeMillis();

        final int[] buffer = new int[1024];
        for (int i = 0 ; i < steps ; i++) {
            buffer[0]++;
            buffer[1]++;
        }
        System.out.println("Finished 2 in " + (System.currentTimeMillis()-startTime) + " ms with sanity " + buffer[0]);
    }

    private static void compare2double(int steps) {
        final long startTime = System.currentTimeMillis();

        final int[] buffer = new int[1024];
        for (int i = 0 ; i < steps ; i++) {
            buffer[0]++;
        }
        for (int i = 0 ; i < steps ; i++) {
            buffer[1]++;
        }
        System.out.println("Finished D in " + (System.currentTimeMillis()-startTime) + " ms with sanity " + buffer[0]);
    }

    private static void compare1I(long steps) {
        final long startTime = System.currentTimeMillis();

        final long[] buffer = new long[1024];
        for (long i = 0 ; i < steps ; i++) {
            buffer[0]++;
            buffer[0]++;
        }
        System.out.println("Finished 1i in " + (System.currentTimeMillis()-startTime) + " ms with sanity " + buffer[0]);
    }

    private static void compare2I(long steps) {
        final long startTime = System.currentTimeMillis();

        final long[] buffer = new long[1025];
        for (long i = 0 ; i < steps ; i++) {
            buffer[(int) (i & 1023)]++;
            buffer[(int) (i & 1023)+1]++;
        }
        System.out.println("Finished 2i in " + (System.currentTimeMillis()-startTime) + " ms with sanity " + buffer[0]);
    }

    private static void compare2doubleI(long steps) {
        final long startTime = System.currentTimeMillis();

        final long[] buffer = new long[1025];
        for (long i = 0 ; i < steps ; i++) {
            buffer[(int) (i & 1023)]++;
        }
        for (long i = 0 ; i < steps ; i++) {
            buffer[(int) (i & 1023)+1]++;
        }
        System.out.println("Finished Di in " + (System.currentTimeMillis()-startTime) + " ms with sanity " + buffer[0]);
    }

}
