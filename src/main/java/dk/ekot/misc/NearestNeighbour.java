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
import java.util.concurrent.Callable;

/**
 * Testing performance of brute force nearest neighbour on high-dimensional vector spaces.
 */
public class NearestNeighbour {
    private static Log log = LogFactory.getLog(NearestNeighbour.class);

    private final int dimensions;
    private final int points;
    private DISTRIBUTION distribution;

    enum DISTRIBUTION {random, linear, exponential, logarithmic, onlyTen, thack2}

    public NearestNeighbour(int dimensions, int points, DISTRIBUTION distribution) {
        System.out.println("Dimensions: " + dimensions + ", points: " + points + ", distributon: " + distribution);
        this.dimensions = dimensions;
        this.points = points;
        this.distribution = distribution;
    }

    public static void main(String[] args) {
        final int RUNS = 3;
        NearestNeighbour nn = new NearestNeighbour(2048, 50000, DISTRIBUTION.thack2);
        nn.measureEarlyTermination(RUNS);
    }

    public void measureEarlyTermination(int runs) {
        log.debug("Creating double array");
        MultiDimPoints multiDimPoints = new MultiDimPoints(dimensions, points);
        log.debug("Filling array");
        multiDimPoints.fill(distribution, true);
        log.debug("Array initialization finished");
        for (NearestFinder finder: new NearestFinder[] {
                new DumbNearestFinder(multiDimPoints),
                new EarlyNearestFinder(multiDimPoints),
                new LengthNearestFinder(multiDimPoints)
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

    private double slowManhattan(MultiDimPoints array, int point1, int point2) {
        double distance = 0;
        for (int x = 0; x < array.getPoints() ; x++) {
            distance +=  Math.abs(array.get(x, point1) - array.get(x, point2));
        }
        return distance;
    }

    private static class EarlyNearestFinder extends NearestFinder {
        public EarlyNearestFinder(MultiDimPoints multiDimPoints) {
            super(multiDimPoints);
        }

        @Override
        protected double getDistance(double shortest, int basePoint, int point) {
            final int STEP = 100;
            double distance = 0;
            for (int dimMajor = 0; dimMajor < multiDimPoints.getDimensions() ; dimMajor += STEP) {
                final int dimMax = Math.min(dimMajor + STEP, multiDimPoints.getDimensions());
                for (int dim = dimMajor; dim < dimMax; dim++) {
                    final double diff = multiDimPoints.get(dim, basePoint) - multiDimPoints.get(dim, point);
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

    private static class LengthNearestFinder extends NearestFinder {
        private final Length[] lengths;
        public LengthNearestFinder(MultiDimPoints multiDimPoints) {
            super(multiDimPoints);
            lengths = multiDimPoints.getLengths(); // Calculate up front
        }

        @Override
        public Nearest findNearest(int basePoint) {
            double shortestLength = Double.MAX_VALUE;
            int basePointIndex = -1;
            for (int i = 0 ; i < lengths.length ; i++) {
                if (basePoint == lengths[i].getPointIndex()) {
                    basePointIndex = i;
                    break;
                }
            }

            int backIndex = basePointIndex-1;
            int bestPointIndex = -1;
            int forwardIndex = basePointIndex+1;
            double shortestDistanceSqr = Double.MAX_VALUE;

            int checks = 0;
            while (backIndex >= 0 || forwardIndex < lengths.length) {
                checks++;
                if (backIndex >= 0) {
                    Length current = lengths[backIndex];
                    double minDist = current.length - lengths[basePointIndex].length;
                    double minDistSqr = minDist*minDist;
                    if (minDistSqr < shortestDistanceSqr) {
//                        System.out.println("Back: checking for shorter min " + minDistSqr + " and shortestExact " + shortestDistanceSqr + " with index " + current.pointIndex);
                        double exactDistanceSquared = exactDistanceSquared(
                                multiDimPoints, current.pointIndex, basePoint);
                        if (exactDistanceSquared < shortestDistanceSqr) {
//                            System.out.println("Back: New shortest " + exactDistanceSquared + " from " + shortestDistanceSqr + " with index " + current.pointIndex);
                            shortestDistanceSqr = exactDistanceSquared;
                            bestPointIndex = current.pointIndex;
                        }
                        backIndex--;
                    } else {
                        backIndex = -1;
                    }
                }
                if (forwardIndex < lengths.length) {
                    Length current = lengths[forwardIndex];
                    double minDist = current.length - lengths[basePointIndex].length;
                    double minDistSqr = minDist*minDist;
                    if (minDist < shortestDistanceSqr) {
                        double exactDistanceSquared = exactDistanceSquared(
                                multiDimPoints, current.pointIndex, basePoint);
                        if (exactDistanceSquared < shortestDistanceSqr) {
                            shortestDistanceSqr = exactDistanceSquared;
                            bestPointIndex = current.pointIndex;
                        }
                        forwardIndex++;
                    } else {
                        forwardIndex = lengths.length;
                    }
                }
            }
//            System.out.println("checks=" + checks);
            return new Nearest(basePoint,  bestPointIndex, shortestDistanceSqr);
        }

        @Override
        public Nearest findNearest(int basePoint, int startPoint, int endPoint) {
            throw new UnsupportedOperationException("Not supported yet");
        }

        @Override
        protected double getDistance(double shortest, int basePoint, int point) {
            final int STEP = 100;
            double distance = 0;
            for (int dimMajor = 0; dimMajor < multiDimPoints.getDimensions() ; dimMajor += STEP) {
                final int dimMax = Math.min(dimMajor + STEP, multiDimPoints.getDimensions());
                for (int dim = dimMajor; dim < dimMax; dim++) {
                    final double diff = multiDimPoints.get(dim, basePoint) - multiDimPoints.get(dim, point);
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

    private static class DumbNearestFinder extends NearestFinder {
        public DumbNearestFinder(MultiDimPoints multiDimPoints) {
            super(multiDimPoints);
        }

        @Override
        protected double getDistance(double shortest, int basePoint, int point) {
            return exactDistanceSquared(multiDimPoints, basePoint, point);
        }

    }

    private static double exactDistanceSquared(MultiDimPoints multiDimPoints, int basePoint, int point) {
        double distance = 0;
        for (int dimIndex = 0; dimIndex < multiDimPoints.getDimensions() ; dimIndex++) {
            final double diff = multiDimPoints.get(dimIndex, basePoint) - multiDimPoints.get(dimIndex, point);
            distance += (diff*diff);
        }
        return distance;
    }
    private static class NearestRunner implements Callable<Nearest> {
        final NearestFinder finder;
        final int basePoint;
        final int startPoint;
        final int endPoint;

        public NearestRunner(NearestFinder finder, int basePoint) {
            this(finder, basePoint, 0, finder.multiDimPoints.points);
        }
        public NearestRunner(NearestFinder finder, int basePoint, int startPoint, int endPoint) {
            this.finder = finder;
            this.basePoint = basePoint;
            this.startPoint = startPoint;
            this.endPoint = endPoint;
        }

        @Override
        public Nearest call() throws Exception {
            return finder.findNearest(basePoint, startPoint, endPoint);
        }
    }

    private static abstract interface NearestFinderBase {
        public Nearest findNearest(int basePoint);
        public Nearest findNearest(int basePoint, int startPoint, int endPoint);
    }
    private static abstract class NearestFinder implements NearestFinderBase {
        protected final MultiDimPoints multiDimPoints;

        public NearestFinder(MultiDimPoints multiDimPoints) {
            this.multiDimPoints = multiDimPoints;
        }

        public Nearest findNearest(int basePoint) {
            return findNearest(basePoint, 0, multiDimPoints.getPoints());
        }
        public Nearest findNearest(int basePoint, int startPoint, int endPoint) {
            int nn = -1;
            double shortest = Double.MAX_VALUE;
            for (int point = startPoint; point < endPoint ; point++) {
                if (point == basePoint) {
                    continue;
                }
                double distance = getDistance(shortest, basePoint, point);
                if (distance < shortest) {
                    shortest = distance;
                    nn = point;
                }
            }
            return new Nearest(basePoint, nn, shortest);
        }

        protected abstract double getDistance(double shortest, int basePoint, int point);
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
            return Double.compare(length, o.length);
        }
    }

    private static class MultiDimPoints {
        private final int points;
        private final int dimensions;
        private final double[] inner;

        private Length[] lengths;

        public MultiDimPoints(int dimensions, int points) {
            this.points = points;
            this.dimensions = dimensions;
            inner = new double[points*dimensions];
        }

        public final double get(final int dimension, final int point) {
            return inner[point * dimensions + dimension];
        }
        public final void set(final int dimension, final int point, final double value) {
            inner[point * dimensions + dimension] = value;
        }

        public int size() {
            return inner.length;
        }

        public Length[] getLengths() {
            if (lengths == null) {
                lengths = new Length[points];
                for (int point = 0; point < points; point++) {
                    double length = 0;
                    for (int dim = 0; dim < dimensions; dim++) {
                        try {
                            final double d = get(dim, point);
                            length += d * d;
                        } catch (Exception e) {
                            throw new RuntimeException("point=" + point + ", dim=" + dim + " with #points=" + points + ", #dims=" + dimensions, e);
                        }
                    }
                    lengths[point] = new Length(point, length);
                }
                Arrays.sort(lengths);
            }
            return lengths;
        }

        public void fill(DISTRIBUTION distribution, boolean randomize) {
            switch (distribution) {
                case random: {
                    Random random = new Random(87);
                    for (int i = 0; i < inner.length; i++) {
                        inner[i] = random.nextDouble();
                    }
                    return;
                }
                case linear: {
                    double value = 0;
                    for (int i = 0; i < inner.length; i++) {
                        inner[i] = (value += 1);
                    }
                    break;
                }
                case exponential: {
                    double value = Float.MAX_VALUE;
                    for (int i = 0; i < inner.length; i++) {
                        inner[i] = (value /= 2);
                    }
                    break;
                }
                case logarithmic: {
                    double value = 1;
                    for (int i = 0; i < inner.length; i++) {
                        inner[i] = Math.log(value);
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
                    for (; index < 20 && index < inner.length; index++) {
                        inner[index] = random.nextDouble() * 2 + 1;
                    }
                    for (; index < 50 && index < inner.length; index++) {
                        inner[index] = random.nextDouble() + 1;
                    }
                    for (; index < 200 && index < inner.length; index++) {
                        inner[index] = random.nextDouble() * 0.5 + 0.5;
                    }
                    for (; index < inner.length; index++) {
                        inner[index] = random.nextDouble() * 0.2;
                    }
                    break;
                }
                default:
                    throw new UnsupportedOperationException(
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
