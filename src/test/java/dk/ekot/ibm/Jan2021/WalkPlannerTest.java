package dk.ekot.ibm.Jan2021;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
public class WalkPlannerTest extends TestCase {

    public void testWalkTrivial() {
        int width = 3;
        int height = 3;
        int[] walk = new int[width*height];
        for (int i = 0 ; i < walk.length ; i++) {
            walk[i] = i;
        }
        assertWalks(width, height, Collections.singletonList(walk));
    }

    public void testWalkSingleStep() {
        int width = 3;
        int height = 3;
        int[] walk = new int[width*height];
        int index = 0;

        for (int x = 0 ; x < width ; x++) {
            walk[index++] = x;
        }

        for (int y = 1 ; y < height ; y++) {
            walk[index++] = y*width;
        }

        for (int y = 1 ; y < height ; y++) {
            for (int x = 1 ; x < width ; x++) {
                walk[index++] = x + y*width;
            }
        }

        assertWalks(width, height, Collections.singletonList(walk));

    }

    public void testWalkComplex() {
        int width = 3;
        int height = 3;
        List<int[]> walks = getComplexWalk(width, height);

        assertWalks(width, height, walks);

    }

    private List<int[]> getComplexWalk(int width, int height) {
        List<int[]> walks = new ArrayList<>();

        int[] walk0 = new int[width];
        int index = 0;
        for (int x = 0; x < width; x++) {
            walk0[index++] = x;
        }
        walks.add(walk0);

        index = 0;
        int[] walk1 = new int[height - 1];
        for (int y = 1; y < height; y++) {
            walk1[index++] = y * width;
        }
        walks.add(walk1);

        index = 0;
        int[] walk2 = new int[(width - 1) * (height - 1)];
        for (int y = 1; y < height; y++) {
            for (int x = 1; x < width; x++) {
                walk2[index++] = x + y * width;
            }
        }
        walks.add(walk2);
        return walks;
    }

    public void testWalkStridesSimple3x3_3_6() {
        int width = 3;
        int height = 3;
        int[] blocks = new int[]{3, 6};

        simpleHelper(width, height, blocks);
    }

    public void testWalkStridesSimple3x3_4_6() {
        int width = 3;
        int height = 3;
        int[] blocks = new int[]{4, 5};

        simpleHelper(width, height, blocks);
    }

    private void simpleHelper(int width, int height, int[] blocks) {
        List<int[]> walks = new ArrayList<>();
        int index = 0;
        for (int block : blocks) {
            int[] walk = new int[block];
            for (int i = 0; i < block; i++) {
                walk[i] = index++;
            }
            walks.add(walk);
        }

        assertWalks(width, height, walks);
    }

    public void testWalkPlanner() {
        int width = 10;
        int height = 10;
        int[][] walks = WalkPlanner.getFullPrioritisedSegments(width, height);
        //int[][] walks = new int[1][];
        //walks[0] = WalkPlanner.getWalkFullLRTD(width, height);
        assertWalks(width, height, Arrays.asList(walks));
    }

    private void assertWalks(int width, int height, List<int[]> walks) {
        // TODO: Adjust back to 1-3
        for (int antiCount = 1 ; antiCount <=3 ; antiCount++) {
            assertWalks(width, height, walks, antiCount);
        }
    }
    private void assertWalks(int width, int height, List<int[]> walks, int antiCount) {
        int fields = width*height;

        assertEquals("The number of positions should match the grid",
                     Integer.valueOf(width*height), walks.stream().map(sub -> sub.length).reduce(Integer::sum).get());
        assertAllEntries(width, height, walks);

        int expected = WalkPlanner.combinations(width, height, antiCount);

        WalkPlanner.Walk walk = new WalkPlanner.Walk(width, height, walks, antiCount);
        int[] antiPoss = null;
        int posCount = 0;
        List<String> positions = new ArrayList<>();
        while ((antiPoss = walk.next(antiPoss)) != null) {
            ++posCount;
            //positions.add(antiPoss[0] < antiPoss[1] ? "[" + antiPoss[0]+ ", " + antiPoss[1] + "]" : "[" + antiPoss[1]+ ", " + antiPoss[0] + "]");
            Arrays.sort(antiPoss);
            positions.add(Arrays.toString(antiPoss));
            //positions.add(Arrays.toString(antiPoss));
        }
        Collections.sort(positions);

        List<String> duplicates = findDuplicates(positions);

        if (antiCount == 2) {
            List<String> allExpected = getExpected2(width * height);
            Collections.sort(allExpected);
            assertEquals("The returned list should contain the expected elements",
                         listToString(allExpected), listToString(positions));
        }

        if (antiCount == 3) {
            List<String> allExpected = getExpected3(width * height);
            Collections.sort(allExpected);
            assertEquals("The returned list should contain the expected elements",
                         listToString(allExpected), listToString(positions));
        }

        assertEquals("The number of delivered tuples for antiCount=" + antiCount + " should be correct\n" +
                     listToString(positions) + "\n\n" +
                     duplicates, expected, posCount);
    }

    private void assertAllEntries(int width, int height, List<int[]> walks) {
        int[] joined = WalkPlanner.getJoinedPath(width, height, walks);
        Arrays.sort(joined);
        for (int i = 0 ; i < width*height ; i++) {
            assertEquals("The entry " + i + " in array of length " + joined.length + " should be as expected",
                         i, joined[i]);
        }
    }

    private String listToString(List<String> positions) {
        return positions.toString().replace("[[", "[").replace("]]", "]").replace("], [", "]\n[");
    }

    private List<String> findDuplicates(List<String> positions) {
        Set<String> set = new HashSet<>();
        return positions.stream().filter(element -> !set.add(element)).collect(Collectors.toList());
    }

    private long fac(int fields) {
        long factorial = 1;
        for (int i = 2 ; i <= fields ; i++) {
            factorial *= i;
        }
        return factorial;
    }

    private List<String> getExpected2(int fields) {
        List<String> result = new ArrayList<>();
        for (int one = 0 ; one < fields-1; one++) {
            for (int two = one+1 ; two < fields; two++) {
                result.add("[" + one + ", " + two + "]");
            }
        }
        return result;
    }

    public List<String> getExpected3(int fields) {
        List<String> result = new ArrayList<>();
        for (int one = 0 ; one < fields-2; one++) {
            for (int two = one+1 ; two < fields-1; two++) {
                for (int three = two+1 ; three < fields; three++) {
                    result.add("[" + one + ", " + two + ", " + three + "]");
                }
            }
        }
        return result;
    }
}