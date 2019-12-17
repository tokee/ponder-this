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

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Arrays;
import java.util.Random;

/**
 * Testing performance of brute force nearest neighbour on high-dimensional vector spaces.
 */
public class NearestNeighbour {
    private static Log log = LogFactory.getLog(NearestNeighbour.class);

    enum DISTRIBUTION {random, linear, exponential, logarithmic, onlyTen, thack2}
    private final DoubleArray array;

    public NearestNeighbour(int dimensions, int points, DISTRIBUTION distribution) {
        System.out.println("Dimensions: " + dimensions + ", points: " + points + ", distributon: " + distribution);
        log.debug("Creating array");
        array = new DoubleArray(dimensions, points);
        log.debug("Filling array");
        array.fill(distribution, true);
        log.debug("Array initialization finished");
    }

    public static void main(String[] args) {
        final int RUNS = 10;
        NearestNeighbour nn = new NearestNeighbour(2048, 100000, DISTRIBUTION.linear);
        nn.measureWorstCase(RUNS);
        nn.measureEarlyTermination(RUNS);
    }

    public void measureWorstCase(int runs) {
        Random random = new Random(87);
        for (int i = 0; i < runs ; i++) {
            long worstNS = -System.nanoTime();
            int shortest = worstCaseNearest(random);
            worstNS += System.nanoTime();
            System.out.println("Slow: " + shortest + " in " + (worstNS / 1000000) + "ms");
        }
    }

    public void measureEarlyTermination(int runs) {
        Random random = new Random(87);
        for (int i = 0; i < runs ; i++) {
            long ns = -System.nanoTime();
            int shortest = earlyTerminationNearest(random);
            ns += System.nanoTime();
            System.out.println("Early termination: " + shortest + " in " + (ns / 1000000) + "ms");
        }
    }

    /**
     * Calculates nearest neighbour in absolute worst case (all points iterated, full distance calculated for all).
     * @return the column with the nearest neighbour.
     */
    public int worstCaseNearest(Random random) {
        final int base = random.nextInt(array.getRows());
        int nn = -1;
        double shortest = Double.MAX_VALUE;
        for (int i = 0 ; i < array.getRows() ; i++) {
            if (i == base) {
                continue;
            }
            double distance = slowDistanceSquared(array, base, i);
            if (distance < shortest) {
                shortest = distance;
                nn = i;
            }
        }
        return nn;
    }

    public int earlyTerminationNearest(Random random) {
        final int base = random.nextInt(array.getRows());
        int nn = -1;
        double shortest = Double.MAX_VALUE;
        for (int i = 0 ; i < array.getRows() ; i++) {
            if (i == base) {
                continue;
            }
            double distance = earlyTerminationDistanceSquared(shortest, array, base, i);
            if (distance < shortest) {
                shortest = distance;
                nn = i;
            }
        }
        return nn;
    }

    private double slowDistanceSquared(DoubleArray array, int point1, int point2) {
        double distance = 0;
        for (int dimIndex = 0 ; dimIndex < array.getCols() ; dimIndex++) {
            final double diff = array.get(dimIndex, point1) - array.get(dimIndex, point2);
            distance += (diff*diff);
        }
        return distance;
    }

    private double earlyTerminationDistanceSquared(double shortest, DoubleArray array, int point1, int point2) {
        double distance = 0;
        for (int x = 0 ; x < array.getCols() ; x++) {
            final double diff = array.get(x, point1) - array.get(x, point2);
            distance += (diff*diff);
            if (distance > shortest) {
//                System.out.print("[" + y + "]");
                return distance;
            }
        }
        return distance;
    }

    private static class DoubleArray {
        private final int cols;
        private final int rows;
        private final double[] inner;

        public DoubleArray(int rows, int cols) {
            this.cols = cols;
            this.rows = rows;
            inner = new double[cols*rows];
        }

        public final double get(final int x, final int y) {
            return inner[y*cols+x];
        }
        public final void set(final int x, final int y, final double value) {
            inner[y*cols+x] = value;
        }
        public final void setDirect(final int index, final double value) {
            inner[index] = value;
        }

        public int size() {
            return inner.length;
        }

        public void clear() {
            Arrays.fill(inner, 0);
        }

        public void fill(DISTRIBUTION distribution, boolean randomize) {
            switch (distribution) {
                case random: {
                    Random random = new Random(87);
                    for (int i = 0 ; i < inner.length ; i++) {
                        inner[i] = random.nextDouble();
                    }
                    return;
                }
                case linear: {
                    double value = 0;
                    for (int i = 0 ; i < inner.length ; i++) {
                        inner[i] = (value += 1);
                    }
                    break;
                }
                case exponential: {
                    double value = Float.MAX_VALUE;
                    for (int i = 0 ; i < inner.length ; i++) {
                        inner[i] = (value /= 2);
                    }
                    break;
                }
                case logarithmic: {
                    double value = 1;
                    for (int i = 0 ; i < inner.length ; i++) {
                        inner[i] = Math.log(value);
                        value += 1;
                    }
                    break;
                }
                case onlyTen: {
                    Random random = new Random(87);
                    for (int col = 0; col < cols; col++) {
                        for (int i = 0; i < 10; i++) {
                            set(col, random.nextInt(rows), random.nextInt());
                        }
                    }
                    return;
                }
                case thack2: {
                    Random random = new Random(87);
                    int index = 0;
                    for (; index < 20 && index < inner.length ; index++) {
                        inner[index] = random.nextDouble()*2+1;
                    }
                    for (; index < 50 && index < inner.length ; index++) {
                        inner[index] = random.nextDouble()+1;
                    }
                    for (; index < 200 && index < inner.length ; index++) {
                        inner[index] = random.nextDouble()*0.5+0.5;
                    }
                    for (; index < inner.length ; index++) {
                        inner[index] = random.nextDouble()*0.2;
                    }
                    break;
                }
                default: throw new UnsupportedOperationException(
                        "The distribution '" + distribution + "' is not supported");
            }
            if (randomize) {
                randomizeOrder();
            }
        }

        public void randomizeOrder() {
            Random random = new Random(87);
            for (int i = inner.length - 1; i > 0; i--) {
                int index = random.nextInt(i + 1);
                double swap = inner[index];
                inner[index] = inner[i];
                inner[i] = swap;
            }
        }


        public int getCols() {
            return cols;
        }

        public int getRows() {
            return rows;
        }
    }
}
