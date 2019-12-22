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

/**
 *
 */
public abstract class NearestFinder implements NearestFinderBase {
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
        for (int point = startPoint; point < endPoint; point++) {
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

    protected double getDistance(double shortest, int basePoint, int point) {
        return exactDistanceSquared(basePoint, point);
    }

    protected double exactDistanceSquared(int basePoint, int point) {
        double distance = 0;
        for (int dimIndex = 0; dimIndex < multiDimPoints.getDimensions() ; dimIndex++) {
            final double diff = multiDimPoints.get(dimIndex, basePoint) - multiDimPoints.get(dimIndex, point);
            distance += (diff*diff);
        }
        return distance;
    }

    protected double atMostDistanceSquared(double atMost, int basePoint, int point) {
        final int STEP = 100;
        double distance = 0;
        for (int dimMajor = 0; dimMajor < multiDimPoints.getDimensions(); dimMajor += STEP) {
            final int dimMax = Math.min(dimMajor + STEP, multiDimPoints.getDimensions());
            for (int dim = dimMajor; dim < dimMax; dim++) {
                final double diff = multiDimPoints.get(dim, basePoint) - multiDimPoints.get(dim, point);
                distance += (diff * diff);
            }
            if (distance > atMost) {
                //System.out.print("[" + dimMajor + "]");
                return distance;
            }
        }
        return distance;
    }

}
