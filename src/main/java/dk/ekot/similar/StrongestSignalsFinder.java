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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Extract the most significant signals from each point and uses that for lookups for candidates.
 */
// TODO: Significant it not necessarily the highest number. Could also be the one
public class StrongestSignalsFinder extends NearestFinder {
    final int signalCount;
    final List<List<Integer>> signals = new ArrayList<>(); // dim, point

    public StrongestSignalsFinder(MultiDimPoints multiDimPoints, int signalCount) {
        super(multiDimPoints);
        this.signalCount = signalCount;
        for (int signal = 0 ; signal < multiDimPoints.dimensions ; signal++) {
            this.signals.add(new ArrayList<>());
        }
        for (int point = 0 ; point < multiDimPoints.points ; point++) {
            for (int dim: getTopDimensions(point)) {
                this.signals.get(dim).add(point);
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private int[] getTopDimensions(int point) {
        BoundedPriorityQueue<SignalPair> pq = new BoundedPriorityQueue<>(signalCount);
        for (int dim = 0 ; dim < multiDimPoints.dimensions ; dim++) {
            pq.add(new SignalPair(dim, multiDimPoints.get(dim, point)));
        }

        int[] topDim = new int[signalCount];
        for (int i = 0 ; i < signalCount ; i++) {
            topDim[i] = pq.poll().dimension;
        }
        Arrays.sort(topDim);
        return topDim;
    }

    @Override
    public Nearest findNearest(int basePoint) {
        int[] topDims = getTopDimensions(basePoint);
        int[] pointCloud = new int[multiDimPoints.points];
        for (int topDim: topDims) {
            for (Integer point: signals.get(topDim)) {
                pointCloud[point]++;
            }
        }

        int bestPoint = -1;
        double bestDist = Double.MAX_VALUE;
        for (int point = 0 ; point < pointCloud.length ; point++) {
            if (point == basePoint || pointCloud[point] == 0) {
                continue;
            }
            double dist = atMostDistanceSquared(bestDist, basePoint, point);
            if (dist > bestDist) {
                continue;
            }
            bestDist = dist;
            bestPoint = point;
        }
        return new Nearest(basePoint, bestPoint, bestDist);
    }

    static class SignalPair implements Comparable<SignalPair> {
        final int dimension;
        final double strength;

        public SignalPair(int dimension, double strength) {
            this.dimension = dimension;
            this.strength = strength;
        }

        @Override
        public int compareTo(SignalPair o) {
            return Double.compare(strength, o.strength);
        }
    }
}
