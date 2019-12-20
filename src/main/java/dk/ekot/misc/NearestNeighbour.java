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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;

/**
 * Testing performance of brute force nearest neighbour on high-dimensional vector spaces.
 */
public class NearestNeighbour {
    private static final String VECTOR_SAMPLE = "/home/te/projects/ponder-this/pixplot_vectors_270707.txt.gz";
    private static Log log = LogFactory.getLog(NearestNeighbour.class);

    private final int dimensions;
    private final int points;
    private DISTRIBUTION distribution;

    enum DISTRIBUTION {random, linear, exponential, logarithmic, onlyTen, thack2, load}

    public NearestNeighbour(int dimensions, int points, DISTRIBUTION distribution) {
        System.out.println("Dimensions: " + dimensions + ", points: " + points + ", distributon: " + distribution);
        this.dimensions = dimensions;
        this.points = points;
        this.distribution = distribution;
    }

    public static void main(String[] args) throws IOException {
        final int RUNS = 10;
        NearestNeighbour nn = new NearestNeighbour(2048, 10000 , DISTRIBUTION.load);
        nn.measureEarlyTermination(RUNS);
    }

    public void measureEarlyTermination(int runs) throws IOException {
        log.debug("Creating double array");
        MultiDimPoints multiDimPoints;
        if (distribution == DISTRIBUTION.load) {
            multiDimPoints = new MultiDimPoints(Paths.get(VECTOR_SAMPLE), points);
        } else {
            multiDimPoints = new MultiDimPoints(dimensions, points);
            log.debug("Filling array");
            multiDimPoints.fill(distribution, true);
        }
        log.debug("Array initialization finished");
        List<NearestFinder> finders = new ArrayList<>();
//        finders.add(new DumbNearestFinder(multiDimPoints));
        finders.add(new EarlyNearestFinder(multiDimPoints));
        finders.add(new LengthNearestFinder(multiDimPoints));

        final int[] nearestPoints = new int[runs];
        final double[] nearestDist = new double[runs];
        Arrays.fill(nearestPoints, -1);
        for (NearestFinder finder: finders) {
            Random random = new Random(87);
            long totalNS = 0;
            for (int run = 0; run < runs; run++) {
                long ns = -System.nanoTime();
                int basePoint = random.nextInt(multiDimPoints.getPoints());
                Nearest nearest = finder.findNearest(basePoint);
                ns += System.nanoTime();
                long pointsPerSec = (long)(multiDimPoints.points/(ns/1000000000.0));
                System.out.print(String.format(
                        "%s: %s in %dms (%d points/s)",
                        finder.getClass().getSimpleName(), nearest, (ns / 1000000), pointsPerSec));
                totalNS += ns;
                if (nearestPoints[run] == -1) {
                    nearestPoints[run] = nearest.point;
                    nearestDist[run] = nearest.distance;
                } else if (nearestPoints[run] != nearest.point) {
                    System.out.print(String.format(" (not a perfect match, which was %d->%d dist=%.2f)",
                                                   basePoint, nearestPoints[run], nearestDist[run]));
                }
                System.out.println();
            }
            long totalPointsPerSec = (long)(multiDimPoints.points*runs/(totalNS/1000000000.0));
            System.out.println(String.format(
                    "Total: %dms (%d points/sec)\n",
                    totalNS / 1000000, totalPointsPerSec));
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
        private int dimChecks = 0;

        public EarlyNearestFinder(MultiDimPoints multiDimPoints) {
            super(multiDimPoints);
        }

        @Override
        public Nearest findNearest(int basePoint, int startPoint, int endPoint) {
            dimChecks = 0;
            Nearest nearest = super.findNearest(basePoint, startPoint, endPoint);
            return new Nearest(nearest.basePoint, nearest.point, nearest.distance,
                               "avg dimChecks=" + dimChecks/(endPoint-startPoint));
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
                    dimChecks += dimMajor;
                    return distance;
                }
            }
            dimChecks += multiDimPoints.getDimensions();
            return distance;
        }
    }

    private static class LengthNearestFinder extends NearestFinder {
        private final int MAX_EXTRA_CHECKS = multiDimPoints.points/10; // 10% overall
        private final Length[] lengths;
        public LengthNearestFinder(MultiDimPoints multiDimPoints) {
            super(multiDimPoints);
            lengths = multiDimPoints.getLengths(); // Calculate up front
        }

        @Override
        public Nearest findNearest(int basePoint) {
            double basePointLength = -1;
            int backIndex = -1;
            int forwardIndex = multiDimPoints.points;
            for (int i = 0 ; i < lengths.length ; i++) {
                if (basePoint == lengths[i].getPointIndex()) {
                    backIndex = i-1;
                    forwardIndex = i+1;
                    basePointLength = lengths[i].getLength();
                    break;
                }
            }

            int bestPointIndex = -1;
            double shortestDistanceSqr = Double.MAX_VALUE;

            int nonMatchesSinceLastReset = 0;
            int checks = 0;
            while ((backIndex >= 0 || forwardIndex < lengths.length) && nonMatchesSinceLastReset < MAX_EXTRA_CHECKS) {
                checks++;
                if (backIndex >= 0) {
                    nonMatchesSinceLastReset++;
                    Length current = lengths[backIndex];
//                    double maxDist = current.length + basePointLength;
                    double minDist = current.length - basePointLength;
                    double minDistAbs = Math.abs(minDist);
                    if (minDistAbs < shortestDistanceSqr) {
                        double exactDistanceSquared = exactDistanceSquared(
                                multiDimPoints, current.pointIndex, basePoint);
//                        System.out.println(String.format("Backward: min=%.2f, exact=%.2f, max=%.2f, index=%d",
//                                                         minDistAbs, exactDistanceSquared, maxDist, backIndex));
//                        System.out.println("Back: checking for shorter min " + minDistSqr + ", exact " + exactDistanceSquared + " and shortestExact " + shortestDistanceSqr + " with index " + current.pointIndex);
                        if (exactDistanceSquared < shortestDistanceSqr) {
//                            System.out.println("Back: New shortest " + exactDistanceSquared + " from " + shortestDistanceSqr + " with index " + current.pointIndex);
                            shortestDistanceSqr = exactDistanceSquared;
                            bestPointIndex = current.pointIndex;
                            nonMatchesSinceLastReset = 0;
                        }
                        backIndex--;
                    } else {
                        backIndex = -1;
                    }
                }
                if (forwardIndex < lengths.length) {
                    nonMatchesSinceLastReset++;
                    Length current = lengths[forwardIndex];
                    double minDist = current.length - basePointLength;
//                    double maxDist = current.length + basePointLength;
                    double minDistAbs = Math.abs(minDist);
                    if (minDistAbs < shortestDistanceSqr) {
                        double exactDistanceSquared = exactDistanceSquared(
                                multiDimPoints, current.pointIndex, basePoint);
//                        System.out.println(String.format("Forward:  min=%.2f, exact=%.2f, max=%.2f, index=%d",
//                                                         minDistAbs, exactDistanceSquared, maxDist, forwardIndex));
                        if (exactDistanceSquared < shortestDistanceSqr) {
                            shortestDistanceSqr = exactDistanceSquared;
                            bestPointIndex = current.pointIndex;
                            nonMatchesSinceLastReset = 0;
                        }
                        forwardIndex++;
                    } else {
                        forwardIndex = lengths.length;
                    }
                }
            }
//            System.out.println("checks=" + checks);
            return new Nearest(basePoint,  bestPointIndex, shortestDistanceSqr, "checked=" + checks);
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

        public MultiDimPoints(Path inputFile, int maxPoints) throws IOException {
            BufferedReader input = getReader(inputFile);
            int totalLines = 0;
            int dimensions = -1;
            String line;
            while ((line = input.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")){
                    continue;
                }
                totalLines++;
                if (dimensions == -1) {
                    dimensions = line.split(" ").length;
                }
                if (totalLines > maxPoints) {
                    break;
                }
            }
            input.close();
            this.points = Math.min(maxPoints, totalLines);
            if (points < maxPoints) {
                System.err.println("Warning: Requested a maximum of " + maxPoints + " points, but '" +
                                   inputFile.getFileName() + " only holds " + points);
            }
            this.dimensions = dimensions;
            inner = new double[points*dimensions];

            input = getReader(inputFile);
            int point = 0;
            while ((line = input.readLine()) != null && point < points) {
                if (line.isEmpty() || line.startsWith("#")){
                    continue;
                }
                String[] tsneDims = line.split(" ");
                if (tsneDims.length != dimensions) {
                    throw new IllegalArgumentException(
                            "The file '" + inputFile.getFileName() + "' was expected to holds points with dimensions " +
                            dimensions + ", but a line with dimension " + tsneDims.length + " was encountered:\n" +
                            line);
                }
                for (int dim = 0 ; dim < dimensions ; dim++) {
                    set(dim, point, Double.parseDouble(tsneDims[dim]));
                }
                point++;
            }
            input.close();
        }
        
        public BufferedReader getReader(Path inputFile) throws IOException {
            return new BufferedReader(
                    inputFile.getFileName().toString().endsWith(".gz") ?
                            new InputStreamReader(new GZIPInputStream(inputFile.toUri().toURL().openStream()), StandardCharsets.UTF_8) :
                            new InputStreamReader(inputFile.toUri().toURL().openStream(), StandardCharsets.UTF_8));
        }

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
            double minLength = Double.MAX_VALUE;
            double maxLength = Double.MIN_VALUE;
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
                    if (length < minLength) {
                        minLength = length;
                    }
                    if (length > maxLength) {
                        maxLength = length;
                    }
                }
                Arrays.sort(lengths);
            }
            System.out.println(String.format("Length calculation for %d points: min=%.2f, max=%.2f",
                                             points, minLength, maxLength));
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
        private final String note;

        public Nearest(int basePoint, int point, double distance) {
            this(basePoint, point, distance, null);
        }
        public Nearest(int basePoint, int point, double distance, String note) {
            this.basePoint = basePoint;
            this.point = point;
            this.distance = distance;
            this.note = note;
        }

        @Override
        public String toString() {
            return String.format("%d->%d dist=%.2f",
                                 basePoint, point, distance) + (note == null ? "" : " (" + note + ")");
        }
    }
}
