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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Creates arrays of anitbot positions that should be tried
 */
public class WalkPlanner {
    private static Log log = LogFactory.getLog(WalkPlanner.class);

    public static Walk getLRTDWalk(int width, int height, int antiCount) {
        return new Walk(width, height, new int[][]{getWalkFullLRTD(width, height)}, antiCount);
    }
    public static Walk getFullSegmentedWalk(int width, int height, int antiCount) {
        return new Walk(width, height, getFullPrioritisedSegments(width, height), antiCount);
    }

    public static class Walk {
        private final int width;
        private final int height;
        private final List<int[]> paths; // Lengths must sum to width*height
        private int pathIndex;
        private final long startPos;
        private final long endPos;
        private final int[] joinedPath;
        private final int antiCount;
        private final int[] antiIndexes;
        private int antiIndexMax;
        private boolean depleted = false;

        private long pos = 0; // Delivered positions
        private int pathOrigo = 0;
        private long invalids = 0; // For debugging

        public Walk(int width, int height, int[][] paths, int antiCount) {
            this(width, height, Arrays.asList(paths), antiCount);
        }
        public Walk(int width, int height, List<int[]> paths, int antiCount) {
            this(width, height, paths, antiCount, 0, -1);
        }
        // startPos & endPos is inclusive, endPos = -1 = autoset
        public Walk(int width, int height, List<int[]> paths, int antiCount, long startPos, long endPos) {
            this.width = width;
            this.height = height;
            this.paths = paths;
            this.startPos = startPos;
            this.endPos = endPos == -1 ? combinations(width, height, antiCount)-1 : endPos;
            // Check path lengths
            int pathsLength = paths.stream().map(path -> path.length).reduce(0, Integer::sum);
            if (pathsLength != width*height) {
                throw new IllegalArgumentException(
                        "The paths must contain all positions for the grid (" + width*height +
                        ") but contained " + pathsLength);
            }

            // Single access path
            joinedPath = getJoinedPath(width, height, paths);
//            System.out.println(Arrays.toString(joinedPath));
//            System.out.println("----------");
            this.antiCount = antiCount;
            this.antiIndexes = new int[antiCount];

            pathIndex = -1;
            switchToNextPath();
            skipTo(startPos);
        }

        public synchronized void skipTo(long destPos) {
            int[] reuseTuple = null;
            while (pos < destPos) {
                if ((reuseTuple = next(reuseTuple)) == null) {
                    break;
                }
            }
        }

        public long getPos() {
            return pos;
        }

        public long getInvalids() {
            return invalids;
        }

        /**
         * @return this walk from the start (low memory impact as the paths are reused).
         */
        public Walk getSubWalk() {
            return new Walk(width, height, paths, antiCount);
        }
        public Walk getSubWalk(long startPos, long endPos) {
            return new Walk(width, height, paths, antiCount, startPos, endPos);
        }

        /**
         * @return the next tuple of antibot positions to try or null if there are no more tuples.
         */
        public int[] next() {
            return next(new int[antiCount]);
        }
        /**
         * @return the next tuple of antibot positions to try or null if there are no more tuples.
         */
        public synchronized int[] next(int[] reuseTuple) {
            if (depleted) {
                return null;
            }

            // Fill the result
            int[] tuple = reuseTuple == null ? new int[antiCount] : reuseTuple;
            for (int ai = 0 ; ai < antiCount ; ai++) {
                tuple[ai] = joinedPath[antiIndexes[ai]];
            }
            //System.out.println(Arrays.toString(tuple));
//            System.out.println("DeliverTuple " + antiIndexes[0] + ", " + antiIndexes[1]);

            // Jump to next valid tuple

            inc();
            if (pos++ == endPos) {
                depleted = true;
            }
            // Might be depleted now, but that's okay
            return tuple;
        }

        // Return true if there are a tuple available
        private boolean inc() {
//            System.out.print("I: " + state());
            do {
                incSimple();
//                System.out.println(" -> " + state());
            } while (!depleted && !isValid());
//            System.out.print(", " + state());
            //System.out.println("");
            return !depleted;
        }

        private boolean isValid() {
            //System.out.println("IsValid antiIndexMax=" + antiIndexMax + " " + state());
            if (antiCount <= 1) {
                return true;
            }

            // Are any secondary+ >= origo?
            for (int ai = 1 ; ai < antiCount ; ai++) {
                if (antiIndexes[ai] >= pathOrigo) {
                    //System.out.println("OK ai=" + ai + "(" + antiIndexes[ai] + ") " + state());
                    return true;
                }
            }
            ++invalids;
            System.out.println("I: " + state() + " pathOrigo=" + pathOrigo + ", antiIndexMax=" + antiIndexMax);
            return false;
        }

        private void incSimple() {
            for (int ai = antiCount - 1; ai >= 0; ai--) {
                ++antiIndexes[ai];

                if (ai == 0) { // Primary
                    if (antiIndexes[0] == joinedPath.length-antiCount+1) { // EOD
                        depleted = true;
                        return;
                    }
                    // TODO: Maybe == antiIndexMax+1?
                    if (antiIndexes[0] == antiIndexMax-antiCount+2) { // End of segment
                        pathOrigo = antiIndexMax+1;
                        antiIndexMax = antiIndexMax + paths.get(++pathIndex).length;
                        antiIndexes[0] = 0;
                        //System.out.println("EOS origo=" + pathOrigo + ", aiMax=" + antiIndexMax);
                        //switchToNextPath();
                        //return;
                        resetSecondaries(1, 1);
                        if (pathOrigo > antiCount) {
                            antiIndexes[antiIndexes.length-1] = pathOrigo; // Make it valid
                        }
                        continue;
                    }
                    
                    //resetSecondaries(1, antiIndexes[0] < pathOrigo ? pathOrigo-antiCount+2 : antiIndexes[0]+1);
                    //resetSecondaries(1, pathIndex == 0 ? antiIndexes[0]+1 : 0);
                    resetSecondaries(1, antiIndexes[0]+1);
                    if (pathOrigo > antiCount) {
                        antiIndexes[antiIndexes.length-1] = pathOrigo; // Make it valid
                    }
//                    System.out.print(" (" + state() + ")");
                    return;
                }

                // Just for speed up: ai != antiCount-1
                if (ai != antiCount-1 && !resetSecondaries(ai+1, antiIndexes[ai]+1)) {
                    continue;
                }

                if (antiIndexes[ai] <= antiIndexMax) {
                    return;
                }
            }
        }

        // Return true if all went okay (indexes within boundaries)
        private boolean resetSecondaries(int startAI, int origo) {
            //System.out.println("resetSecondaries(startAI=" + startAI + ", origo=" + origo + ")");
            for (int ai = startAI ; ai < antiCount ; ai++) {
                antiIndexes[ai] = ai-startAI+origo;
                if (antiIndexes[ai] > antiIndexMax) {
                    return false;
                }
            }
//            if (startAI < antiCount-1) {
//                System.out.println("Last=" + pathOrigo);
//                antiIndexes[antiCount-1] = pathOrigo;
//            }
            return true;
        }


        private String state(int ai) {
            return Arrays.toString(antiIndexes) +
                   //", val=" + IntStream.of(antiIndexes).boxed().map(index -> joinedPath[index]).collect(Collectors.toList()) +
                   ", i=" + ai + ", origo=" + pathOrigo + ", max=" + antiIndexMax;
        }

        private String state() {
            return Arrays.toString(antiIndexes);
                   //", val=" + IntStream.of(antiIndexes).boxed().map(index -> joinedPath[index]).collect(Collectors.toList()) +
                   //", i=" + ai + ", origo=" + pathOrigo + ", max=" + antiIndexMax;
        }

        // Return true if there are a tuple available
        private boolean switchToNextPath() {
//            System.out.println("Switching to next with " + antiIndexes[0]);
            if (pathIndex >= 0) {
                pathOrigo += paths.get(pathIndex).length;
            }
            if (++pathIndex == paths.size()) {
                depleted = true;
                return false;
            }
            antiIndexMax = pathOrigo+paths.get(pathIndex).length-1;
            antiIndexes[0] = pathOrigo;
            int secondaryOrigo = pathIndex == 0 ? 1 : 0;
            for (int ai = 1 ; ai < antiCount ; ai++) {
                antiIndexes[ai] = ai-1+secondaryOrigo;
                if (antiIndexes[ai] > antiIndexMax) {
                    throw new IllegalStateException("Internal error: The length of the path is too short: " + antiIndexMax);
                }
            }

            //System.out.println("EOS origo=" + pathOrigo + ", aiMax=" + antiIndexMax);
            //System.out.println(" -> " + state());

//                int[] tuple = new int[antiCount];
//                for (int ai = 0; ai < antiCount; ai++) {
//                    tuple[ai] = joinedPath[antiIndexes[ai]];
//                }
//                System.out.println("******** Dexexte " + Arrays.toString(tuple));
            return true;
        }

        private boolean currentIndexesOKdisabled() {
            // Check for duplicates
            for (int i = 0 ; i < antiCount ; i++) {
                for (int j = i+1 ; j < antiCount ; j++) {
                    if (antiIndexes[i] == antiIndexes[j]) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    /**
     * @return the number of unique positions for the given number of antiBots.
     */
    static int combinations(int width, int height, int antiBots) {
        int divident = 1;
        int divisor = 1;
        // fields * (fields - 1) / 2; // 2! = 2
        for (int i = 1 ; i <= antiBots ; i++) {
            divident *= (width*height-(i-1));
            divisor *= i;
        }
        return divident / divisor;
    }

    static int[][] getWalksFull(int width, int height, int antiCount, List<Integer> startYs) {
        return asWalks(getWalkStartY(width, height, startYs), antiCount);
    }

    private static int[][] getWalksFullRLDT(int width, int height, int antiCount) {
        return asWalks(getWalkFullRLDT(width, height), antiCount);
    }
    private static int[] getWalkFullRLDT(int width, int height) {
        int[] walk = new int[width * height];
        int index = 0;
        for (int y = height - 1; y >= 0 ; y--) {
            for (int x = width - 1; x >= 0 ; x--) {
                walk[index++] = x + y * width;
            }
        }
        return walk;
    }

    private static int[][] getWalksFullLRTD(int width, int height, int antiCount) {
        return asWalks(getWalkFullLRTD(width, height), antiCount);
    }
    public static int[] getWalkFullLRTD(int width, int height) {
        int[] walk = new int[width * height];
        int index = 0;
        for (int y = 0 ; y < height ; y++) {
            for (int x = 0 ; x < width ; x++) {
                walk[index++] = x + y * width;
            }
        }
        return walk;
    }

    public static int[] getJoinedPath(int width, int height, List<int[]> paths) {
        final int[] joinedPath;
        joinedPath = new int[width * height];
        {
            int joinedIndex = 0;
            for (int[] path : paths) {
                    //System.out.println(Arrays.toString(path));
                System.arraycopy(path, 0, joinedPath, joinedIndex, path.length);
                joinedIndex += path.length;
            }
        }
        return joinedPath;
    }

    // Based on observation of all solutiond for sizes < 75
    // col0(TD), rowMax(RL), then LR, TD
    public static int[][] getWalksFullOrdered(int width, int height, int antiCount) {
        return asWalks(getWalkFullOrdered(width, height), antiCount);
    }
    public static int[] getWalkFullOrdered(int width, int height) {
        boolean[] marked = new boolean[width*height];
        int[] walk = new int[width*height];
        int index = 0;

        // column 0
        for (int y = 0 ; y < height ;y++) {
            walk[index++] = y*width;
            marked[y*width] = true;
        }

        // Bottom row
        for (int x = width-1 ; x > 0 ; x--) {
            walk[index++] = x + (height-1)*width;
            marked[x + (height-1)*width] = true;
        }

        // Top right & bottom right corners (highest priority to the positions closest to the corners)
        // cornerWidth formula from eyeballing the result of
        // for S in $(seq 30 60); do MAX_C0=0 MAX_RM=0 ./antis.sh -G $S ; done | grep -v :
        int cornerWidth = width/4; // Not ideal if width != height
        for (int cx = 0 ; cx < cornerWidth ; cx++) {
            for (int cy = 0 ; cy <= cx ; cy++) {
                int br = (width-1-cx) + (height-1-cy)*width;
                if (!marked[br]) {
                    walk[index++] = br;
                    marked[br] = true;
                }
                int tr = (width-1-cx) + cy*width;
                if (!marked[tr]) {
                    walk[index++] = tr;
                    marked[tr] = true;
                }
            }
        }

        // Fill in the blanks Left->right, Top->Down (no real strategy yet)
        for (int y = 0 ; y < height-1 ;y++) {
            for (int x = 1 ; x < width ; x++) {
                int pos = x + y*width;
                if (!marked[pos]) {
                    walk[index++] = pos;
                    marked[pos] = true;
                }
            }
        }

        // Check the math
        if (index != walk.length) {
            throw new IllegalStateException("Logic error:index is " + index + " but should be " + width*height);
        }
        return walk;
    }

    public static int[][] getFullPrioritisedSegments(int width, int height) {
        return getFullPrioritisedSegmentsList(width, height).toArray(new int[0][0]);
    }
    public static List<int[]> getFullPrioritisedSegmentsList(int width, int height) {
        boolean[] marked = new boolean[width*height];
        List<int[]> full = new ArrayList<>();

        addColumn0(width, height, marked, full);
//        addBottomRow(width, height, marked, full);
//        addRightCorners(width, height, marked, full);
        fillBlanksLRTD(width, height, marked, full);

        // Check the math
        checkSize(width, height, full);
        return full;
    }

    private static void checkSize(int width, int height, List<int[]> full) {
        int allSegmentsLengths = full.stream().map(segment -> segment.length).reduce(Integer::sum).get();
        if (allSegmentsLengths != width * height) {
            throw new IllegalStateException("Logic error: allSegmentsLengths is " + allSegmentsLengths +
                                            " but should be " + width * height);
        }
    }

    private static void fillBlanksLRTD(int width, int height, boolean[] marked, List<int[]> full) {
        // Fill in the blanks Left->right, Top->Down (no real strategy yet)
        int[] tmpWalk = new int[width * height];
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 1; x < width; x++) {
                int pos = x + y * width;
                if (!marked[pos]) {
                    tmpWalk[index++] = pos;
                    marked[pos] = true;
                }
            }
        }
        int[] walk = new int[index];
        System.arraycopy(tmpWalk, 0, walk, 0, index);
        full.add(walk);
    }

    private static void addRightCorners(int width, int height, boolean[] marked, List<int[]> full) {
        // Top right & bottom right corners (highest priority to the positions closest to the corners)
        // cornerWidth formula from eyeballing the result of
        // for S in $(seq 30 60); do MAX_C0=0 MAX_RM=0 ./antis.sh -G $S ; done | grep -v :
        int[] tmpWalk = new int[width * height];
        int index = 0;
        int cornerWidth = width / 4; // Not ideal if width != height
        for (int cx = 0; cx < cornerWidth; cx++) {
            for (int cy = 0; cy <= cx; cy++) {
                int br = (width - 1 - cx) + (height - 1 - cy) * width;
                if (!marked[br]) {
                    tmpWalk[index++] = br;
                    marked[br] = true;
                }
                int tr = (width - 1 - cx) + cy * width;
                if (!marked[tr]) {
                    tmpWalk[index++] = tr;
                    marked[tr] = true;
                }
            }
        }

        int[] walk = new int[index];
        System.arraycopy(tmpWalk, 0, walk, 0, index);
        full.add(walk);
    }

    private static void addBottomRow(int width, int height, boolean[] marked, List<int[]> full) {
        // Bottom row
        int index = 0;
        int[] walk = new int[width - 1];
        for (int x = width - 1; x > 0; x--) {
            walk[index++] = x + (height - 1) * width;
            marked[x + (height - 1) * width] = true;
        }
        full.add(walk);
        if (index != walk.length) {
            throw new IllegalStateException("walk should contain " + walk.length + " positions but was " + index);
        }
    }

    private static void addColumn0(int width, int height, boolean[] marked, List<int[]> full) {
        // column 0
        int index = 0;
        int[] walk = new int[height];
        for (int y = 0; y < height; y++) {
            walk[index++] = y * width;
            marked[y * width] = true;
        }
        full.add(walk);
        if (index != walk.length) {
            throw new IllegalStateException("walk should contain " + walk.length + " positions but was " + index);
        }
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

    // TD,LR starting at the given startYs
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

    // LR,TD
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
