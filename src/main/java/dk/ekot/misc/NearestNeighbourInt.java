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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Testing performance of brute force nearest neighbour on high-dimensional vector spaces.
 *
 * Warning: Just an experiment that was not that interesting. Discontinued in favor of {@link NearestNeighbour}.
 */
public class NearestNeighbourInt {
    private static Log log = LogFactory.getLog(NearestNeighbourInt.class);

    private final int dimensions;
    private final int points;
    private DISTRIBUTION distribution;

    enum DISTRIBUTION {random, linear, exponential, logarithmic, onlyTen, thack2}

    public NearestNeighbourInt(int dimensions, int points, DISTRIBUTION distribution) {
        System.out.println("Dimensions: " + dimensions + ", points: " + points + ", distributon: " + distribution);
        this.dimensions = dimensions;
        this.points = points;
        this.distribution = distribution;
    }

    public static void main(String[] args) {
        final int RUNS = 3;
        NearestNeighbourInt nn = new NearestNeighbourInt(2048, 10000, DISTRIBUTION.thack2);
        nn.measureEarlyTerminationInts(RUNS);
    }

    public void measureEarlyTerminationInts(int runs) {
        log.debug("Creating int array");
        MultiDimPointsInt multiDimPoints = new MultiDimPointsInt(dimensions, points);
        log.debug("Filling array");
        multiDimPoints.fill(distribution, true);
        log.debug("Array initialization finished");
        for (NearestFinderInt finder: new NearestFinderInt[] {
                new DumbNearestFinderInt(multiDimPoints),
                new EarlyNearestFinderInt(multiDimPoints)
        }) {
            Random random = new Random(87);
            for (int i = 0; i < runs; i++) {
                long ns = -System.nanoTime();
                Nearest nearest = finder.findNearest(random.nextInt(multiDimPoints.getPoints()));
                ns += System.nanoTime();
                long pointsPerSec = (long)(points/(ns/1000000000.0));
                System.out.println(String.format(
                        "%s: %s in %dms (%d points/s)",
                        finder.getClass().getSimpleName(), nearest, (ns / 1000000), pointsPerSec));
            }
            System.out.println();
        }
    }

    public void measureEarlyTerminationIntsThreaded(int runs, int threads) {
        log.debug("Creating int array");
        MultiDimPointsInt multiDimPoints = new MultiDimPointsInt(dimensions, points);
        log.debug("Filling array");
        multiDimPoints.fill(distribution, true);
        log.debug("Array initialization finished");
        ExecutorService executor = Executors.newCachedThreadPool();

        for (NearestFinderInt finder: new NearestFinderInt[] {
                new DumbNearestFinderInt(multiDimPoints),
                new EarlyNearestFinderInt(multiDimPoints)
        }) {
            Random random = new Random(87);
            for (int i = 0; i < runs; i++) {
                final int basePoint = random.nextInt(multiDimPoints.getPoints());
                long ns = -System.nanoTime();
                Nearest nearest = finder.findNearest(basePoint);
                ns += System.nanoTime();
                long pointsPerSec = (long)(points/(ns/1000000000.0));
                System.out.println(String.format(
                        "%s: %s in %dms (%d points/s)",
                        finder.getClass().getSimpleName(), nearest, (ns / 1000000), pointsPerSec));
            }
            System.out.println();
        }
    }

    private static class EarlyNearestFinderInt extends NearestFinderInt {
        public EarlyNearestFinderInt(MultiDimPointsInt multiDimPoints) {
            super(multiDimPoints);
        }

        @Override
        protected long getDistance(long shortest, int basePoint, int point) {
            final int STEP = 100;
            long distance = 0;
            for (int dimMajor = 0; dimMajor < multiDimPoints.getDimensions() ; dimMajor += STEP) {
                final int dimMax = Math.min(dimMajor + STEP, multiDimPoints.getDimensions());
                for (int dim = dimMajor; dim < dimMax; dim++) {
                    final long diff = multiDimPoints.get(dim, basePoint) - multiDimPoints.get(dim, point);
                    distance += (diff * diff);
                }
                if (distance > shortest) {
                    //System.out.print("[" + dimMajor + "]");
                    return distance;
                }
            }
            return distance;
        }
    }

    private static class DumbNearestFinderInt extends NearestFinderInt {
        public DumbNearestFinderInt(MultiDimPointsInt multiDimPoints) {
            super(multiDimPoints);
        }

        @Override
        protected long getDistance(long shortest, int basePoint, int point) {
            long distance = 0;
            for (int dimIndex = 0; dimIndex < multiDimPoints.getDimensions() ; dimIndex++) {
                final long diff = multiDimPoints.get(dimIndex, basePoint) - multiDimPoints.get(dimIndex, point);
                distance += (diff*diff);
            }
            return distance;
        }
    }

    private static abstract interface NearestFinderBase {
        public Nearest findNearest(int basePoint);
        public Nearest findNearest(int basePoint, int startPoint, int endPoint);
    }

    private static abstract class NearestFinderInt implements NearestFinderBase {
        protected final MultiDimPointsInt multiDimPoints;

        public NearestFinderInt(MultiDimPointsInt multiDimPoints) {
            this.multiDimPoints = multiDimPoints;
        }

        public Nearest findNearest(int basePoint) {
            return findNearest(basePoint, 0, multiDimPoints.getPoints());
        }
        public Nearest findNearest(int basePoint, int startPoint, int endPoint) {
            int nn = -1;
            long shortest = Long.MAX_VALUE;
            for (int point = startPoint; point < endPoint ; point++) {
                if (point == basePoint) {
                    continue;
                }
                long distance = getDistance(shortest, basePoint, point);
                if (distance < shortest) {
                    shortest = distance;
                    nn = point;
                }
            }
            return new Nearest(basePoint, nn, shortest);
        }

        protected abstract long getDistance(long shortest, int basePoint, int point);
    }

    private static class Length implements Comparable<Length> {
        private final int pointIndex;
        private final double length;

        public Length(int pointIndex, double length) {
            this.pointIndex = pointIndex;
            this.length = length;
        }

        public int getPointIndex() {
            return pointIndex;
        }

        public double getLength() {
            return length;
        }

        @Override
        public int compareTo(Length o) {
            return Integer.compare(pointIndex, o.pointIndex);
        }
    }

    private static class MultiDimPointsInt {
        private final int points;
        private final int dimensions;
        private final int[] inner;

        public MultiDimPointsInt(int dimensions, int points) {
            this.points = points;
            this.dimensions = dimensions;
            inner = new int[points*dimensions];
        }

        public final int get(final int dimension, final int point) {
            return inner[point * dimensions + dimension];
        }
        public final void set(final int dimension, final int point, final int value) {
            inner[point * dimensions + dimension] = value;
        }

        public int size() {
            return inner.length;
        }

        public void fill(DISTRIBUTION distribution, boolean randomize) {
            switch (distribution) {
                case random: {
                    Random random = new Random(87);
                    for (int i = 0 ; i < inner.length ; i++) {
                        inner[i] = random.nextInt();
                    }
                    return;
                }
                case linear: {
                    int value = 0;
                    for (int i = 0 ; i < inner.length ; i++) {
                        inner[i] = (value += 1);
                    }
                    break;
                }
                case exponential: {
                    int value = Integer.MAX_VALUE;
                    for (int i = 0 ; i < inner.length ; i++) {
                        inner[i] = (value /= 2);
                    }
                    break;
                }
                case logarithmic: {
                    double value = 1;
                    for (int i = 0 ; i < inner.length ; i++) {
                        inner[i] = (int)Math.log(value);
                        value += 1;
                    }
                    break;
                }
                case onlyTen: {
                    Random random = new Random(87);
                    for (int point = 0; point < points; point++) {
                        for (int dimension = 0; dimension < 10; dimension++) {
                            set(random.nextInt(dimensions), point, random.nextInt());
                        }
                    }
                    return;
                }
                case thack2: {
                    Random random = new Random(87);
                    int index = 0;
                    for (; index < 20 && index < inner.length ; index++) {
                        inner[index] = random.nextInt(2000)+1000;
                    }
                    for (; index < 50 && index < inner.length ; index++) {
                        inner[index] = random.nextInt(1000)+1000;
                    }
                    for (; index < 200 && index < inner.length ; index++) {
                        inner[index] = random.nextInt(500)+500;
                    }
                    for (; index < inner.length ; index++) {
                        inner[index] = random.nextInt(200);
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
                int swap = inner[index];
                inner[index] = inner[i];
                inner[i] = swap;
            }
        }


        public int getPoints() {
            return points;
        }

        public int getDimensions() {
            return dimensions;
        }
    }

    private static class Nearest {
        private final int basePoint;
        private final int point;
        private final double distance;

        public Nearest(int basePoint, int point, double distance) {
            this.basePoint = basePoint;
            this.point = point;
            this.distance = distance;
        }

        @Override
        public String toString() {
            return String.format("%d->%d %.2f", basePoint, point, distance);
        }
    }
}
