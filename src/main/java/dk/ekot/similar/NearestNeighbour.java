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
package dk.ekot.similar;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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

}
