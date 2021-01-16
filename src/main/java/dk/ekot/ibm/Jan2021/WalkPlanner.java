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
package dk.ekot.ibm.Jan2021;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Creates arrays of anitbot positions that should be tried
 */
public class WalkPlanner {
    private static Log log = LogFactory.getLog(WalkPlanner.class);

    static int[][] getWalksFull(int width, int height, int antiCount, List<Integer> startYs) {
        int[][] walks = new int[antiCount][];
        int[] walk = getWalkStartY(width, height, startYs);;
        for (int antiIndex = 0; antiIndex < antiCount; antiIndex++) {
            walks[antiIndex] = walk;
        }
        return walks;
    }

    private static int[][] getWalksFullRLDT(int width, int height, int antiCount) {
        int[] walk = new int[width*height];
        int index = 0;
        for (int y = height-1 ; y >= 0 ; y--) {
            for (int x = width-1 ; x >= 0 ; x--) {
                walk[index++] = x + y*width;
            }
        }
        return asWalks(walk, antiCount);
    }

    // Duplicate walk to all antis
    static int[][] asWalks(int[] walk, int antiCount) {
        int[][] walks = new int[antiCount][];
        Arrays.fill(walks, walk);
        return walks;
    }

    static int[][] getWalksFirstRowOnly(int width, int height, int antiCount) {
        return asWalks(getWalkFirstRowOnly(width, height), antiCount);
    }

    private static int[][] getWalksFirstRowOnly(int width, int height, int antiCount, List<Integer> startYs) {
        return asWalks(getWalkFirstRowOnly(width, height, startYs), antiCount);
    }

    private static int[] getWalkStartY(int width, int height, List<Integer> startYs) {
        Set<Integer> startYsSet = toFullCoordinateSet(startYs, width);

        int[] walk = new int[width * height];
        int index = 0;

        // Calculated starting positions
        for (int pos: startYsSet) {
            walk[index++] = pos;
        }

        // Plain top-down
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pos = x + y * width;
                if (!startYsSet.contains(pos)) {
                    walk[index++] = pos;
                }
            }
        }
        return walk;
    }

    static int[] getWalkRowBased(int width, int height) {
        int[] walk = new int[width * height];
        int index = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pos = x + y * width;
                walk[index++] = pos;
            }
        }
        return walk;
    }

    private static int[] getWalkFirstRowOnly(int width, int height) {
        int[] walk = new int[height];
        int index = 0;
        for (int y = 0; y < height; y++) {
            int pos = y * width;
            walk[index++] = pos;
        }
        return walk;
    }

    private static int[] getWalkFirstRowOnly(int width, int height, List<Integer> startYs) {
        Set<Integer> startYsSet = toFullCoordinateSet(startYs, width);

        int[] walk = new int[height];
        int index = 0;

        // Calculated starting positions
        for (int pos : startYsSet) {
            walk[index++] = pos;
        }

        // The rest of first row
        for (int y = 0; y < height; y++) {
            int pos = y * width;
            if (!startYsSet.contains(pos)) {
                walk[index++] = pos;
            }
        }
        return walk;
    }

    public static Set<Integer> toFullCoordinateSet(List<Integer> rows, int width) {
        Set<Integer> full = new HashSet<>(rows.size());
        for (Integer row: rows) {
            full.add(row*width);
        }
        return full;
    }
}
