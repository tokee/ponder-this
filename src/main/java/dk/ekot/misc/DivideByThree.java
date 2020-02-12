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

/**
 * https://www.embeddedrelated.com/showthread/comp.arch.embedded/26432-1.php
 * https://stackoverflow.com/questions/171301/whats-the-fastest-way-to-divide-an-integer-by-3
 */
public class DivideByThree {
    public static void main(String[] args) {
        for (int x = 0 ; x <=243 ; x++) {
            if (x/3 != divideByThree(x)) {
                System.err.println(
                        "Wrong result for " + x + "/3, should have been " + (x/3) + " but was " + divideByThree(x));
            }
            if (x%3 != modByThree(x)) {
                System.err.println(
                        "Wrong result for " + x + "%3, should have been " + (x/3) + " but was " + divideByThree(x));
            }
            for (int divisor: new int[]{3, 3*3, 3*3*3, 3*3*3*3}) {
                if (x / divisor != divide(x, divisor)) {
                    System.err.println("Wrong flex-result for " + x + "/" + divisor + ", should have been " +
                                       (x / divisor) + " but was " + divide(x, divisor));
                }
            }
        }
    }

    private static int divide(int x, int divisor) {
        final int FACTOR=16;
        final int FULL = (int) Math.pow(2, FACTOR);
        final int MULT = FULL/divisor;
        return ((x+1)*MULT)>>FACTOR;
    }

    private static int modByThree(int x) {
        final int MULT = 1024/3;
        return x-((((x+1)*MULT)>>10)*3);
    }

    private static int divideByThree(int x) {
        final int MULT = 1024/3;
        return ((x+1)*MULT)>>10;
    }

}
