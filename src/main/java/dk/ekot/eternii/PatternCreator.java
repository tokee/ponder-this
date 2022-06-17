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
package dk.ekot.eternii;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 *
 */
public class PatternCreator {
    private static final Logger log = LoggerFactory.getLogger(PatternCreator.class);


    public static int[][] spiralOut(int side) {
        int[][] spiral = new int[side*side][];
        for (int n = 0 ; n < side*side ; n++) {
            spiral[n] = position(n+1);
            spiral[n][0] += side/2-1;
            spiral[n][1] += side/2-1;
        }
        return spiral;
    }
    public static int[][] spiralIn(int side) {
        int[][] spiralOut = spiralOut(side);
        int[][] spiralIn = new int[spiralOut.length][];
        for (int i = 0; i < spiralOut.length; i++) {
            spiralIn[i] = spiralOut[side * side - 1 - i];
        }
        return spiralIn;
    }

    /**
     * Finds coordinates (position) of the number
     *
     * @param {Number} n - number to find position/coordinates for
     * @return {Number[]} - x and y coordinates of the number
     */
    private static int[] position(int n) {
        final int k = (int) Math.ceil((Math.sqrt(n) - 1) / 2);
        int t = 2 * k + 1;
        int m = t * t;
        t -= 1;
        if (n >= m - t) {
            return new int[]{k - (m - n), -k};
        }
        m -= t;
        if (n >= m - t) {
            return new int[]{-k, -k + (m - n)};
        }
        m -= t;
        if (n >= m - t) {
            return new int[]{-k + (m - n), k};
        }
        return new int[]{k, k - (m - n - t)};
    }
}
